package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Products.ProductCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Brands;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Repository.BrandRepository;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import com.example.api_sell_clothes_v1.Utils.ExcelErrorReport;
import com.example.api_sell_clothes_v1.Utils.SlugGenerator;
import com.example.api_sell_clothes_v1.Utils.ZipImportNoProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dịch vụ chuyên xử lý import/export sản phẩm cơ bản qua Excel (không bao gồm biến thể)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSingleExcelService {
    private final ProductService productService;
    private final ProductThumbnailService thumbnailService;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SlugGenerator slugGenerator;

    private static final String[] PRODUCT_HEADERS = {
            "Tên Sản Phẩm (*)", "Loại Sản Phẩm (*)", "Thương Hiệu (*)",
            "Mô Tả", "Giá Gốc (*)", "Giá Khuyến Mãi", "Slug (Tự động)", "Ảnh Thumbnail"
    };

    /**
     * Lớp chứa kết quả import sản phẩm
     */
    @Getter
    @Setter
    public static class ProductImportResult {
        private boolean success;
        private String message;
        private int totalErrors;
        private int errorRowCount;
        private byte[] errorReportBytes;
        private int totalImported;
        private List<Long> productIds;
    }

    /**
     * Tạo file Excel mẫu cho sản phẩm
     */
    public void generateProductTemplateFile(HttpServletResponse response) throws IOException {
        // Thiết lập header cẩn thận
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"product_template.xlsx\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Sử dụng ByteArrayOutputStream trước để đảm bảo dữ liệu hoàn chỉnh
        try (Workbook workbook = createProductTemplateWorkbook()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(16384); // 16KB ban đầu
            workbook.write(baos);
            baos.flush();

            // Lấy mảng byte hoàn chỉnh
            byte[] excelBytes = baos.toByteArray();

            // Ghi rõ kích thước content-length
            response.setContentLength(excelBytes.length);

            // Ghi dữ liệu hoàn chỉnh vào response
            try (OutputStream out = new BufferedOutputStream(response.getOutputStream(), 8192)) {
                out.write(excelBytes);
                out.flush();
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo template sản phẩm: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Không thể tạo template: " + e.getMessage());
        }
    }

    /**
     * Tạo file ZIP chứa template Excel và thư mục thumbnail mẫu
     */
    public void generateProductTemplatePackage(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"product_template_package.zip\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        int bufferSize = 8192 * 4; // 32KB buffer
        response.setBufferSize(bufferSize);

        try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream(), bufferSize))) {
            // Tạo và thêm file Excel vào ZIP
            try (Workbook workbook = createProductTemplateWorkbook()) {
                ZipEntry excelEntry = new ZipEntry("product_template.xlsx");
                zipOut.putNextEntry(excelEntry);
                workbook.write(zipOut);
                zipOut.closeEntry();
            }

            // Tạo cấu trúc thư mục ảnh mẫu
            // Tạo thư mục Images/thumbnails
            ZipEntry thumbnailFolder = new ZipEntry("Images/thumbnails/");
            zipOut.putNextEntry(thumbnailFolder);
            zipOut.closeEntry();

            // Thêm 2 ảnh thumbnail mẫu
            byte[] sampleImage = createSampleImage();

            ZipEntry thumbnail1Entry = new ZipEntry("Images/thumbnails/thumbnail1.jpg");
            zipOut.putNextEntry(thumbnail1Entry);
            zipOut.write(sampleImage);
            zipOut.closeEntry();

            ZipEntry thumbnail2Entry = new ZipEntry("Images/thumbnails/thumbnail2.jpg");
            zipOut.putNextEntry(thumbnail2Entry);
            zipOut.write(sampleImage);
            zipOut.closeEntry();

            // Thêm file README.txt
            ZipEntry readmeEntry = new ZipEntry("README.txt");
            zipOut.putNextEntry(readmeEntry);
            String instructions = createInstructionText();
            zipOut.write(instructions.getBytes("UTF-8"));
            zipOut.closeEntry();

            zipOut.finish();
            zipOut.flush();
        } catch (Exception e) {
            log.error("Lỗi khi tạo package template sản phẩm: ", e);
            throw new IOException("Không thể tạo package template sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Import sản phẩm từ file Excel
     */
    public ProductImportResult importProductsFromExcel(MultipartFile file) {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            log.info("Bắt đầu import sản phẩm từ file Excel: {}", file.getOriginalFilename());
            return importProductsFromWorkbook(workbook, null);
        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ Excel: ", e);
            ProductImportResult result = new ProductImportResult();
            result.setSuccess(false);
            result.setMessage("Lỗi khi import sản phẩm: " + e.getMessage());
            return result;
        }
    }

    /**
     * Import sản phẩm từ file ZIP (Excel + ảnh thumbnail)
     */
    public ProductImportResult importProductsFromZip(MultipartFile zipFile) {
        try {
            log.info("Bắt đầu import sản phẩm từ file ZIP: {}", zipFile.getOriginalFilename());

            ZipImportNoProcessor.ImportData importData = new ZipImportNoProcessor().processZipFile(zipFile);
            Workbook workbook = importData.getWorkbook();

            if (workbook == null) {
                log.error("Không thể đọc file Excel từ ZIP: {}", zipFile.getOriginalFilename());
                throw new FileHandlingException("Không thể đọc file Excel từ ZIP");
            }

            // Ghi log chi tiết về cấu trúc file ZIP
            log.info("Đã tìm thấy file Excel trong ZIP: {}", importData.getExcelFileName());
            log.info("Tổng số file trong ZIP: {}", importData.getAllFilePaths().size());
            log.info("Danh sách đường dẫn thumbnail có sẵn: {}",
                    importData.getAllFilePaths().stream()
                            .filter(path -> path.contains("thumbnail") || path.contains("thumbnails"))
                            .collect(Collectors.joining(", ")));

            // Tiếp tục xử lý import từ workbook
            ProductImportResult result = importProductsFromWorkbook(workbook, importData);

            if (result.isSuccess()) {
                log.info("Import sản phẩm từ ZIP thành công: {} sản phẩm", result.getTotalImported());
            } else {
                log.warn("Import sản phẩm từ ZIP có lỗi: {} lỗi trong {} dòng",
                        result.getTotalErrors(), result.getErrorRowCount());
            }

            return result;
        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ ZIP: {}", e.getMessage(), e);
            ProductImportResult result = new ProductImportResult();
            result.setSuccess(false);
            result.setMessage("Lỗi khi import sản phẩm: " + e.getMessage());
            return result;
        }
    }

    /**
     * Kiểm tra xem một dòng có phải là dòng ghi chú/lưu ý hay không
     */
    private boolean isNoteRow(Row row) {
        if (row == null) return false;

        // Kiểm tra ô đầu tiên xem có phải là ghi chú hay không
        Cell firstCell = row.getCell(0);
        if (firstCell == null) return false;

        String cellValue = getCellStringValue(firstCell);
        if (cellValue == null) return false;

        // Nếu giá trị bắt đầu bằng "LƯU Ý" hoặc có dạng lưu ý
        if (cellValue.startsWith("LƯU Ý") || cellValue.contains("LƯU Ý")) {
            return true;
        }

        // Kiểm tra nếu dòng không có dữ liệu ở các cột quan trọng (loại SP, thương hiệu, giá)
        // cũng coi như là không phải dòng sản phẩm
        Cell categoryCell = row.getCell(1);
        Cell brandCell = row.getCell(2);
        Cell priceCell = row.getCell(4);

        boolean hasCategory = categoryCell != null && getCellStringValue(categoryCell) != null;
        boolean hasBrand = brandCell != null && getCellStringValue(brandCell) != null;
        boolean hasPrice = priceCell != null && getCellBigDecimalValue(priceCell) != null;

        // Nếu tất cả các cột quan trọng đều trống, coi như không phải dòng sản phẩm
        return !hasCategory && !hasBrand && !hasPrice;
    }

    /**
     * Import sản phẩm từ Workbook - Phiên bản nâng cao, đã cập nhật để bỏ qua các dòng ghi chú
     */
    private ProductImportResult importProductsFromWorkbook(Workbook workbook, ZipImportNoProcessor.ImportData importData) {
        Sheet productSheet = workbook.getSheet("Sản Phẩm");
        if (productSheet == null) {
            productSheet = workbook.getSheetAt(0); // Thử lấy sheet đầu tiên nếu không tìm thấy sheet "Sản Phẩm"
            if (productSheet == null) {
                throw new FileHandlingException("File Excel không chứa sheet sản phẩm");
            }
            log.info("Không tìm thấy sheet 'Sản Phẩm', sử dụng sheet đầu tiên: {}", productSheet.getSheetName());
        }

        ExcelErrorReport errorReport = new ExcelErrorReport(workbook);
        Map<String, Long> categoryMap = getCategoryMapByName();
        Map<String, Long> brandMap = getBrandMapByName();
        List<Long> importedProductIds = new ArrayList<>();

        int numRows = productSheet.getPhysicalNumberOfRows();
        log.info("Số dòng dữ liệu cần xử lý: {}", numRows - 1); // Trừ dòng tiêu đề

        // In ra tất cả loại sản phẩm và thương hiệu có trong hệ thống
        log.info("Các loại sản phẩm có trong hệ thống: {}", categoryMap.keySet());
        log.info("Các thương hiệu có trong hệ thống: {}", brandMap.keySet());

        // Đếm số sản phẩm hợp lệ đã được xử lý
        int validProductCount = 0;
        boolean foundEmptyRows = false;

        // Bỏ qua dòng tiêu đề
        for (int i = 1; i < numRows; i++) {
            Row row = productSheet.getRow(i);
            if (row == null) {
                // Nếu gặp dòng trống, đánh dấu có thể đã hết dữ liệu
                foundEmptyRows = true;
                continue;
            }

            // Kiểm tra xem dòng hiện tại có phải là dòng ghi chú không
            if (isNoteRow(row)) {
                log.info("Bỏ qua dòng {}: Đây là dòng ghi chú hoặc lưu ý", i);
                continue;
            }

            // Nếu đã gặp vài dòng trống và sau đó là dòng ghi chú, coi như đã hết dữ liệu
            if (foundEmptyRows && validProductCount > 0) {
                log.info("Đã tìm thấy dòng trống sau khi xử lý {} sản phẩm. Dừng import.", validProductCount);
                break;
            }

            String name = getCellStringValue(row.getCell(0));
            String categoryName = getCellStringValue(row.getCell(1));
            String brandName = getCellStringValue(row.getCell(2));
            String description = getCellStringValue(row.getCell(3));
            BigDecimal price = getCellBigDecimalValue(row.getCell(4));
            BigDecimal salePrice = getCellBigDecimalValue(row.getCell(5));
            String slug = getCellStringValue(row.getCell(6));
            String thumbnailPath = getCellStringValue(row.getCell(7));

            // Kiểm tra nếu dòng này không có đủ dữ liệu quan trọng
            if ((name == null || name.isEmpty()) &&
                    (categoryName == null || categoryName.isEmpty()) &&
                    (brandName == null || brandName.isEmpty()) &&
                    (price == null)) {
                // Đây có thể là dòng trống hoặc dòng ghi chú, bỏ qua
                foundEmptyRows = true;
                continue;
            }

            // In ra giá trị đọc được từ mỗi dòng để debug
            log.info("Dòng {}: name={}, category={}, brand={}, price={}, thumbnailPath={}",
                    i, name, categoryName, brandName, price, thumbnailPath);

            boolean hasError = false;

            if (name == null || name.isEmpty()) {
                errorReport.addError("Sản Phẩm", i, "Tên sản phẩm không được để trống");
                hasError = true;
            }

            // Xử lý loại sản phẩm với tìm kiếm linh hoạt
            Long categoryId = null;
            if (categoryName == null || categoryName.isEmpty()) {
                errorReport.addError("Sản Phẩm", i, "Loại sản phẩm không được để trống");
                hasError = true;
            } else {
                // Tìm chính xác
                categoryId = categoryMap.get(categoryName);

                // Tìm không phân biệt hoa thường
                if (categoryId == null) {
                    for (Map.Entry<String, Long> entry : categoryMap.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(categoryName)) {
                            categoryId = entry.getValue();
                            categoryName = entry.getKey(); // Lấy tên chính xác
                            log.info("Đã tìm thấy loại sản phẩm '{}' với ID: {}", categoryName, categoryId);
                            break;
                        }
                    }
                }

                if (categoryId == null) {
                    errorReport.addError("Sản Phẩm", i, "Loại sản phẩm '" + categoryName + "' không tồn tại");
                    hasError = true;
                }
            }

            // Xử lý thương hiệu với tìm kiếm linh hoạt
            Long brandId = null;
            if (brandName == null || brandName.isEmpty()) {
                errorReport.addError("Sản Phẩm", i, "Thương hiệu không được để trống");
                hasError = true;
            } else {
                // Tìm chính xác
                brandId = brandMap.get(brandName);

                // Tìm không phân biệt hoa thường
                if (brandId == null) {
                    for (Map.Entry<String, Long> entry : brandMap.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(brandName)) {
                            brandId = entry.getValue();
                            brandName = entry.getKey(); // Lấy tên chính xác
                            log.info("Đã tìm thấy thương hiệu '{}' với ID: {}", brandName, brandId);
                            break;
                        }
                    }
                }

                if (brandId == null) {
                    errorReport.addError("Sản Phẩm", i, "Thương hiệu '" + brandName + "' không tồn tại");
                    hasError = true;
                }
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

            if (hasError) {
                continue;
            }

            // Nếu slug trống, tự động tạo slug từ tên sản phẩm
            if (slug == null || slug.trim().isEmpty()) {
                slug = slugGenerator.generateSlug(name);
            }

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

            try {
                // Xử lý thumbnail nếu có
                MultipartFile thumbnailFile = null;
                if (thumbnailPath != null && !thumbnailPath.isEmpty() && importData != null) {
                    try {
                        // Thử nhiều cách tìm khác nhau với đường dẫn chính xác và không phân biệt hoa thường
                        byte[] thumbnailData = null;
                        String foundPath = null;

                        // Log tất cả đường dẫn hiện có trong ZIP để debug
                        log.info("Đang tìm thumbnail '{}' trong file ZIP.", thumbnailPath);

                        // Cách 1: Tìm trực tiếp với đường dẫn cụ thể (các dạng phổ biến)
                        String[] possiblePaths = {
                                "thumbnails/" + thumbnailPath,
                                "Images/thumbnails/" + thumbnailPath,
                                "images/thumbnails/" + thumbnailPath,
                                "thumbnail/" + thumbnailPath,
                                "Images/thumbnail/" + thumbnailPath,
                                "images/thumbnail/" + thumbnailPath,
                                thumbnailPath // Thử tìm tên file trực tiếp
                        };

                        // In ra tất cả đường dẫn sẽ thử tìm kiếm
                        log.info("Sẽ tìm kiếm theo các đường dẫn: {}", Arrays.toString(possiblePaths));

                        for (String path : possiblePaths) {
                            thumbnailData = importData.getFileContent(path);
                            if (thumbnailData != null) {
                                foundPath = path;
                                log.info("Đã tìm thấy thumbnail tại đường dẫn: {}", foundPath);
                                break;
                            }
                        }

                        // Cách 2: Tìm không phân biệt hoa thường
                        if (thumbnailData == null) {
                            log.info("Thử tìm không phân biệt hoa thường...");
                            for (String path : importData.getAllFilePaths()) {
                                if ((path.toLowerCase().contains("thumbnail") ||
                                        path.toLowerCase().contains("thumbnails")) &&
                                        path.toLowerCase().endsWith(thumbnailPath.toLowerCase())) {
                                    thumbnailData = importData.getFileContent(path);
                                    foundPath = path;
                                    log.info("Đã tìm thấy thumbnail không phân biệt hoa thường tại: {}", foundPath);
                                    break;
                                }
                            }
                        }

                        // Cách 3: Tìm chỉ theo tên file, không quan tâm thư mục
                        if (thumbnailData == null) {
                            log.info("Thử tìm theo tên file không quan tâm thư mục...");
                            for (String path : importData.getAllFilePaths()) {
                                if (path.toLowerCase().endsWith(thumbnailPath.toLowerCase())) {
                                    thumbnailData = importData.getFileContent(path);
                                    foundPath = path;
                                    log.info("Đã tìm thấy thumbnail theo tên file tại: {}", foundPath);
                                    break;
                                }
                            }
                        }

                        if (thumbnailData != null) {
                            thumbnailFile = createMultipartFile(thumbnailData, thumbnailPath,
                                    determineContentType(thumbnailPath));
                            log.info("Đã tạo MultipartFile cho thumbnail: {}", thumbnailPath);
                        } else {
                            log.warn("Không tìm thấy thumbnail '{}' cho sản phẩm '{}'. Vui lòng kiểm tra cấu trúc thư mục và tên file.",
                                    thumbnailPath, name);
                        }
                    } catch (Exception e) {
                        log.warn("Lỗi khi xử lý thumbnail {}: {}", thumbnailPath, e.getMessage());
                    }
                }

                // Tạo sản phẩm với thumbnail (nếu có)
                ProductResponseDTO savedProduct = productService.createProduct(productDTO, thumbnailFile);
                importedProductIds.add(savedProduct.getProductId());
                log.info("Đã tạo sản phẩm: {} với ID: {}", name, savedProduct.getProductId());
                validProductCount++; // Đếm số sản phẩm đã được import thành công
            } catch (Exception e) {
                errorReport.addError("Sản Phẩm", i, "Lỗi khi tạo sản phẩm: " + e.getMessage());
                log.error("Lỗi khi tạo sản phẩm tại dòng {}: {}", i + 1, e.getMessage());
            }
        }

        if (errorReport.hasErrors()) {
            errorReport.markErrors();
            ProductImportResult result = new ProductImportResult();
            result.setSuccess(false);
            result.setTotalErrors(errorReport.getTotalErrors());
            result.setErrorRowCount(errorReport.getErrorRowCount());
            try {
                result.setErrorReportBytes(errorReport.toByteArray());
            } catch (IOException e) {
                log.error("Lỗi khi tạo báo cáo lỗi: ", e);
            }
            result.setMessage("Import không thành công. Có " + errorReport.getTotalErrors() +
                    " lỗi trong " + errorReport.getErrorRowCount() + " dòng.");
            result.setTotalImported(importedProductIds.size());
            result.setProductIds(importedProductIds);
            return result;
        }

        ProductImportResult result = new ProductImportResult();
        result.setSuccess(true);
        result.setTotalImported(importedProductIds.size());
        result.setMessage("Import thành công. Đã nhập " + importedProductIds.size() + " sản phẩm.");
        result.setProductIds(importedProductIds);
        return result;
    }

    /**
     * Tạo Workbook Excel mẫu cho sản phẩm
     */
    private Workbook createProductTemplateWorkbook() {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // Tạo sheet danh sách loại sản phẩm và thương hiệu trước
        Sheet categoriesSheet = createCategoriesSheet(workbook);
        Sheet brandsSheet = createBrandsSheet(workbook);

        // Tạo sheet sản phẩm
        Sheet productSheet = workbook.createSheet("Sản Phẩm");

        // Thiết lập độ rộng cột
        for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
            productSheet.setColumnWidth(i, 4000);
        }
        // Đặt độ rộng lớn hơn cho cột mô tả và cột ảnh thumbnail
        productSheet.setColumnWidth(3, 6000); // Mô tả
        productSheet.setColumnWidth(7, 6000); // Ảnh Thumbnail

        // Tạo style cho tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo row tiêu đề
        Row headerRow = productSheet.createRow(0);
        for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(PRODUCT_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Style cơ bản cho dữ liệu
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Style cho các ô dropdown (Loại sản phẩm, Thương hiệu)
        CellStyle dropdownStyle = workbook.createCellStyle();
        dropdownStyle.cloneStyleFrom(dataStyle);
        dropdownStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        dropdownStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Style cho ô thumbnail
        CellStyle thumbnailStyle = workbook.createCellStyle();
        thumbnailStyle.cloneStyleFrom(dataStyle);
        thumbnailStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        thumbnailStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Style cho ô slug (tự động tạo)
        CellStyle slugStyle = workbook.createCellStyle();
        slugStyle.cloneStyleFrom(dataStyle);
        Font grayFont = workbook.createFont();
        grayFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        grayFont.setItalic(true);
        slugStyle.setFont(grayFont);

        // Tạo dropdown cho chọn danh mục và thương hiệu
        createCategoryDropdown(productSheet, categoriesSheet);
        createBrandDropdown(productSheet, brandsSheet);

        // Thêm dữ liệu mẫu
        for (int i = 1; i <= 3; i++) {
            Row row = productSheet.createRow(i);

            // Tên sản phẩm
            Cell cell0 = row.createCell(0);
            cell0.setCellValue("Sản phẩm mẫu " + i);
            cell0.setCellStyle(dataStyle);

            // Loại sản phẩm - để trống để người dùng chọn từ dropdown
            Cell cell1 = row.createCell(1);
            cell1.setCellStyle(dropdownStyle);

            // Thương hiệu - để trống để người dùng chọn từ dropdown
            Cell cell2 = row.createCell(2);
            cell2.setCellStyle(dropdownStyle);

            // Mô tả
            Cell cell3 = row.createCell(3);
            cell3.setCellValue("Mô tả sản phẩm mẫu " + i);
            cell3.setCellStyle(dataStyle);

            // Giá gốc
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(100000 * i);
            cell4.setCellStyle(dataStyle);

            // Giá khuyến mãi
            Cell cell5 = row.createCell(5);
            cell5.setCellStyle(dataStyle);

            // Slug (tự động tạo từ tên sản phẩm)
            Cell cell6 = row.createCell(6);
            String slugFormula = "IF(A" + (i+1) + "<>\"\", " +
                    "LOWER(SUBSTITUTE(A" + (i+1) + ", \" \", \"-\"))" +
                    ", \"\")";
            cell6.setCellFormula(slugFormula);
            cell6.setCellStyle(slugStyle);

            // Ảnh Thumbnail
            Cell cell7 = row.createCell(7);
            cell7.setCellValue("thumbnail" + i + ".jpg");
            cell7.setCellStyle(thumbnailStyle);
        }

        // Thêm 1 dòng trống để làm ranh giới
        Row emptyRow = productSheet.createRow(4);
        for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
            Cell emptyCell = emptyRow.createCell(i);
            emptyCell.setCellValue("");
        }

        // Thêm ghi chú hướng dẫn
        Row noteCategoryRow = productSheet.createRow(5);
        Cell noteCategoryCell = noteCategoryRow.createCell(0);
        noteCategoryCell.setCellValue("LƯU Ý QUAN TRỌNG: Hãy bắt buộc chọn Loại Sản Phẩm và Thương Hiệu từ danh sách dropdown (những ô có nền màu vàng). KHÔNG ĐƯỢC TỰ NHẬP vào các ô này.");
        CellStyle noteStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        noteStyle.setFont(boldFont);
        noteCategoryCell.setCellStyle(noteStyle);
        productSheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 7));

        Row noteThumbnailRow = productSheet.createRow(6);
        Cell noteThumbnailCell = noteThumbnailRow.createCell(0);
        noteThumbnailCell.setCellValue("LƯU Ý: Ảnh thumbnail là ảnh chính của sản phẩm, nhập tên file ảnh vào ô màu xanh lá (đặt file trong thư mục Images/thumbnails/)");
        noteThumbnailCell.setCellStyle(noteStyle);
        productSheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 7));

        Row noteSlugRow = productSheet.createRow(7);
        Cell noteSlugCell = noteSlugRow.createCell(0);
        noteSlugCell.setCellValue("LƯU Ý: Cột Slug sẽ tự động tạo từ tên sản phẩm. Bạn có thể sửa nếu muốn.");
        noteSlugCell.setCellStyle(noteStyle);
        productSheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 7));

        // Tạo sheet hướng dẫn
        createInstructionSheet(workbook);

        return workbook;
    }

    /**
     * Tạo sheet danh sách loại sản phẩm - Phiên bản nâng cao
     */
    private Sheet createCategoriesSheet(Workbook workbook) {
        // Sử dụng tên hiển thị đầy đủ cho sheet
        Sheet categorySheet = workbook.createSheet("Danh Sách Loại SP");

        categorySheet.setColumnWidth(0, 2000); // ID
        categorySheet.setColumnWidth(1, 6000); // Tên loại
        categorySheet.setColumnWidth(2, 6000); // Mô tả

        // Tạo style cho tiêu đề với nền màu và font đậm
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo row tiêu đề
        Row headerRow = categorySheet.createRow(0);
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("ID");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Tên Loại SP");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Mô Tả");
        cell2.setCellStyle(headerStyle);

        // Tạo style cho dữ liệu với viền
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Tạo style cho cột tên với nền màu nhẹ để nổi bật - dùng cho dropdown
        CellStyle nameStyle = workbook.createCellStyle();
        nameStyle.cloneStyleFrom(dataStyle);
        nameStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        nameStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        try {
            List<Categories> categories = categoryRepository.findAll();
            int rowNum = 1;

            for (Categories category : categories) {
                if (Boolean.TRUE.equals(category.getStatus())) {
                    Row row = categorySheet.createRow(rowNum++);

                    Cell idCell = row.createCell(0);
                    idCell.setCellValue(category.getCategoryId());
                    idCell.setCellStyle(dataStyle);

                    Cell nameCell = row.createCell(1);
                    nameCell.setCellValue(category.getName());
                    nameCell.setCellStyle(nameStyle); // Sử dụng style đặc biệt cho cột tên

                    Cell descCell = row.createCell(2);
                    descCell.setCellValue(category.getDescription() != null ? category.getDescription() : "");
                    descCell.setCellStyle(dataStyle);
                }
            }

            // Log danh sách loại sản phẩm để debug
            log.info("Đã tạo {} loại sản phẩm trong Excel", rowNum - 1);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách loại sản phẩm: ", e);
            createDummyCategoryData(categorySheet, dataStyle, nameStyle);
        }

        return categorySheet;
    }

    /**
     * Tạo sheet danh sách thương hiệu - Phiên bản nâng cao
     */
    private Sheet createBrandsSheet(Workbook workbook) {
        // Sử dụng tên hiển thị đầy đủ cho sheet
        Sheet brandSheet = workbook.createSheet("Danh Sách Thương Hiệu");

        brandSheet.setColumnWidth(0, 2000); // ID
        brandSheet.setColumnWidth(1, 6000); // Tên thương hiệu
        brandSheet.setColumnWidth(2, 6000); // Mô tả

        // Tạo style cho tiêu đề với nền màu và font đậm
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo row tiêu đề
        Row headerRow = brandSheet.createRow(0);
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("ID");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Tên Thương Hiệu");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Mô Tả");
        cell2.setCellStyle(headerStyle);

        // Tạo style cho dữ liệu với viền
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Tạo style cho cột tên với nền màu nhẹ để nổi bật - dùng cho dropdown
        CellStyle nameStyle = workbook.createCellStyle();
        nameStyle.cloneStyleFrom(dataStyle);
        nameStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        nameStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        try {
            List<Brands> brands = brandRepository.findAll();
            int rowNum = 1;

            for (Brands brand : brands) {
                if (Boolean.TRUE.equals(brand.getStatus())) {
                    Row row = brandSheet.createRow(rowNum++);

                    Cell idCell = row.createCell(0);
                    idCell.setCellValue(brand.getBrandId());
                    idCell.setCellStyle(dataStyle);

                    Cell nameCell = row.createCell(1);
                    nameCell.setCellValue(brand.getName());
                    nameCell.setCellStyle(nameStyle); // Sử dụng style đặc biệt cho cột tên

                    Cell descCell = row.createCell(2);
                    descCell.setCellValue(brand.getDescription() != null ? brand.getDescription() : "");
                    descCell.setCellStyle(dataStyle);
                }
            }

            // Log danh sách thương hiệu để debug
            log.info("Đã tạo {} thương hiệu trong Excel", rowNum - 1);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thương hiệu: ", e);
            createDummyBrandData(brandSheet, dataStyle, nameStyle);
        }

        return brandSheet;
    }

    /**
     * Tạo dữ liệu loại sản phẩm mẫu khi không thể lấy từ database
     */
    private void createDummyCategoryData(Sheet sheet, CellStyle dataStyle, CellStyle nameStyle) {
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
            cell0.setCellStyle(dataStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(dummyData[i][1]);
            cell1.setCellStyle(nameStyle); // Sử dụng style đặc biệt cho cột tên

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(dummyData[i][2]);
            cell2.setCellStyle(dataStyle);
        }
    }

    /**
     * Tạo dữ liệu thương hiệu mẫu khi không thể lấy từ database
     */
    private void createDummyBrandData(Sheet sheet, CellStyle dataStyle, CellStyle nameStyle) {
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
            cell0.setCellStyle(dataStyle);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue(dummyData[i][1]);
            cell1.setCellStyle(nameStyle); // Sử dụng style đặc biệt cho cột tên

            Cell cell2 = row.createCell(2);
            cell2.setCellValue(dummyData[i][2]);
            cell2.setCellStyle(dataStyle);
        }
    }

    /**
     * Tạo dropdown cho cột Loại Sản Phẩm - Phiên bản nâng cao, hiệu quả
     */
    private void createCategoryDropdown(Sheet productSheet, Sheet categoriesSheet) {
        try {
            // Phương pháp 1: Lấy danh sách trực tiếp từ sheet
            List<String> categoryValues = new ArrayList<>();
            int lastRow = categoriesSheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                Row row = categoriesSheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(1); // Cột B chứa tên loại sản phẩm
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue();
                        if (value != null && !value.trim().isEmpty()) {
                            categoryValues.add(value);
                        }
                    }
                }
            }

            // Đảm bảo có ít nhất một giá trị trong danh sách
            if (categoryValues.isEmpty()) {
                categoryValues.add("Áo nam");
                categoryValues.add("Quần nam");
                categoryValues.add("Áo nữ");
                categoryValues.add("Quần nữ");
                categoryValues.add("Phụ kiện");
            }

            log.info("Tạo dropdown loại sản phẩm với {} giá trị", categoryValues.size());

            // Tạo DataValidation sử dụng danh sách trực tiếp - hiệu quả nhất
            DataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) productSheet);
            DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(
                    categoryValues.toArray(new String[0])
            );

            // Áp dụng cho cột loại sản phẩm (cột B = index 1)
            CellRangeAddressList addressList = new CellRangeAddressList(1, 100, 1, 1);
            DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);

            // Cấu hình hiển thị
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Loại Sản Phẩm");

            productSheet.addValidationData(validation);

            // Phương pháp 2: Tạo tham chiếu công thức sẵn
            try {
                DataValidationConstraint dvConstraint2 = dvHelper.createFormulaListConstraint(
                        "'Danh Sách Loại SP'!$B$2:$B$" + (lastRow + 1)
                );

                CellRangeAddressList addressList2 = new CellRangeAddressList(1, 100, 1, 1);
                DataValidation validation2 = dvHelper.createValidation(dvConstraint2, addressList2);

                validation2.setShowErrorBox(true);
                validation2.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation2.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Loại Sản Phẩm");

                productSheet.addValidationData(validation2);
            } catch (Exception e) {
                log.warn("Không thể tạo tham chiếu công thức cho loại sản phẩm: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Lỗi khi tạo dropdown loại sản phẩm: ", e);

            // Fallback: Tạo danh sách cứng nếu có lỗi
            String[] defaultCategories = {"Áo nam", "Quần nam", "Áo nữ", "Quần nữ", "Phụ kiện"};

            DataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) productSheet);
            DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(defaultCategories);

            CellRangeAddressList addressList = new CellRangeAddressList(1, 100, 1, 1);
            DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);

            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Loại Sản Phẩm");

            productSheet.addValidationData(validation);
        }
    }

    /**
     * Tạo dropdown cho cột Thương Hiệu - Phiên bản nâng cao, hiệu quả
     */
    private void createBrandDropdown(Sheet productSheet, Sheet brandsSheet) {
        try {
            // Phương pháp 1: Lấy danh sách trực tiếp từ sheet
            List<String> brandValues = new ArrayList<>();
            int lastRow = brandsSheet.getLastRowNum();

            for (int i = 1; i <= lastRow; i++) {
                Row row = brandsSheet.getRow(i);
                if (row != null) {
                    Cell cell = row.getCell(1); // Cột B chứa tên thương hiệu
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String value = cell.getStringCellValue();
                        if (value != null && !value.trim().isEmpty()) {
                            brandValues.add(value);
                        }
                    }
                }
            }

            // Đảm bảo có ít nhất một giá trị trong danh sách
            if (brandValues.isEmpty()) {
                brandValues.add("Nike");
                brandValues.add("Adidas");
                brandValues.add("Zara");
                brandValues.add("H&M");
                brandValues.add("Uniqlo");
            }

            log.info("Tạo dropdown thương hiệu với {} giá trị", brandValues.size());

            // Tạo DataValidation sử dụng danh sách trực tiếp - hiệu quả nhất
            DataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) productSheet);
            DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(
                    brandValues.toArray(new String[0])
            );

            // Áp dụng cho cột thương hiệu (cột C = index 2)
            CellRangeAddressList addressList = new CellRangeAddressList(1, 100, 2, 2);
            DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);

            // Cấu hình hiển thị
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Thương Hiệu");

            productSheet.addValidationData(validation);

            // Phương pháp 2: Tạo tham chiếu công thức sẵn
            try {
                DataValidationConstraint dvConstraint2 = dvHelper.createFormulaListConstraint(
                        "'Danh Sách Thương Hiệu'!$B$2:$B$" + (lastRow + 1)
                );

                CellRangeAddressList addressList2 = new CellRangeAddressList(1, 100, 2, 2);
                DataValidation validation2 = dvHelper.createValidation(dvConstraint2, addressList2);

                validation2.setShowErrorBox(true);
                validation2.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation2.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Thương Hiệu");

                productSheet.addValidationData(validation2);
            } catch (Exception e) {
                log.warn("Không thể tạo tham chiếu công thức cho thương hiệu: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Lỗi khi tạo dropdown thương hiệu: ", e);

            // Fallback: Tạo danh sách cứng nếu có lỗi
            String[] defaultBrands = {"Nike", "Adidas", "Zara", "H&M", "Uniqlo"};

            DataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) productSheet);
            DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(defaultBrands);

            CellRangeAddressList addressList = new CellRangeAddressList(1, 100, 2, 2);
            DataValidation validation = dvHelper.createValidation(dvConstraint, addressList);

            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Thương Hiệu");

            productSheet.addValidationData(validation);
        }
    }

    /**
     * Tạo sheet hướng dẫn
     */
    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Hướng Dẫn");

        sheet.setColumnWidth(0, 10000);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        subtitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        subtitleStyle.setBorderBottom(BorderStyle.THIN);
        subtitleStyle.setBorderTop(BorderStyle.THIN);
        subtitleStyle.setBorderLeft(BorderStyle.THIN);
        subtitleStyle.setBorderRight(BorderStyle.THIN);
        Font subtitleFont = workbook.createFont();
        subtitleFont.setBold(true);
        subtitleStyle.setFont(subtitleFont);

        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);

        // Style cho hướng dẫn quan trọng
        CellStyle importantStyle = workbook.createCellStyle();
        importantStyle.cloneStyleFrom(contentStyle);
        importantStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        importantStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font importantFont = workbook.createFont();
        importantFont.setBold(true);
        importantStyle.setFont(importantFont);

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HƯỚNG DẪN NHẬP SẢN PHẨM");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        String[] instructions = {
                "1. Nhập thông tin sản phẩm vào sheet 'Sản Phẩm'",
                "2. Các trường đánh dấu (*) là bắt buộc phải nhập",
                "3. Sử dụng dropdown để chọn Loại Sản Phẩm và Thương Hiệu từ danh sách có sẵn (các ô có nền màu vàng)",
                "4. Nhập tên file ảnh thumbnail vào cột 'Ảnh Thumbnail' (ô có nền màu xanh lá)",
                "5. Đặt file ảnh thumbnail trong thư mục Images/thumbnails/ khi tạo file ZIP",
                "6. Trường Slug sẽ tự động tạo từ tên sản phẩm, bạn có thể để trống hoặc tùy chỉnh nếu muốn"
        };

        for (int i = 0; i < instructions.length; i++) {
            Row row = sheet.createRow(i + 2);
            Cell cell = row.createCell(0);
            cell.setCellValue(instructions[i]);
            cell.setCellStyle(contentStyle);
            sheet.addMergedRegion(new CellRangeAddress(i + 2, i + 2, 0, 3));
        }

        // Thêm lưu ý quan trọng về dropdown
        Row importantRow = sheet.createRow(instructions.length + 2);
        Cell importantCell = importantRow.createCell(0);
        importantCell.setCellValue("LƯU Ý QUAN TRỌNG: Với các ô Loại Sản Phẩm và Thương Hiệu (ô màu vàng), bạn CHỈ CÓ THỂ CHỌN từ danh sách dropdown, KHÔNG THỂ TỰ NHẬP. Nếu không tìm thấy loại sản phẩm hoặc thương hiệu cần thiết, bạn phải tạo trước trong hệ thống.");
        importantCell.setCellStyle(importantStyle);
        sheet.addMergedRegion(new CellRangeAddress(instructions.length + 2, instructions.length + 2, 0, 3));

        // Thêm hướng dẫn về cách xem danh sách
        Row viewRow = sheet.createRow(instructions.length + 3);
        Cell viewCell = viewRow.createCell(0);
        viewCell.setCellValue("LƯU Ý: Bạn có thể xem danh sách Loại Sản Phẩm và Thương Hiệu đầy đủ trong các sheet 'Danh Sách Loại SP' và 'Danh Sách Thương Hiệu'.");
        viewCell.setCellStyle(importantStyle);
        sheet.addMergedRegion(new CellRangeAddress(instructions.length + 3, instructions.length + 3, 0, 3));

        // Thêm lưu ý khắc phục lỗi dropdown
        Row fixDropdownRow = sheet.createRow(instructions.length + 4);
        Cell fixDropdownCell = fixDropdownRow.createCell(0);
        fixDropdownCell.setCellValue("LƯU Ý QUAN TRỌNG: Nếu không thấy dropdown khi nhấp vào các ô màu vàng, hãy sử dụng Microsoft Excel và đảm bảo bạn đã chọn giá trị chính xác từ danh sách trong sheet 'Danh Sách Loại SP' và 'Danh Sách Thương Hiệu'.");
        fixDropdownCell.setCellStyle(importantStyle);
        sheet.addMergedRegion(new CellRangeAddress(instructions.length + 4, instructions.length + 4, 0, 3));

        // Tiếp tục với các lưu ý khác
        String[] additionalInstructions = {
                "7. Trước khi import, hãy kiểm tra kỹ thông tin đã điền, đặc biệt là Loại Sản Phẩm và Thương Hiệu",
                "8. Đảm bảo file Excel và thư mục Images được đóng gói chính xác trong ZIP file"
        };

        for (int i = 0; i < additionalInstructions.length; i++) {
            Row row = sheet.createRow(instructions.length + 5 + i);
            Cell cell = row.createCell(0);
            cell.setCellValue(additionalInstructions[i]);
            cell.setCellStyle(contentStyle);
            sheet.addMergedRegion(new CellRangeAddress(instructions.length + 5 + i, instructions.length + 5 + i, 0, 3));
        }

        // Thêm hướng dẫn cấu trúc thư mục
        Row folderHeaderRow = sheet.createRow(instructions.length + additionalInstructions.length + 6);
        Cell folderHeaderCell = folderHeaderRow.createCell(0);
        folderHeaderCell.setCellValue("CẤU TRÚC THƯ MỤC KHI TẠO FILE ZIP");
        folderHeaderCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(instructions.length + additionalInstructions.length + 6,
                instructions.length + additionalInstructions.length + 6, 0, 3));

        String[] folderStructure = {
                "your_product_import.zip",
                "├── product_template.xlsx  (File Excel đã điền thông tin)",
                "└── Images/",
                "    └── thumbnails/",
                "        ├── thumbnail1.jpg",
                "        ├── thumbnail2.jpg",
                "        └── ...",
        };

        for (int i = 0; i < folderStructure.length; i++) {
            Row row = sheet.createRow(instructions.length + additionalInstructions.length + 7 + i);
            Cell cell = row.createCell(0);
            cell.setCellValue(folderStructure[i]);
            cell.setCellStyle(contentStyle);
            sheet.addMergedRegion(new CellRangeAddress(instructions.length + additionalInstructions.length + 7 + i,
                    instructions.length + additionalInstructions.length + 7 + i, 0, 3));
        }
    }

    /**
     * Tạo file hướng dẫn dạng text
     */
    private String createInstructionText() {
        StringBuilder content = new StringBuilder();
        content.append("HƯỚNG DẪN NHẬP SẢN PHẨM\n");
        content.append("=======================\n\n");

        content.append("I. HƯỚNG DẪN ĐIỀN THÔNG TIN\n");
        content.append("1. Mở file product_template.xlsx\n");
        content.append("2. Điền thông tin sản phẩm vào sheet 'Sản Phẩm'\n");
        content.append("3. Các trường đánh dấu (*) là bắt buộc phải nhập\n");
        content.append("4. (**) QUAN TRỌNG: Với Loại Sản Phẩm và Thương Hiệu, bạn PHẢI CHỌN từ dropdown có sẵn, KHÔNG ĐƯỢC TỰ NHẬP\n");
        content.append("   * Nhấp vào ô có nền màu vàng và chọn từ danh sách hiện ra\n");
        content.append("   * Có thể xem đầy đủ danh sách trong sheet 'Danh Sách Loại SP' và 'Danh Sách Thương Hiệu'\n");
        content.append("   * Nếu không thấy giá trị cần thiết, vui lòng tạo trước trong hệ thống\n");
        content.append("5. Nhập tên file ảnh thumbnail vào cột 'Ảnh Thumbnail'\n");
        content.append("6. Lưu file Excel sau khi hoàn thành\n\n");

        content.append("II. HƯỚNG DẪN CHUẨN BỊ ẢNH THUMBNAIL\n");
        content.append("1. Tạo thư mục Images/thumbnails/\n");
        content.append("2. Đặt các file ảnh thumbnail vào thư mục này\n");
        content.append("3. Tên file ảnh phải khớp với tên đã nhập trong Excel\n\n");

        content.append("III. HƯỚNG DẪN TẠO FILE ZIP\n");
        content.append("1. Tạo cấu trúc thư mục như sau:\n");
        content.append("   your_product_import.zip\n");
        content.append("   ├── product_template.xlsx  (File Excel đã điền thông tin)\n");
        content.append("   └── Images/\n");
        content.append("       └── thumbnails/\n");
        content.append("           ├── thumbnail1.jpg\n");
        content.append("           ├── thumbnail2.jpg\n");
        content.append("           └── ...\n\n");
        content.append("2. Nén tất cả thành file ZIP\n");
        content.append("3. Tải lên file ZIP qua chức năng 'Nhập Sản Phẩm' trên hệ thống\n\n");

        content.append("IV. LƯU Ý QUAN TRỌNG\n");
        content.append("- Các trường bắt buộc: Tên Sản Phẩm, Loại Sản Phẩm, Thương Hiệu, Giá Gốc\n");
        content.append("- Giá Khuyến Mãi phải nhỏ hơn Giá Gốc\n");
        content.append("- Loại Sản Phẩm và Thương Hiệu BẮT BUỘC phải chọn từ danh sách dropdown\n");
        content.append("- KHÔNG ĐƯỢC TỰ NHẬP vào các ô Loại Sản Phẩm và Thương Hiệu, chỉ được chọn từ danh sách\n");
        content.append("- Thư mục Images/thumbnails/ chứa các ảnh thumbnail\n");
        content.append("- Sau khi nhập sản phẩm thành công, bạn có thể thêm biến thể riêng cho từng sản phẩm\n");

        content.append("\nV. KHẮC PHỤC LỖI DROPDOWN KHÔNG XUẤT HIỆN\n");
        content.append("Nếu bạn không thấy dropdown khi nhấp vào ô Loại SP hoặc Thương Hiệu:\n");
        content.append("1. Hãy mở sheet 'Danh Sách Loại SP' và 'Danh Sách Thương Hiệu' để xem các giá trị hợp lệ\n");
        content.append("2. Quay lại sheet 'Sản Phẩm' và gõ chính xác tên loại sản phẩm/thương hiệu từ danh sách đó\n");
        content.append("3. Đảm bảo bạn đang sử dụng Microsoft Excel để mở file\n");
        content.append("4. Nếu cần, bạn có thể tải template mới và thử lại\n");

        return content.toString();
    }

    /**
     * Lấy danh sách loại sản phẩm dạng Map
     */
    private Map<String, Long> getCategoryMapByName() {
        try {
            List<Categories> categories = categoryRepository.findAll();
            Map<String, Long> result = categories.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getStatus()))
                    .collect(Collectors.toMap(
                            Categories::getName,
                            Categories::getCategoryId,
                            (existing, replacement) -> existing
                    ));

            log.info("Đã lấy {} loại sản phẩm từ database: {}", result.size(), result.keySet());
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách category: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Lấy danh sách thương hiệu dạng Map
     */
    private Map<String, Long> getBrandMapByName() {
        try {
            List<Brands> brands = brandRepository.findAll();
            Map<String, Long> result = brands.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getStatus()))
                    .collect(Collectors.toMap(
                            Brands::getName,
                            Brands::getBrandId,
                            (existing, replacement) -> existing
                    ));

            log.info("Đã lấy {} thương hiệu từ database: {}", result.size(), result.keySet());
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách brand: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Xác định Content-Type dựa trên extension của file
     */
    private String determineContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Tạo MultipartFile từ mảng byte
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
     * Tạo hình ảnh mẫu
     */
    private byte[] createSampleImage() {
        return "Sample Image Content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Lấy giá trị chuỗi từ cell
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
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
     * Lấy giá trị BigDecimal từ cell
     */
    private BigDecimal getCellBigDecimalValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    try {
                        return new BigDecimal(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                case FORMULA:
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue());
                    } catch (Exception e) {
                        try {
                            return new BigDecimal(cell.getStringCellValue().trim());
                        } catch (Exception ex) {
                            return null;
                        }
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("Lỗi khi đọc giá trị BigDecimal từ cell: ", e);
            return null;
        }
    }


    /**
     * Xuất danh sách sản phẩm ra Excel
     */
    public void exportProducts(List<Long> productIds, HttpServletResponse response) throws IOException {
        List<ProductResponseDTO> products = new ArrayList<>();

        // Lấy thông tin sản phẩm theo IDs
        if (productIds != null && !productIds.isEmpty()) {
            for (Long id : productIds) {
                try {
                    ProductResponseDTO product = productService.getProductById(id);
                    products.add(product);
                } catch (Exception e) {
                    log.warn("Không thể lấy thông tin sản phẩm ID {}: {}", id, e.getMessage());
                }
            }
        }

        String fileName = "products_export.xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        int bufferSize = 8192 * 4;
        response.setBufferSize(bufferSize);

        try (Workbook workbook = createProductExportWorkbook(products)) {
            try (OutputStream outputStream = new BufferedOutputStream(response.getOutputStream(), bufferSize)) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("Lỗi khi xuất sản phẩm: ", e);
            throw new IOException("Không thể xuất sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Tạo Workbook xuất danh sách sản phẩm
     */
    private Workbook createProductExportWorkbook(List<ProductResponseDTO> products) {
        Workbook workbook = new XSSFWorkbook();

        // Tạo sheet sản phẩm
        Sheet productSheet = workbook.createSheet("Danh Sách Sản Phẩm");

        // Thiết lập độ rộng cột
        productSheet.setColumnWidth(0, 4000); // ID
        productSheet.setColumnWidth(1, 6000); // Tên
        productSheet.setColumnWidth(2, 4000); // Loại SP
        productSheet.setColumnWidth(3, 4000); // Thương hiệu
        productSheet.setColumnWidth(4, 4000); // Giá
        productSheet.setColumnWidth(5, 4000); // Giá KM
        productSheet.setColumnWidth(6, 5000); // Thumbnail
        productSheet.setColumnWidth(7, 4000); // Ngày tạo
        productSheet.setColumnWidth(8, 3000); // Trạng thái

        // Tạo style cho tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo row tiêu đề
        Row headerRow = productSheet.createRow(0);
        String[] exportHeaders = {"ID", "Tên Sản Phẩm", "Loại Sản Phẩm", "Thương Hiệu",
                "Giá Gốc", "Giá Khuyến Mãi", "Thumbnail", "Ngày Tạo", "Trạng Thái"};
        for (int i = 0; i < exportHeaders.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(exportHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        // Style cho dữ liệu
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Style cho trạng thái
        CellStyle activeStyle = workbook.createCellStyle();
        activeStyle.cloneStyleFrom(dataStyle);
        activeStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        activeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle inactiveStyle = workbook.createCellStyle();
        inactiveStyle.cloneStyleFrom(dataStyle);
        inactiveStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        inactiveStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Format date
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(dataStyle);
        CreationHelper creationHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));

        // Thêm dữ liệu sản phẩm
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = 0; i < products.size(); i++) {
            ProductResponseDTO product = products.get(i);
            Row row = productSheet.createRow(i + 1);

            Cell idCell = row.createCell(0);
            idCell.setCellValue(product.getProductId());
            idCell.setCellStyle(dataStyle);

            Cell nameCell = row.createCell(1);
            nameCell.setCellValue(product.getName());
            nameCell.setCellStyle(dataStyle);

            Cell categoryCell = row.createCell(2);
            categoryCell.setCellValue(product.getCategory().getName());
            categoryCell.setCellStyle(dataStyle);

            Cell brandCell = row.createCell(3);
            brandCell.setCellValue(product.getBrand().getName());
            brandCell.setCellStyle(dataStyle);

            Cell priceCell = row.createCell(4);
            priceCell.setCellValue(product.getPrice().doubleValue());
            priceCell.setCellStyle(dataStyle);

            Cell salePriceCell = row.createCell(5);
            if (product.getSalePrice() != null) {
                salePriceCell.setCellValue(product.getSalePrice().doubleValue());
            } else {
                salePriceCell.setCellValue("");
            }
            salePriceCell.setCellStyle(dataStyle);

            Cell thumbnailCell = row.createCell(6);
            if (product.getThumbnail() != null) {
                thumbnailCell.setCellValue(product.getThumbnail());
            } else {
                thumbnailCell.setCellValue("");
            }
            thumbnailCell.setCellStyle(dataStyle);

            Cell dateCell = row.createCell(7);
            LocalDateTime createdAt = product.getCreatedAt();
            if (createdAt != null) {
                dateCell.setCellValue(createdAt.format(formatter));
            } else {
                dateCell.setCellValue("");
            }
            dateCell.setCellStyle(dataStyle);

            Cell statusCell = row.createCell(8);
            boolean isActive = product.getStatus() != null && product.getStatus();
            statusCell.setCellValue(isActive ? "Hoạt động" : "Không hoạt động");
            statusCell.setCellStyle(isActive ? activeStyle : inactiveStyle);
        }

        return workbook;
    }
}