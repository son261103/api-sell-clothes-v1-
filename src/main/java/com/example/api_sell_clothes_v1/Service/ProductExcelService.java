package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ProductVariant.BulkProductVariantCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.ProductVariantResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductExcelTemplateResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Brands;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Repository.BrandRepository;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import com.example.api_sell_clothes_v1.Utils.ExcelErrorReport;
import com.example.api_sell_clothes_v1.Utils.ZipImportProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExcelService {
    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final ProductThumbnailService thumbnailService;
    private final ProductVariantImageService variantImageService;
    private final ProductImageService productImageService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SkuGeneratorService skuGeneratorService;

    private static final String TEMPLATE_VERSION = "1.1";
    private static final int BUFFER_SIZE = 8192;

    private static final String[] PRODUCT_HEADERS = {
            "Tên Sản Phẩm (*)", "Loại Sản Phẩm (*)", "Thương Hiệu (*)",
            "Mô Tả", "Giá Gốc (*)", "Giá Khuyến Mãi", "Slug"
    };

    private static final String[] VARIANT_HEADERS = {
            "Mã Sản Phẩm (*)", "Kích Thước (*)", "Màu Sắc (*)",
            "SKU", "Số Lượng Tồn Kho (*)"
    };

    /**
     * Trả về thông tin về template hiện tại
     */
    public ProductExcelTemplateResponseDTO getTemplateInfo() {
        List<String> supportedFeatures = Arrays.asList(
                "Nhập sản phẩm cơ bản",
                "Nhập biến thể sản phẩm",
                "Tự động tạo SKU",
                "Nhập hàng loạt hình ảnh",
                "Kiểm tra lỗi dữ liệu"
        );

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = dateFormat.format(new Date());

        return ProductExcelTemplateResponseDTO.builder()
                .version(TEMPLATE_VERSION)
                .lastUpdated(currentDate)
                .supportedFeatures(supportedFeatures)
                .includesCategories(true)
                .includesBrands(true)
                .includesVariants(true)
                .templateUrl("/api/products/excel/template")
                .build();
    }

    /**
     * Tạo file Excel mẫu và hướng dẫn - sử dụng phương pháp đơn giản hóa
     */
    public void generateTemplatePackage(HttpServletResponse response) throws IOException {
        // Đặt content type cho response
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=product_import_template.xlsx");

        // Tạo workbook trực tiếp
        try (Workbook workbook = new XSSFWorkbook()) {
            // Tạo các sheet
            createProductSheet(workbook);
            createVariantSheet(workbook);
            createCategoriesSheet(workbook);
            createBrandsSheet(workbook);
            createInstructionSheet(workbook);

            // Ghi trực tiếp vào response
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo Excel template: ", e);
            throw new IOException("Không thể tạo template Excel: " + e.getMessage());
        }
    }

    /**
     * Tạo hướng dẫn dạng text và gửi đến client
     */
    public void generateInstructionText(HttpServletResponse response) throws IOException {
        // Đặt content type cho response
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=huong_dan_nhap_san_pham.txt");

        // Tạo nội dung hướng dẫn
        String instructions = createInstructionText();

        // Ghi trực tiếp vào response
        try (Writer writer = response.getWriter()) {
            writer.write(instructions);
            writer.flush();
        } catch (Exception e) {
            log.error("Lỗi khi tạo file hướng dẫn: ", e);
            throw new IOException("Không thể tạo file hướng dẫn: " + e.getMessage());
        }
    }

    /**
     * Tạo sheet sản phẩm
     */
    private void createProductSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Sản Phẩm");

        // Set column widths
        for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }

        // Tạo cell style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(PRODUCT_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Tạo cell style cho data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Add sample data
        for (int i = 1; i <= 3; i++) {
            Row row = sheet.createRow(i);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue("Áo thun nam ABC " + i);
            cell0.setCellStyle(dataStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue("Áo nam");
            cell1.setCellStyle(dataStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue("Nike");
            cell2.setCellStyle(dataStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue("Áo thun nam chất lượng cao");
            cell3.setCellStyle(dataStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue(350000);
            cell4.setCellStyle(dataStyle);
        }
    }

    /**
     * Tạo sheet biến thể
     */
    private void createVariantSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Biến Thể");

        // Set column widths
        for (int i = 0; i < VARIANT_HEADERS.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }

        // Tạo cell style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < VARIANT_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(VARIANT_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Tạo cell style cho data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Add sample data
        String[] sizes = {"S", "M", "L", "XL"};
        String[] colors = {"Đen", "Trắng", "Xanh"};

        int rowNum = 1;
        for (int p = 1; p <= 3; p++) {
            for (String size : sizes) {
                for (String color : colors) {
                    Row row = sheet.createRow(rowNum++);

                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(p);
                    cell0.setCellStyle(dataStyle);

                    Cell cell1 = row.createCell(1);
                    cell1.setCellValue(size);
                    cell1.setCellStyle(dataStyle);

                    Cell cell2 = row.createCell(2);
                    cell2.setCellValue(color);
                    cell2.setCellStyle(dataStyle);

                    // SKU left empty
                    Cell cell3 = row.createCell(3);
                    cell3.setCellStyle(dataStyle);

                    Cell cell4 = row.createCell(4);
                    cell4.setCellValue(100);
                    cell4.setCellStyle(dataStyle);
                }
            }
        }
    }

    /**
     * Tạo sheet danh sách loại sản phẩm
     */
    private void createCategoriesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Loại SP");

        // Set column widths
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);

        // Tạo cell style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("ID");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Tên Loại SP");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Mô Tả");
        cell2.setCellStyle(headerStyle);

        // Tạo cell style cho data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Add categories from database with error handling
        try {
            List<Categories> categories = categoryRepository.findAll();
            int rowNum = 1;

            for (Categories category : categories) {
                if (Boolean.TRUE.equals(category.getStatus())) {
                    Row row = sheet.createRow(rowNum++);

                    Cell idCell = row.createCell(0);
                    idCell.setCellValue(category.getCategoryId());
                    idCell.setCellStyle(dataStyle);

                    Cell nameCell = row.createCell(1);
                    nameCell.setCellValue(category.getName());
                    nameCell.setCellStyle(dataStyle);

                    Cell descCell = row.createCell(2);
                    if (category.getDescription() != null) {
                        descCell.setCellValue(category.getDescription());
                    } else {
                        descCell.setCellValue("");
                    }
                    descCell.setCellStyle(dataStyle);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách loại sản phẩm: ", e);
            createDummyCategoryData(sheet, dataStyle);
        }
    }

    /**
     * Tạo dữ liệu mẫu cho categories khi gặp lỗi
     */
    private void createDummyCategoryData(Sheet sheet, CellStyle style) {
        String[][] dummyData = {
                {"1", "Áo nam", "Các loại áo dành cho nam"},
                {"2", "Quần nam", "Các loại quần dành cho nam"},
                {"3", "Áo nữ", "Các loại áo dành cho nữ"},
                {"4", "Quần nữ", "Các loại quần dành cho nữ"},
                {"5", "Phụ kiện", "Các loại phụ kiện thời trang"}
        };

        for (int i = 0; i < dummyData.length; i++) {
            Row row = sheet.createRow(i + 1);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue(Integer.parseInt(dummyData[i][0]));
            cell0.setCellStyle(style);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(dummyData[i][1]);
            cell1.setCellStyle(style);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(dummyData[i][2]);
            cell2.setCellStyle(style);
        }
    }

    /**
     * Tạo sheet danh sách thương hiệu
     */
    private void createBrandsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Thương Hiệu");

        // Set column widths
        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);

        // Tạo cell style cho header
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("ID");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Tên Thương Hiệu");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Mô Tả");
        cell2.setCellStyle(headerStyle);

        // Tạo cell style cho data
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Add brands from database with error handling
        try {
            List<Brands> brands = brandRepository.findAll();
            int rowNum = 1;

            for (Brands brand : brands) {
                if (Boolean.TRUE.equals(brand.getStatus())) {
                    Row row = sheet.createRow(rowNum++);

                    Cell idCell = row.createCell(0);
                    idCell.setCellValue(brand.getBrandId());
                    idCell.setCellStyle(dataStyle);

                    Cell nameCell = row.createCell(1);
                    nameCell.setCellValue(brand.getName());
                    nameCell.setCellStyle(dataStyle);

                    Cell descCell = row.createCell(2);
                    if (brand.getDescription() != null) {
                        descCell.setCellValue(brand.getDescription());
                    } else {
                        descCell.setCellValue("");
                    }
                    descCell.setCellStyle(dataStyle);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thương hiệu: ", e);
            createDummyBrandData(sheet, dataStyle);
        }
    }

    /**
     * Tạo dữ liệu mẫu cho brands khi gặp lỗi
     */
    private void createDummyBrandData(Sheet sheet, CellStyle style) {
        String[][] dummyData = {
                {"1", "Nike", "Thương hiệu thời trang thể thao"},
                {"2", "Adidas", "Thương hiệu thời trang thể thao quốc tế"},
                {"3", "Zara", "Thương hiệu thời trang Tây Ban Nha"},
                {"4", "H&M", "Thương hiệu thời trang Thụy Điển"},
                {"5", "Uniqlo", "Thương hiệu thời trang Nhật Bản"}
        };

        for (int i = 0; i < dummyData.length; i++) {
            Row row = sheet.createRow(i + 1);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue(Integer.parseInt(dummyData[i][0]));
            cell0.setCellStyle(style);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(dummyData[i][1]);
            cell1.setCellStyle(style);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(dummyData[i][2]);
            cell2.setCellStyle(style);
        }
    }

    /**
     * Tạo sheet hướng dẫn
     */
    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Hướng Dẫn");

        // Set column width
        sheet.setColumnWidth(0, 10000);

        // Tạo cell style cho title
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // Tạo cell style cho content
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);

        // Create title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HƯỚNG DẪN NHẬP SẢN PHẨM");
        titleCell.setCellStyle(titleStyle);

        // Add instructions
        String[] instructions = {
                "1. File này chứa 4 sheet: Sản Phẩm, Biến Thể, Danh Sách Loại SP và Danh Sách Thương Hiệu.",
                "2. Nhập thông tin sản phẩm vào sheet 'Sản Phẩm' trước. Các trường đánh dấu (*) là bắt buộc.",
                "3. Nhập biến thể sản phẩm vào sheet 'Biến Thể'. Mỗi sản phẩm có thể có nhiều biến thể.",
                "4. Tham khảo sheet 'Danh Sách Loại SP' và 'Danh Sách Thương Hiệu' để nhập đúng tên.",
                "5. Hệ thống sẽ tự sinh SKU nếu bạn để trống cột SKU trong biến thể.",
                "6. Đặt tên file ảnh thumbnail theo định dạng: 'thumbnail_[tên sản phẩm].jpg'",
                "7. Đặt tên file ảnh biến thể theo định dạng: 'variant_[SKU].jpg'",
                "8. Đặt tất cả ảnh vào cùng một thư mục và nén lại thành file ZIP cùng với file Excel này."
        };

        for (int i = 0; i < instructions.length; i++) {
            Row row = sheet.createRow(i + 2);
            Cell cell = row.createCell(0);
            cell.setCellValue(instructions[i]);
            cell.setCellStyle(contentStyle);
        }
    }

    /**
     * Tạo nội dung hướng dẫn text
     */
    private String createInstructionText() {
        StringBuilder content = new StringBuilder();
        content.append("HƯỚNG DẪN NHẬP SẢN PHẨM BẰNG FILE EXCEL\n");
        content.append("=========================================\n\n");
        content.append("I. QUY TRÌNH NHẬP SẢN PHẨM\n");
        content.append("1. Điền thông tin sản phẩm và biến thể vào file Excel mẫu\n");
        content.append("2. Chuẩn bị ảnh thumbnail và ảnh biến thể theo cấu trúc thư mục\n");
        content.append("3. Nén thư mục Images và file Excel thành file ZIP\n");
        content.append("4. Tải lên file ZIP qua chức năng 'Nhập sản phẩm từ Excel' trên hệ thống\n\n");

        content.append("II. CẤU TRÚC THƯ MỤC ẢNH\n");
        content.append("Tạo thư mục Images với cấu trúc như sau:\n\n");
        content.append("Images/\n");
        content.append("  ├── [SKU1]/            (SKU của biến thể)\n");
        content.append("  │    ├── main.jpg      (Ảnh chính của biến thể)\n");
        content.append("  │    ├── 1.jpg         (Ảnh phụ 1)\n");
        content.append("  │    └── 2.jpg         (Ảnh phụ 2)\n");
        content.append("  ├── [SKU2]/\n");
        content.append("  │    ├── main.jpg\n");
        content.append("  │    └── 1.jpg\n");
        content.append("  └── ...\n\n");

        content.append("Ví dụ:\n");
        content.append("Images/\n");
        content.append("  ├── ATN001-S-DEN/\n");
        content.append("  │    ├── main.jpg\n");
        content.append("  │    ├── 1.jpg\n");
        content.append("  │    └── 2.jpg\n");
        content.append("  └── ATN001-M-TRG/\n");
        content.append("       ├── main.jpg\n");
        content.append("       └── 1.jpg\n\n");

        content.append("III. LƯU Ý QUAN TRỌNG\n");
        content.append("- Hệ thống sẽ tự động sinh SKU nếu bạn để trống trong file Excel\n");
        content.append("- Nếu tự sinh SKU, bạn cần điền lại file Excel trước khi chuẩn bị ảnh\n");
        content.append("- SKU được sinh theo định dạng: [Mã SP]-[Kích thước]-[Màu sắc]\n");
        content.append("- File main.jpg sẽ được sử dụng làm ảnh chính của biến thể\n");
        content.append("- Các file ảnh phụ (1.jpg, 2.jpg, vv) sẽ được liên kết với sản phẩm\n");
        content.append("- Định dạng ảnh hỗ trợ: JPG, PNG, WebP\n");
        content.append("- Kích thước ảnh tối đa: 5MB\n");
        content.append("- Không sử dụng ký tự đặc biệt trong tên thư mục và tên file\n");

        return content.toString();
    }

    /**
     * Xử lý import sản phẩm từ file ZIP chứa Excel và ảnh, với báo cáo lỗi
     */
    public ImportResult importProductsFromExcel(MultipartFile zipFile) {
        Workbook workbook = null;

        try {
            // Xử lý file ZIP và trích xuất Excel + ảnh
            ZipImportProcessor.ImportData importData = new ZipImportProcessor().processZipFile(zipFile);
            workbook = importData.getWorkbook();

            // Kiểm tra workbook
            if (workbook == null) {
                throw new FileHandlingException("Không thể đọc file Excel từ ZIP");
            }

            // Tạo đối tượng báo cáo lỗi
            ExcelErrorReport errorReport = new ExcelErrorReport(workbook);

            // Xử lý sheet sản phẩm
            Sheet productSheet = workbook.getSheet("Sản Phẩm");
            if (productSheet == null) {
                throw new FileHandlingException("File Excel không chứa sheet 'Sản Phẩm'");
            }

            // Xử lý sheet biến thể
            Sheet variantSheet = workbook.getSheet("Biến Thể");
            if (variantSheet == null) {
                throw new FileHandlingException("File Excel không chứa sheet 'Biến Thể'");
            }

            // Lấy danh sách categories và brands để lookup
            Map<String, Long> categoryMap = getCategoryMapByName();
            Map<String, Long> brandMap = getBrandMapByName();

            // Xử lý dữ liệu sản phẩm với báo cáo lỗi
            Map<Integer, Long> productRowIdMap = processProductSheetWithValidation(productSheet, categoryMap, brandMap, errorReport);

            // Xử lý dữ liệu biến thể với ảnh từ ZIP và báo cáo lỗi
            processVariantSheetWithSkuImagesAndValidation(variantSheet, productRowIdMap, importData, errorReport);

            // Đánh dấu tất cả lỗi trong file Excel
            errorReport.markErrors();

            // Tạo kết quả import
            ImportResult result = new ImportResult();
            result.setSuccess(!errorReport.hasErrors());
            result.setTotalErrors(errorReport.getTotalErrors());
            result.setErrorRowCount(errorReport.getErrorRowCount());

            // Nếu có lỗi, trả về file Excel đã đánh dấu lỗi
            if (errorReport.hasErrors()) {
                result.setErrorReportBytes(errorReport.toByteArray());
                result.setMessage("Import không thành công. Có " + errorReport.getTotalErrors()
                        + " lỗi trong " + errorReport.getErrorRowCount()
                        + " dòng. Vui lòng kiểm tra file báo cáo lỗi.");
            } else {
                result.setMessage("Import thành công. Đã nhập " + productRowIdMap.size() + " sản phẩm.");
            }

            return result;
        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ ZIP: ", e);
            ImportResult result = new ImportResult();
            result.setSuccess(false);
            result.setMessage("Lỗi khi import sản phẩm: " + e.getMessage());
            return result;
        } finally {
            // Đảm bảo workbook được đóng đúng cách
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("Lỗi khi đóng workbook: ", e);
                }
            }
        }
    }

    /**
     * Lớp chứa kết quả import
     */
    @Getter
    @Setter
    public static class ImportResult {
        private boolean success;
        private String message;
        private int totalErrors;
        private int errorRowCount;
        private byte[] errorReportBytes;
    }

    /**
     * Xử lý sheet sản phẩm với validation và báo cáo lỗi
     */
    private Map<Integer, Long> processProductSheetWithValidation(
            Sheet productSheet,
            Map<String, Long> categoryMap,
            Map<String, Long> brandMap,
            ExcelErrorReport errorReport) {

        Map<Integer, Long> productRowIdMap = new HashMap<>();
        int numRows = productSheet.getPhysicalNumberOfRows();

        // Bỏ qua hàng header
        for (int i = 1; i < numRows; i++) {
            Row row = productSheet.getRow(i);
            if (row == null) continue;

            // Đọc dữ liệu sản phẩm từ Excel
            String name = getCellStringValue(row.getCell(0));
            String categoryName = getCellStringValue(row.getCell(1));
            String brandName = getCellStringValue(row.getCell(2));
            String description = getCellStringValue(row.getCell(3));
            BigDecimal price = getCellBigDecimalValue(row.getCell(4));
            BigDecimal salePrice = getCellBigDecimalValue(row.getCell(5));
            String slug = getCellStringValue(row.getCell(6));

            // Validate dữ liệu bắt buộc
            boolean hasError = false;

            if (name == null || name.isEmpty()) {
                errorReport.addError("Sản Phẩm", i, "Tên sản phẩm không được để trống");
                hasError = true;
            }

            if (categoryName == null || categoryName.isEmpty()) {
                errorReport.addError("Sản Phẩm", i, "Loại sản phẩm không được để trống");
                hasError = true;
            } else if (!categoryMap.containsKey(categoryName)) {
                errorReport.addError("Sản Phẩm", i, "Loại sản phẩm '" + categoryName + "' không tồn tại");
                hasError = true;
            }

            if (brandName == null || brandName.isEmpty()) {
                errorReport.addError("Sản Phẩm", i, "Thương hiệu không được để trống");
                hasError = true;
            } else if (!brandMap.containsKey(brandName)) {
                errorReport.addError("Sản Phẩm", i, "Thương hiệu '" + brandName + "' không tồn tại");
                hasError = true;
            }

            if (price == null) {
                errorReport.addError("Sản Phẩm", i, "Giá sản phẩm không được để trống");
                hasError = true;
            } else if (price.compareTo(BigDecimal.ZERO) <= 0) {
                errorReport.addError("Sản Phẩm", i, "Giá sản phẩm phải lớn hơn 0");
                hasError = true;
            }

            if (salePrice != null && price != null && salePrice.compareTo(price) >= 0) {
                errorReport.addError("Sản Phẩm", i, "Giá khuyến mãi phải nhỏ hơn giá gốc");
                hasError = true;
            }

            // Nếu có lỗi, bỏ qua dòng này
            if (hasError) {
                continue;
            }

            // Tìm category và brand ID
            Long categoryId = categoryMap.get(categoryName);
            Long brandId = brandMap.get(brandName);

            // Tạo DTO sản phẩm
            ProductCreateDTO productDTO = ProductCreateDTO.builder()
                    .name(name)
                    .categoryId(categoryId)
                    .brandId(brandId)
                    .description(description)
                    .price(price)
                    .salePrice(salePrice)
                    .slug(slug)
                    .status(true)
                    .build();

            // Lưu sản phẩm vào DB
            try {
                ProductResponseDTO savedProduct = productService.createProduct(productDTO, null);

                // Lưu mapping giữa row index và product ID
                productRowIdMap.put(i, savedProduct.getProductId());
                log.info("Đã tạo sản phẩm: {} với ID: {}", name, savedProduct.getProductId());
            } catch (Exception e) {
                errorReport.addError("Sản Phẩm", i, "Lỗi khi tạo sản phẩm: " + e.getMessage());
                log.error("Lỗi khi tạo sản phẩm tại dòng {}: {}", i + 1, e.getMessage());
            }
        }

        return productRowIdMap;
    }

    /**
     * Xử lý sheet biến thể với validation, ảnh từ ZIP và báo cáo lỗi
     */
    private void processVariantSheetWithSkuImagesAndValidation(
            Sheet variantSheet,
            Map<Integer, Long> productRowIdMap,
            ZipImportProcessor.ImportData importData,
            ExcelErrorReport errorReport) {

        int numRows = variantSheet.getPhysicalNumberOfRows();
        Map<Long, List<BulkProductVariantCreateDTO.VariantDetail>> productVariantsMap = new HashMap<>();
        Map<Integer, Long> rowToProductIdMap = new HashMap<>();
        Map<Integer, BulkProductVariantCreateDTO.VariantDetail> rowToVariantMap = new HashMap<>();

        // Bỏ qua hàng header
        for (int i = 1; i < numRows; i++) {
            Row row = variantSheet.getRow(i);
            if (row == null) continue;

            // Đọc dữ liệu biến thể từ Excel
            Double productRowId = getCellDoubleValue(row.getCell(0));
            String size = getCellStringValue(row.getCell(1));
            String color = getCellStringValue(row.getCell(2));
            String sku = getCellStringValue(row.getCell(3)); // Có thể null
            Integer stockQuantity = getCellIntValue(row.getCell(4));

            // Validate dữ liệu bắt buộc
            boolean hasError = false;

            if (productRowId == null) {
                errorReport.addError("Biến Thể", i, "Mã sản phẩm không được để trống");
                hasError = true;
            } else if (!productRowIdMap.containsKey(productRowId.intValue())) {
                errorReport.addError("Biến Thể", i, "Không tìm thấy sản phẩm với ID dòng: " + productRowId.intValue());
                hasError = true;
            }

            if (size == null || size.isEmpty()) {
                errorReport.addError("Biến Thể", i, "Kích thước không được để trống");
                hasError = true;
            }

            if (color == null || color.isEmpty()) {
                errorReport.addError("Biến Thể", i, "Màu sắc không được để trống");
                hasError = true;
            }

            if (stockQuantity == null) {
                errorReport.addError("Biến Thể", i, "Số lượng tồn kho không được để trống");
                hasError = true;
            } else if (stockQuantity < 0) {
                errorReport.addError("Biến Thể", i, "Số lượng tồn kho không được âm");
                hasError = true;
            }

            // Nếu có lỗi, bỏ qua dòng này
            if (hasError) {
                continue;
            }

            // Lấy product ID từ mapping
            Long productId = productRowIdMap.get(productRowId.intValue());

            // Tạo đối tượng chi tiết biến thể
            BulkProductVariantCreateDTO.VariantDetail variantDetail = new BulkProductVariantCreateDTO.VariantDetail();
            variantDetail.setSize(size);
            variantDetail.setColor(color);
            variantDetail.setSku(sku); // Có thể null, sẽ được tự động sinh
            variantDetail.setStockQuantity(stockQuantity);

            // Thêm vào map để xử lý hàng loạt
            if (!productVariantsMap.containsKey(productId)) {
                productVariantsMap.put(productId, new ArrayList<>());
            }
            productVariantsMap.get(productId).add(variantDetail);

            // Lưu mapping từ row đến product ID và variant
            rowToProductIdMap.put(i, productId);
            rowToVariantMap.put(i, variantDetail);
        }

        // Map để lưu SKU đã tạo cho mỗi row
        Map<Integer, String> rowToSkuMap = new HashMap<>();

        // Xử lý từng sản phẩm và các biến thể của nó
        for (Map.Entry<Long, List<BulkProductVariantCreateDTO.VariantDetail>> entry : productVariantsMap.entrySet()) {
            Long productId = entry.getKey();
            List<BulkProductVariantCreateDTO.VariantDetail> variants = entry.getValue();

            try {
                // Tạo đối tượng bulk create DTO
                BulkProductVariantCreateDTO bulkDTO = new BulkProductVariantCreateDTO();
                bulkDTO.setProductId(productId);
                bulkDTO.setVariants(variants);

                // Gọi service để tạo hàng loạt biến thể và lấy về các SKU đã tạo
                List<String> createdSkus = productVariantService.createBulkVariants(bulkDTO);

                // Cập nhật SKU cho tracking
                for (int i = 0; i < variants.size(); i++) {
                    // Tìm row tương ứng
                    for (Map.Entry<Integer, BulkProductVariantCreateDTO.VariantDetail> variantEntry : rowToVariantMap.entrySet()) {
                        if (variantEntry.getValue() == variants.get(i) &&
                                productId.equals(rowToProductIdMap.get(variantEntry.getKey())) &&
                                i < createdSkus.size()) {

                            rowToSkuMap.put(variantEntry.getKey(), createdSkus.get(i));
                            break;
                        }
                    }
                }

                // Xử lý ảnh cho mỗi biến thể
                for (String sku : createdSkus) {
                    processSingleVariantImages(sku, importData, productId);
                }

                log.info("Đã tạo {} biến thể cho sản phẩm ID: {}", variants.size(), productId);
            } catch (Exception e) {
                log.error("Lỗi khi tạo biến thể cho sản phẩm ID {}: {}", productId, e.getMessage());

                // Tìm các dòng bị lỗi để đánh dấu
                for (Map.Entry<Integer, Long> entry2 : rowToProductIdMap.entrySet()) {
                    if (productId.equals(entry2.getValue())) {
                        errorReport.addError("Biến Thể", entry2.getKey(),
                                "Lỗi khi tạo biến thể: " + e.getMessage());
                    }
                }
            }
        }

        // Báo cáo lỗi ảnh thiếu
        for (Map.Entry<Integer, String> entry : rowToSkuMap.entrySet()) {
            String sku = entry.getValue();
            int rowIndex = entry.getKey();

            if (!importData.hasImagesForSku(sku)) {
                errorReport.addError("Biến Thể", rowIndex,
                        "Không tìm thấy ảnh cho biến thể có SKU: " + sku);
            }
        }
    }

    /**
     * Xử lý ảnh cho một biến thể cụ thể
     */
    private void processSingleVariantImages(String sku, ZipImportProcessor.ImportData importData, Long productId) {
        try {
            if (importData.hasImagesForSku(sku)) {
                // Lấy ảnh chính (main.jpg) cho biến thể
                byte[] mainImageData = importData.getMainImageForSku(sku);
                if (mainImageData != null) {
                    // Tạo MultipartFile từ byte array
                    MultipartFile mainImageFile = createMultipartFile(mainImageData, "main.jpg", "image/jpeg");
                    // Cập nhật ảnh cho biến thể
                    productVariantService.updateVariantImageBySku(sku, mainImageFile);
                }

                // Lấy các ảnh phụ và liên kết với sản phẩm
                List<byte[]> secondaryImagesData = importData.getSecondaryImagesForSku(sku);
                if (!secondaryImagesData.isEmpty()) {
                    // Tìm ID của biến thể để lấy ProductID
                    ProductVariantResponseDTO variant = productVariantService.getVariantBySku(sku);
                    if (variant != null) {
                        Long variantProductId = variant.getProduct().getProductId();

                        // Tạo danh sách MultipartFile cho ảnh phụ
                        List<MultipartFile> secondaryImageFiles = new ArrayList<>();
                        int imageIndex = 1;
                        for (byte[] imageData : secondaryImagesData) {
                            MultipartFile imageFile = createMultipartFile(
                                    imageData,
                                    imageIndex + ".jpg",
                                    "image/jpeg"
                            );
                            secondaryImageFiles.add(imageFile);
                            imageIndex++;
                        }

                        // Upload ảnh phụ cho sản phẩm
                        if (!secondaryImageFiles.isEmpty()) {
                            productImageService.uploadProductImages(variantProductId, secondaryImageFiles);
                        }
                    }
                }
            } else {
                log.warn("Không tìm thấy ảnh cho biến thể có SKU: {}", sku);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý ảnh cho biến thể có SKU {}: {}", sku, e.getMessage());
        }
    }

    /**
     * Tạo MultipartFile từ byte array
     */
    private MultipartFile createMultipartFile(byte[] data, String filename, String contentType) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "file";
            }

            @Override
            public String getOriginalFilename() {
                return filename;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public boolean isEmpty() {
                return data == null || data.length == 0;
            }

            @Override
            public long getSize() {
                return data != null ? data.length : 0;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return data;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(data);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream stream = new FileOutputStream(dest)) {
                    stream.write(data);
                    stream.flush();
                }
            }
        };
    }

    /**
     * Lấy map kết nối tên category với ID
     */
    private Map<String, Long> getCategoryMapByName() {
        try {
            List<Categories> categories = categoryRepository.findAll();
            return categories.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getStatus()))
                    .collect(Collectors.toMap(
                            Categories::getName,
                            Categories::getCategoryId,
                            (existing, replacement) -> existing // Nếu có duplicate, giữ giá trị đầu tiên
                    ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách category: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Lấy map kết nối tên brand với ID
     */
    private Map<String, Long> getBrandMapByName() {
        try {
            List<Brands> brands = brandRepository.findAll();
            return brands.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getStatus()))
                    .collect(Collectors.toMap(
                            Brands::getName,
                            Brands::getBrandId,
                            (existing, replacement) -> existing // Nếu có duplicate, giữ giá trị đầu tiên
                    ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách brand: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Helper method để lấy giá trị chuỗi từ cell
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    // Xử lý trường hợp số nguyên
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e) {
                        try {
                            return String.valueOf(cell.getNumericCellValue());
                        } catch (Exception ex) {
                            return "";
                        }
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("Lỗi khi đọc giá trị chuỗi từ cell: ", e);
            return null;
        }
    }

    /**
     * Helper method để lấy giá trị số từ cell
     */
    private Double getCellDoubleValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    try {
                        return Double.parseDouble(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                case FORMULA:
                    try {
                        return cell.getNumericCellValue();
                    } catch (Exception e) {
                        try {
                            return Double.parseDouble(cell.getStringCellValue().trim());
                        } catch (Exception ex) {
                            return null;
                        }
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("Lỗi khi đọc giá trị số từ cell: ", e);
            return null;
        }
    }

    /**
     * Helper method để lấy giá trị số nguyên từ cell
     */
    private Integer getCellIntValue(Cell cell) {
        Double value = getCellDoubleValue(cell);
        return value != null ? value.intValue() : null;
    }

    /**
     * Helper method để lấy giá trị BigDecimal từ cell
     */
    private BigDecimal getCellBigDecimalValue(Cell cell) {
        Double value = getCellDoubleValue(cell);
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}