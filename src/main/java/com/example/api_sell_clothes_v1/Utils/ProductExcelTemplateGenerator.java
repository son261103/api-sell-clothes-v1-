package com.example.api_sell_clothes_v1.Utils;

import com.example.api_sell_clothes_v1.Entity.Brands;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Repository.BrandRepository;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductExcelTemplateGenerator {
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    private static final String[] PRODUCT_HEADERS = {
            "Tên Sản Phẩm (*)", "Loại Sản Phẩm (*)", "Thương Hiệu (*)",
            "Mô Tả", "Giá Gốc (*)", "Giá Khuyến Mãi", "Slug (Tự động)", "Ảnh Thumbnail"
    };

    private static final String[] VARIANT_HEADERS = {
            "STT Sản Phẩm (*)", "Kích Thước (*)", "Màu Sắc (*)",
            "Số Lượng Tồn Kho (*)"
    };

    private static final String[] SKU_LIST_HEADERS = {
            "STT", "Tên Sản Phẩm", "Kích Thước", "Màu Sắc", "SKU", "Thư Mục Ảnh"
    };

    private static final String[] SIZE_OPTIONS = {
            "S", "M", "L", "XL", "XXL", "XXXL",
            "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45"
    };

    private static final String[] COLOR_OPTIONS = {
            "Đen", "Trắng", "Đỏ", "Xanh lá", "Xanh dương", "Vàng", "Cam",
            "Hồng", "Tím", "Nâu", "Xám", "Be", "Xanh ngọc", "Đỏ đô"
    };

    // Ánh xạ ký tự có dấu sang không dấu - dùng cho Excel Formula
    private static final Map<String, String> VIETNAMESE_CHARS_MAPPING = new HashMap<>();

    static {
        // Chữ A
        VIETNAMESE_CHARS_MAPPING.put("Á", "A");
        VIETNAMESE_CHARS_MAPPING.put("À", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ả", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ã", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ạ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ă", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ắ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ằ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ẳ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ẵ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ặ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Â", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ấ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ầ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ẩ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ẫ", "A");
        VIETNAMESE_CHARS_MAPPING.put("Ậ", "A");

        // Chữ E
        VIETNAMESE_CHARS_MAPPING.put("É", "E");
        VIETNAMESE_CHARS_MAPPING.put("È", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ẻ", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ẽ", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ẹ", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ê", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ế", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ề", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ể", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ễ", "E");
        VIETNAMESE_CHARS_MAPPING.put("Ệ", "E");

        // Chữ I
        VIETNAMESE_CHARS_MAPPING.put("Í", "I");
        VIETNAMESE_CHARS_MAPPING.put("Ì", "I");
        VIETNAMESE_CHARS_MAPPING.put("Ỉ", "I");
        VIETNAMESE_CHARS_MAPPING.put("Ĩ", "I");
        VIETNAMESE_CHARS_MAPPING.put("Ị", "I");

        // Chữ O
        VIETNAMESE_CHARS_MAPPING.put("Ó", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ò", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ỏ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Õ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ọ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ô", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ố", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ồ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ổ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ỗ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ộ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ơ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ớ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ờ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ở", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ỡ", "O");
        VIETNAMESE_CHARS_MAPPING.put("Ợ", "O");

        // Chữ U
        VIETNAMESE_CHARS_MAPPING.put("Ú", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ù", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ủ", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ũ", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ụ", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ư", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ứ", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ừ", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ử", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ữ", "U");
        VIETNAMESE_CHARS_MAPPING.put("Ự", "U");

        // Chữ Y
        VIETNAMESE_CHARS_MAPPING.put("Ý", "Y");
        VIETNAMESE_CHARS_MAPPING.put("Ỳ", "Y");
        VIETNAMESE_CHARS_MAPPING.put("Ỷ", "Y");
        VIETNAMESE_CHARS_MAPPING.put("Ỹ", "Y");
        VIETNAMESE_CHARS_MAPPING.put("Ỵ", "Y");

        // Chữ D
        VIETNAMESE_CHARS_MAPPING.put("Đ", "D");
    }

    /**
     * Tạo workbook Excel template hoàn chỉnh
     */
    public Workbook createTemplateWorkbook() {
        Workbook workbook = new XSSFWorkbook();

        // Tạo các sheet tham chiếu trước
        Sheet categoriesSheet = createCategoriesSheet(workbook);
        Sheet brandsSheet = createBrandsSheet(workbook);
        Sheet sizesSheet = createSizesSheet(workbook);
        Sheet colorsSheet = createColorsSheet(workbook);

        // Tạo sheet bảng tra cứu chữ cái tiếng Việt không dấu
        createVietnameseCharsSheet(workbook);

        // Tạo các sheet chính
        Sheet productSheet = createProductSheet(workbook, categoriesSheet, brandsSheet);
        Sheet variantSheet = createVariantSheet(workbook, productSheet, sizesSheet, colorsSheet);

        // Tạo sheet danh sách SKU
        createSkuListSheet(workbook, productSheet, variantSheet);

        // Tạo các sheet hướng dẫn
        createInstructionSheet(workbook);
        createFolderStructureSheet(workbook);

        return workbook;
    }

    /**
     * Tạo file ZIP chứa file Excel mẫu và thư mục ảnh mẫu
     */
    public void createFullTemplatePackage(HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=product_import_template.zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // 1. Tạo và thêm file Excel vào ZIP
            try (Workbook workbook = createTemplateWorkbook()) {
                ZipEntry excelEntry = new ZipEntry("product_import.xlsx");
                zipOut.putNextEntry(excelEntry);
                workbook.write(zipOut);
                zipOut.closeEntry();
            }

            // 2. Tạo thư mục Images/ và thêm ảnh mẫu
            // Lưu ý: Sử dụng ví dụ không dấu để đồng bộ với backend
            String[] sampleSkus = {"ATN001-S-DEN", "ATN001-M-TRG"};
            for (String sku : sampleSkus) {
                // Tạo thư mục con cho SKU
                ZipEntry folderEntry = new ZipEntry("Images/" + sku + "/");
                zipOut.putNextEntry(folderEntry);
                zipOut.closeEntry();

                // Thêm ảnh main.jpg mẫu
                byte[] sampleImage = createSampleImage();
                ZipEntry mainImageEntry = new ZipEntry("Images/" + sku + "/main.jpg");
                zipOut.putNextEntry(mainImageEntry);
                zipOut.write(sampleImage);
                zipOut.closeEntry();

                // Thêm ảnh phụ 1.jpg mẫu
                ZipEntry secondaryImageEntry = new ZipEntry("Images/" + sku + "/1.jpg");
                zipOut.putNextEntry(secondaryImageEntry);
                zipOut.write(sampleImage);
                zipOut.closeEntry();

                // Thêm ảnh phụ 2.jpg mẫu
                ZipEntry thirdImageEntry = new ZipEntry("Images/" + sku + "/2.jpg");
                zipOut.putNextEntry(thirdImageEntry);
                zipOut.write(sampleImage);
                zipOut.closeEntry();
            }

            // Thêm mẫu thumbnail
            ZipEntry thumbnailEntry = new ZipEntry("Images/thumbnails/");
            zipOut.putNextEntry(thumbnailEntry);
            zipOut.closeEntry();

            // Thêm thumbnail mẫu
            ZipEntry thumbnailSampleEntry = new ZipEntry("Images/thumbnails/thumbnail1.jpg");
            zipOut.putNextEntry(thumbnailSampleEntry);
            zipOut.write(createSampleImage());
            zipOut.closeEntry();

            ZipEntry thumbnailSample2Entry = new ZipEntry("Images/thumbnails/thumbnail2.jpg");
            zipOut.putNextEntry(thumbnailSample2Entry);
            zipOut.write(createSampleImage());
            zipOut.closeEntry();

            zipOut.finish();
            zipOut.flush();
        } catch (Exception e) {
            log.error("Lỗi khi tạo file ZIP template: ", e);
            throw new IOException("Không thể tạo file ZIP template: " + e.getMessage());
        }
    }

    /**
     * Tạo sheet ánh xạ ký tự tiếng Việt sang không dấu để Excel sử dụng
     */
    private Sheet createVietnameseCharsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Maps");
        // Ẩn sheet này khỏi người dùng
        workbook.setSheetHidden(workbook.getSheetIndex("Maps"), true);

        sheet.setColumnWidth(0, 2000); // Ký tự có dấu
        sheet.setColumnWidth(1, 2000); // Ký tự không dấu

        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Vietnamese");

        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("ASCII");

        int rowNum = 1;
        for (Map.Entry<String, String> entry : VIETNAMESE_CHARS_MAPPING.entrySet()) {
            Row row = sheet.createRow(rowNum++);

            Cell charCell = row.createCell(0);
            charCell.setCellValue(entry.getKey());

            Cell asciiCell = row.createCell(1);
            asciiCell.setCellValue(entry.getValue());
        }

        // Tạo tên vùng dữ liệu để sử dụng trong các công thức
        Name vietnameseRange = workbook.createName();
        vietnameseRange.setNameName("Vietnamese");
        vietnameseRange.setRefersToFormula("Maps!$A$2:$A$" + rowNum);

        Name asciiRange = workbook.createName();
        asciiRange.setNameName("ASCII");
        asciiRange.setRefersToFormula("Maps!$B$2:$B$" + rowNum);

        return sheet;
    }

    /**
     * Tạo sheet danh sách SKU tự động sinh - cập nhật để loại bỏ dấu tiếng Việt
     */
    private Sheet createSkuListSheet(Workbook workbook, Sheet productSheet, Sheet variantSheet) {
        Sheet sheet = workbook.createSheet("Danh Sách SKU");

        // Thiết lập độ rộng cột
        sheet.setColumnWidth(0, 2000); // STT
        sheet.setColumnWidth(1, 6000); // Tên Sản Phẩm
        sheet.setColumnWidth(2, 3000); // Kích Thước
        sheet.setColumnWidth(3, 3000); // Màu Sắc
        sheet.setColumnWidth(4, 5000); // SKU
        sheet.setColumnWidth(5, 8000); // Thư Mục Ảnh

        // Tạo style cho tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo style cho nội dung
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Tạo tiêu đề sheet
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < SKU_LIST_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(SKU_LIST_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Tối đa 100 biến thể
        for (int i = 1; i <= 100; i++) {
            Row row = sheet.createRow(i);

            // STT
            Cell sttCell = row.createCell(0);
            sttCell.setCellValue(i);
            sttCell.setCellStyle(dataStyle);

            // Tên Sản Phẩm: lấy từ sheet sản phẩm
            Cell nameCell = row.createCell(1);
            String productNameFormula = "IF(AND('Biến Thể'!A" + (i+1) + "<>\"\", 'Biến Thể'!B" + (i+1) + "<>\"\", 'Biến Thể'!C" + (i+1) + "<>\"\"), INDEX('Sản Phẩm'!A:A, 'Biến Thể'!A" + (i+1) + "+1), \"\")";
            nameCell.setCellFormula(productNameFormula);
            nameCell.setCellStyle(dataStyle);

            // Kích Thước: lấy trực tiếp từ sheet biến thể
            Cell sizeCell = row.createCell(2);
            String sizeFormula = "IF(AND('Biến Thể'!A" + (i+1) + "<>\"\", 'Biến Thể'!B" + (i+1) + "<>\"\", 'Biến Thể'!C" + (i+1) + "<>\"\"), 'Biến Thể'!B" + (i+1) + ", \"\")";
            sizeCell.setCellFormula(sizeFormula);
            sizeCell.setCellStyle(dataStyle);

            // Màu Sắc: lấy trực tiếp từ sheet biến thể
            Cell colorCell = row.createCell(3);
            String colorFormula = "IF(AND('Biến Thể'!A" + (i+1) + "<>\"\", 'Biến Thể'!B" + (i+1) + "<>\"\", 'Biến Thể'!C" + (i+1) + "<>\"\"), 'Biến Thể'!C" + (i+1) + ", \"\")";
            colorCell.setCellFormula(colorFormula);
            colorCell.setCellStyle(dataStyle);

            // SKU: tạo tự động từ tên sản phẩm, kích thước và màu sắc - đồng bộ với backend
            Cell skuCell = row.createCell(4);

            // Xử lý tên sản phẩm (bỏ dấu và bỏ khoảng trắng, lấy 3 ký tự đầu)
            String productNameWithoutDiacritics =
                    "UPPER(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(" +
                            "SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(" +
                            "SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(" +
                            "INDEX('Sản Phẩm'!A:A,'Biến Thể'!A" + (i+1) + "+1,1)," +
                            "\"Á\", \"A\"), \"À\", \"A\"), \"Ả\", \"A\"), \"Ã\", \"A\"), " +
                            "\"Ạ\", \"A\"), \"Â\", \"A\"), \"Ă\", \"A\"), \"Đ\", \"D\"), " +
                            "\"É\", \"E\"), \"È\", \"E\"), \"Ê\", \"E\"), \"Ô\", \"O\"))";

            // Tạo công thức SKU đồng bộ với backend
            String skuFormula = "IF(AND('Biến Thể'!A" + (i+1) + "<>\"\", 'Biến Thể'!B" + (i+1) + "<>\"\", 'Biến Thể'!C" + (i+1) + "<>\"\"),"
                    + "CONCATENATE("
                    // Lấy 3 ký tự đầu của tên sản phẩm đã bỏ dấu và khoảng trắng
                    + "LEFT(SUBSTITUTE(" + productNameWithoutDiacritics + ",\" \",\"\"),3),"
                    + "\"001-\","
                    // Size giữ nguyên
                    + "'Biến Thể'!B" + (i+1) + ","
                    + "\"-\","
                    // Xử lý màu sắc đặc biệt với #
                    + "IF(LEFT('Biến Thể'!C" + (i+1) + ",1)=\"#\", "
                    // Nếu là mã màu # thì lấy 4 ký tự đầu (đồng bộ với backend)
                    + "LEFT('Biến Thể'!C" + (i+1) + ",4), "
                    // Nếu không, lấy 3 ký tự đầu sau khi bỏ dấu và khoảng trắng
                    + "LEFT(SUBSTITUTE(UPPER(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE('Biến Thể'!C" + (i+1)
                    + ",\"Đ\",\"D\"),\"Ô\",\"O\"),\"Ă\",\"A\")),\" \",\"\"),3)"
                    + ")"
                    + "),\"\")";

            skuCell.setCellFormula(skuFormula);
            skuCell.setCellStyle(dataStyle);

            // Thư Mục Ảnh: tên thư mục đầy đủ cần tạo
            Cell folderCell = row.createCell(5);
            String folderFormula = "IF(E" + (i+1) + "<>\"\", CONCATENATE(\"Images/\", E" + (i+1) + ", \"/\"), \"\")";
            folderCell.setCellFormula(folderFormula);
            folderCell.setCellStyle(dataStyle);
        }

        // Thêm ghi chú ở cuối sheet
        Row noteRow1 = sheet.createRow(102);
        Cell noteCell1 = noteRow1.createCell(0);
        noteCell1.setCellValue("Lưu ý: Bảng này sẽ tự động cập nhật khi bạn nhập dữ liệu vào sheet Sản Phẩm và Biến Thể.");

        Row noteRow2 = sheet.createRow(103);
        Cell noteCell2 = noteRow2.createCell(0);
        noteCell2.setCellValue("Cách tạo SKU: 3 ký tự đầu tên sản phẩm (bỏ dấu và bỏ khoảng trắng) + 001 + size + mã màu.");

        Row noteRow3 = sheet.createRow(104);
        Cell noteCell3 = noteRow3.createCell(0);
        noteCell3.setCellValue("Ví dụ: \"Áo Thun\" -> \"AOT001-S-DEN\" (không phải ATN001)");

        Row noteRow4 = sheet.createRow(105);
        Cell noteCell4 = noteRow4.createCell(0);
        noteCell4.setCellValue("Đối với mã màu hex (bắt đầu bằng #), SKU sẽ lấy 4 ký tự đầu (ví dụ: AOT001-S-#FFF).");

        return sheet;
    }

    /**
     * Tạo sheet mới hướng dẫn cấu trúc thư mục ảnh
     */
    private void createFolderStructureSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Cấu Trúc Thư Mục Ảnh");

        // Thiết lập độ rộng cột
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 8000);

        // Tạo style cho tiêu đề
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // Tạo style cho tiêu đề phụ
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

        // Tạo style cho nội dung
        CellStyle contentStyle = workbook.createCellStyle();
        contentStyle.setWrapText(true);
        contentStyle.setBorderBottom(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);

        // Tạo tiêu đề sheet
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HƯỚNG DẪN CẤU TRÚC THƯ MỤC ẢNH");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        // Thêm hướng dẫn chung
        Row introRow = sheet.createRow(2);
        Cell introCell = introRow.createCell(0);
        introCell.setCellValue("Hướng dẫn tổ chức thư mục ảnh cho nhập sản phẩm:");
        introCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 2));

        Row infoRow = sheet.createRow(3);
        Cell infoCell = infoRow.createCell(0);
        infoCell.setCellValue("Ảnh sản phẩm phải được tổ chức theo cấu trúc thư mục sau để hệ thống nhập sản phẩm có thể xử lý đúng cách. " +
                "Mỗi biến thể sản phẩm (mỗi SKU) cần một thư mục riêng trong thư mục Images/. Xem sheet 'Danh Sách SKU' để lấy tên thư mục chính xác. " +
                "Lưu ý rằng SKU được tạo sẽ loại bỏ dấu tiếng Việt.");
        infoCell.setCellStyle(contentStyle);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 2));

        // Tạo tiêu đề bảng
        Row headerRow = sheet.createRow(5);

        Cell folderHeaderCell = headerRow.createCell(0);
        folderHeaderCell.setCellValue("Cấu Trúc Thư Mục");
        folderHeaderCell.setCellStyle(subtitleStyle);

        Cell descriptionHeaderCell = headerRow.createCell(1);
        descriptionHeaderCell.setCellValue("Mô Tả");
        descriptionHeaderCell.setCellStyle(subtitleStyle);

        Cell exampleHeaderCell = headerRow.createCell(2);
        exampleHeaderCell.setCellValue("Ví Dụ");
        exampleHeaderCell.setCellStyle(subtitleStyle);

        // Thêm nội dung bảng
        String[][] folderStructureData = {
                {"Images/", "Thư mục gốc chứa tất cả ảnh sản phẩm", "Images/"},
                {"Images/thumbnails/", "Thư mục chứa ảnh thumbnail cho sản phẩm", "Images/thumbnails/thumbnail1.jpg"},
                {"Images/[SKU]/", "Thư mục con cho mỗi biến thể sản phẩm, đặt tên theo SKU tự động", "Images/ATN001-S-DEN/"},
                {"Images/[SKU]/main.jpg", "Ảnh chính của biến thể, hiển thị trong trang sản phẩm", "Images/ATN001-S-DEN/main.jpg"},
                {"Images/[SKU]/1.jpg, 2.jpg, ...", "Ảnh phụ của biến thể, đánh số từ 1 trở đi", "Images/ATN001-S-DEN/1.jpg"},
        };

        for (int i = 0; i < folderStructureData.length; i++) {
            Row row = sheet.createRow(i + 6);

            Cell structureCell = row.createCell(0);
            structureCell.setCellValue(folderStructureData[i][0]);
            structureCell.setCellStyle(contentStyle);

            Cell descCell = row.createCell(1);
            descCell.setCellValue(folderStructureData[i][1]);
            descCell.setCellStyle(contentStyle);

            Cell exampleCell = row.createCell(2);
            exampleCell.setCellValue(folderStructureData[i][2]);
            exampleCell.setCellStyle(contentStyle);
        }

        // Thêm lưu ý quan trọng
        Row notesHeaderRow = sheet.createRow(12);
        Cell notesHeaderCell = notesHeaderRow.createCell(0);
        notesHeaderCell.setCellValue("LƯU Ý QUAN TRỌNG");
        notesHeaderCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(12, 12, 0, 2));

        String[] notes = {
                "1. Tên thư mục SKU phải chính xác và khớp với SKU trong sheet 'Danh Sách SKU'",
                "2. SKU được tự động tạo sẽ KHÔNG CÓ DẤU TIẾNG VIỆT. Ví dụ: 'Áo Thun Nam' → 'ATN'",
                "3. Với màu sắc tiếng Việt: 'Đỏ' → 'DO', 'Xanh Dương' → 'XDG'",
                "4. File main.jpg là bắt buộc cho mỗi biến thể",
                "5. Hệ thống hỗ trợ định dạng ảnh JPG, PNG, WebP",
                "6. Kích thước ảnh tối đa là 5MB mỗi ảnh",
                "7. SKU được tự động tạo dựa trên thông tin sản phẩm, kích thước và màu sắc",
                "8. Không sử dụng ký tự đặc biệt trong tên file ảnh",
                "9. Thư mục Images phải nằm cùng cấp với file Excel khi nén thành ZIP",
                "10. Đặt ảnh thumbnail vào thư mục Images/thumbnails/ và nhập tên file ở cột 'Ảnh Thumbnail' trong sheet Sản Phẩm"
        };

        for (int i = 0; i < notes.length; i++) {
            Row row = sheet.createRow(13 + i);
            Cell cell = row.createCell(0);
            cell.setCellValue(notes[i]);
            cell.setCellStyle(contentStyle);
            sheet.addMergedRegion(new CellRangeAddress(13 + i, 13 + i, 0, 2));
        }

        // Thêm ví dụ cấu trúc thư mục
        Row exampleHeaderRow = sheet.createRow(24);
        Cell exampleHeader = exampleHeaderRow.createCell(0);
        exampleHeader.setCellValue("VÍ DỤ CẤU TRÚC THƯ MỤC ĐẦY ĐỦ");
        exampleHeader.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(24, 24, 0, 2));

        Row exampleRow = sheet.createRow(25);
        Cell exampleCell = exampleRow.createCell(0);
        exampleCell.setCellValue(
                "Images/\n" +
                        "  ├── thumbnails/\n" +
                        "  │    ├── thumbnail1.jpg\n" +
                        "  │    └── thumbnail2.jpg\n" +
                        "  ├── ATN001-S-DEN/\n" +
                        "  │    ├── main.jpg\n" +
                        "  │    ├── 1.jpg\n" +
                        "  │    └── 2.jpg\n" +
                        "  ├── ATN001-M-TRG/\n" +
                        "  │    ├── main.jpg\n" +
                        "  │    └── 1.jpg\n" +
                        "  └── ATN002-L-XDG/\n" +
                        "       ├── main.jpg\n" +
                        "       ├── 1.jpg\n" +
                        "       └── 2.jpg"
        );
        exampleCell.setCellStyle(contentStyle);
        sheet.addMergedRegion(new CellRangeAddress(25, 25, 0, 2));

        // Thêm bảng chuyển đổi ví dụ
        Row conversionRow = sheet.createRow(27);
        Cell conversionTitle = conversionRow.createCell(0);
        conversionTitle.setCellValue("VÍ DỤ CHUYỂN ĐỔI TỪ TÊN CÓ DẤU THÀNH SKU KHÔNG DẤU");
        conversionTitle.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(27, 27, 0, 2));

        // Tạo tiêu đề cho bảng chuyển đổi
        Row convHeaderRow = sheet.createRow(28);
        Cell convHeader1 = convHeaderRow.createCell(0);
        convHeader1.setCellValue("Tên Sản Phẩm");
        convHeader1.setCellStyle(subtitleStyle);

        Cell convHeader2 = convHeaderRow.createCell(1);
        convHeader2.setCellValue("Màu Sắc & Kích Thước");
        convHeader2.setCellStyle(subtitleStyle);

        Cell convHeader3 = convHeaderRow.createCell(2);
        convHeader3.setCellValue("SKU Kết Quả");
        convHeader3.setCellStyle(subtitleStyle);

        // Thêm dữ liệu ví dụ chuyển đổi
        String[][] conversionExamples = {
                {"Áo Thun Nam", "Size S, Đen", "ATN001-S-DEN"},
                {"Áo Sơ Mi Đẹp", "Size M, Đỏ", "ASD001-M-DO"},
                {"Quần Dài Kaki", "Size L, Xanh Dương", "QDK001-L-XDG"},
                {"Đầm Xòe Nữ", "Size XL, Hồng", "DXN001-XL-HON"},
        };

        for (int i = 0; i < conversionExamples.length; i++) {
            Row row = sheet.createRow(29 + i);

            Cell cell1 = row.createCell(0);
            cell1.setCellValue(conversionExamples[i][0]);
            cell1.setCellStyle(contentStyle);

            Cell cell2 = row.createCell(1);
            cell2.setCellValue(conversionExamples[i][1]);
            cell2.setCellStyle(contentStyle);

            Cell cell3 = row.createCell(2);
            cell3.setCellValue(conversionExamples[i][2]);
            cell3.setCellStyle(contentStyle);
        }
    }

    /**
     * Tạo sheet sản phẩm với dropdown bắt buộc cho danh mục và thương hiệu
     * Thêm cột để nhập ảnh thumbnail
     */
    private Sheet createProductSheet(Workbook workbook, Sheet categoriesSheet, Sheet brandsSheet) {
        Sheet sheet = workbook.createSheet("Sản Phẩm");

        // Thiết lập độ rộng cột
        for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }
        // Đặt độ rộng lớn hơn cho cột mô tả và cột ảnh thumbnail
        sheet.setColumnWidth(3, 6000); // Mô tả
        sheet.setColumnWidth(7, 6000); // Ảnh Thumbnail

        // Tạo style cho tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Tạo row tiêu đề
        Row headerRow = sheet.createRow(0);
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

        // Style cho các ô dropdown
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
        createCategoryDropdown(sheet, categoriesSheet);
        createBrandDropdown(sheet, brandsSheet);

        // Thêm dữ liệu mẫu
        for (int i = 1; i <= 3; i++) {
            Row row = sheet.createRow(i);

            // Tên sản phẩm
            Cell cell0 = row.createCell(0);
            cell0.setCellValue("Áo thun nam ABC " + i);
            cell0.setCellStyle(dataStyle);

            // Loại sản phẩm - để trống để người dùng chọn từ dropdown
            Cell cell1 = row.createCell(1);
            cell1.setCellStyle(dropdownStyle);

            // Thương hiệu - để trống để người dùng chọn từ dropdown
            Cell cell2 = row.createCell(2);
            cell2.setCellStyle(dropdownStyle);

            // Mô tả
            Cell cell3 = row.createCell(3);
            cell3.setCellValue("Áo thun nam chất lượng cao");
            cell3.setCellStyle(dataStyle);

            // Giá gốc
            Cell cell4 = row.createCell(4);
            cell4.setCellValue(350000);
            cell4.setCellStyle(dataStyle);

            // Giá khuyến mãi
            Cell cell5 = row.createCell(5);
            cell5.setCellStyle(dataStyle);

            // Slug (tự động tạo từ tên sản phẩm)
            Cell cell6 = row.createCell(6);
            String slugFormula = "IF(A" + (i+1) + "<>\"\", " +
                    "LOWER(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(" +
                    "SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(" +
                    "SUBSTITUTE(SUBSTITUTE(SUBSTITUTE(A" + (i+1) + ", \" \", \"-\"), \"Á\", \"a\"), \"À\", \"a\"), " +
                    "\"Ả\", \"a\"), \"Ã\", \"a\"), \"Ạ\", \"a\"), \"Ă\", \"a\"), \"Â\", \"a\"), \"Đ\", \"d\"), " +
                    "\"É\", \"e\"), \"È\", \"e\"), \"Ẻ\", \"e\"), \"Ẽ\", \"e\"), \"Ẹ\", \"e\"), \"Ê\", \"e\"), " +
                    "\"Ô\", \"o\"), \"Ơ\", \"o\"))" +
                    ", \"\")";
            cell6.setCellFormula(slugFormula);
            cell6.setCellStyle(slugStyle);

            // Ảnh Thumbnail
            Cell cell7 = row.createCell(7);
            cell7.setCellValue("thumbnail" + i + ".jpg");
            cell7.setCellStyle(thumbnailStyle);
        }

        // Thêm ghi chú hướng dẫn
        Row noteCategoryRow = sheet.createRow(5);
        Cell noteCategoryCell = noteCategoryRow.createCell(0);
        noteCategoryCell.setCellValue("LƯU Ý: Hãy chọn Loại Sản Phẩm và Thương Hiệu từ danh sách dropdown (những ô có nền màu vàng)");
        CellStyle noteStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        noteStyle.setFont(boldFont);
        noteCategoryCell.setCellStyle(noteStyle);
        sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, 7));

        Row noteThumbnailRow = sheet.createRow(6);
        Cell noteThumbnailCell = noteThumbnailRow.createCell(0);
        noteThumbnailCell.setCellValue("LƯU Ý: Ảnh thumbnail là ảnh chính của sản phẩm, nhập tên file ảnh vào ô màu xanh lá (đặt file trong thư mục Images/thumbnails/)");
        noteThumbnailCell.setCellStyle(noteStyle);
        sheet.addMergedRegion(new CellRangeAddress(6, 6, 0, 7));

        Row noteSlugRow = sheet.createRow(7);
        Cell noteSlugCell = noteSlugRow.createCell(0);
        noteSlugCell.setCellValue("LƯU Ý: Cột Slug sẽ tự động tạo từ tên sản phẩm. Bạn có thể sửa nếu muốn.");
        noteSlugCell.setCellStyle(noteStyle);
        sheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 7));

        return sheet;
    }

    /**
     * Tạo dropdown rõ ràng cho cột Loại Sản Phẩm
     */
    private void createCategoryDropdown(Sheet productSheet, Sheet categoriesSheet) {
        // Chuẩn bị danh sách các giá trị hiển thị trong dropdown
        List<String> categoryNames = new ArrayList<>();

        // Lấy tất cả tên danh mục từ sheet danh mục
        for (int i = 1; i <= categoriesSheet.getLastRowNum(); i++) {
            Row row = categoriesSheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(1); // Cột B - Tên danh mục
                if (cell != null) {
                    String categoryName = cell.getStringCellValue();
                    if (categoryName != null && !categoryName.isEmpty()) {
                        categoryNames.add(categoryName);
                    }
                }
            }
        }

        // Tạo một mảng từ danh sách tên danh mục
        String[] categories = categoryNames.toArray(new String[0]);

        // Sử dụng constraint kiểu explicit list thay vì formula
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) productSheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                dvHelper.createExplicitListConstraint(categories);

        // Áp dụng validation cho tất cả các hàng trong cột 1 (B)
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 1, 1);
        XSSFDataValidation validation = (XSSFDataValidation)
                dvHelper.createValidation(dvConstraint, addressList);

        // Thiết lập hiển thị rõ ràng
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Loại Sản Phẩm");
        validation.setSuppressDropDownArrow(false);

        productSheet.addValidationData(validation);
    }

    /**
     * Tạo dropdown rõ ràng cho cột Thương Hiệu
     */
    private void createBrandDropdown(Sheet productSheet, Sheet brandsSheet) {
        // Chuẩn bị danh sách các giá trị hiển thị trong dropdown
        List<String> brandNames = new ArrayList<>();

        // Lấy tất cả tên thương hiệu từ sheet thương hiệu
        for (int i = 1; i <= brandsSheet.getLastRowNum(); i++) {
            Row row = brandsSheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(1); // Cột B - Tên thương hiệu
                if (cell != null) {
                    String brandName = cell.getStringCellValue();
                    if (brandName != null && !brandName.isEmpty()) {
                        brandNames.add(brandName);
                    }
                }
            }
        }

        // Tạo một mảng từ danh sách tên thương hiệu
        String[] brands = brandNames.toArray(new String[0]);

        // Sử dụng constraint kiểu explicit list thay vì formula
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) productSheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                dvHelper.createExplicitListConstraint(brands);

        // Áp dụng validation cho tất cả các hàng trong cột 2 (C)
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 2, 2);
        XSSFDataValidation validation = (XSSFDataValidation)
                dvHelper.createValidation(dvConstraint, addressList);

        // Thiết lập hiển thị rõ ràng
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Thương Hiệu");
        validation.setSuppressDropDownArrow(false);

        productSheet.addValidationData(validation);
    }

    /**
     * Tạo sheet biến thể với dropdown cho kích thước và màu sắc
     */
    private Sheet createVariantSheet(Workbook workbook, Sheet productSheet, Sheet sizesSheet, Sheet colorsSheet) {
        Sheet sheet = workbook.createSheet("Biến Thể");

        for (int i = 0; i < VARIANT_HEADERS.length; i++) {
            sheet.setColumnWidth(i, 4000);
        }

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < VARIANT_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(VARIANT_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Style cho dropdown
        CellStyle dropdownStyle = workbook.createCellStyle();
        dropdownStyle.cloneStyleFrom(dataStyle);
        dropdownStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        dropdownStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Thêm dropdown cho kích thước và màu sắc
        createSizeDropdown(sheet, sizesSheet);
        createColorDropdown(sheet, colorsSheet);

        // Thêm dữ liệu mẫu
        int rowNum = 1;
        for (int p = 1; p <= 3; p++) {
            for (int s = 0; s < 2; s++) {  // Thêm 2 kích thước mỗi sản phẩm
                for (int c = 0; c < 2; c++) {  // Thêm 2 màu mỗi kích thước
                    Row row = sheet.createRow(rowNum);

                    // STT sản phẩm
                    Cell cell0 = row.createCell(0);
                    cell0.setCellValue(p);
                    cell0.setCellStyle(dataStyle);

                    // Kích thước - để trống để chọn từ dropdown
                    Cell cell1 = row.createCell(1);
                    cell1.setCellStyle(dropdownStyle);

                    // Màu sắc - để trống để chọn từ dropdown
                    Cell cell2 = row.createCell(2);
                    cell2.setCellStyle(dropdownStyle);

                    // Số lượng tồn kho
                    Cell cell3 = row.createCell(3);
                    cell3.setCellValue(100);
                    cell3.setCellStyle(dataStyle);

                    rowNum++;
                }
            }
        }

        // Thêm ghi chú hướng dẫn
        Row noteRow = sheet.createRow(rowNum + 1);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("LƯU Ý: Hãy chọn Kích Thước và Màu Sắc từ danh sách dropdown (những ô có nền màu vàng)");
        CellStyle noteStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        noteStyle.setFont(boldFont);
        noteCell.setCellStyle(noteStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowNum + 1, rowNum + 1, 0, 3));

        return sheet;
    }

    /**
     * Tạo dropdown cho cột Kích Thước
     */
    private void createSizeDropdown(Sheet variantSheet, Sheet sizesSheet) {
        // Chuẩn bị danh sách các giá trị hiển thị trong dropdown
        List<String> sizeNames = new ArrayList<>();

        // Lấy tất cả kích thước từ sheet kích thước
        for (int i = 1; i <= sizesSheet.getLastRowNum(); i++) {
            Row row = sizesSheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(0); // Cột A - Tên kích thước
                if (cell != null) {
                    String sizeName = cell.getStringCellValue();
                    if (sizeName != null && !sizeName.isEmpty()) {
                        sizeNames.add(sizeName);
                    }
                }
            }
        }

        // Tạo một mảng từ danh sách kích thước
        String[] sizes = sizeNames.toArray(new String[0]);

        // Sử dụng constraint kiểu explicit list
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) variantSheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                dvHelper.createExplicitListConstraint(sizes);

        // Áp dụng validation cho tất cả các hàng trong cột 1 (B)
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 1, 1);
        XSSFDataValidation validation = (XSSFDataValidation)
                dvHelper.createValidation(dvConstraint, addressList);

        // Thiết lập hiển thị rõ ràng
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Kích Thước");
        validation.setSuppressDropDownArrow(false);

        variantSheet.addValidationData(validation);
    }

    /**
     * Tạo dropdown cho cột Màu Sắc
     */
    private void createColorDropdown(Sheet variantSheet, Sheet colorsSheet) {
        // Chuẩn bị danh sách các giá trị hiển thị trong dropdown
        List<String> colorNames = new ArrayList<>();

        // Lấy tất cả màu sắc từ sheet màu sắc
        for (int i = 1; i <= colorsSheet.getLastRowNum(); i++) {
            Row row = colorsSheet.getRow(i);
            if (row != null) {
                Cell cell = row.getCell(0); // Cột A - Tên màu
                if (cell != null) {
                    String colorName = cell.getStringCellValue();
                    if (colorName != null && !colorName.isEmpty()) {
                        colorNames.add(colorName);
                    }
                }
            }
        }

        // Tạo một mảng từ danh sách màu sắc
        String[] colors = colorNames.toArray(new String[0]);

        // Sử dụng constraint kiểu explicit list
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) variantSheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                dvHelper.createExplicitListConstraint(colors);

        // Áp dụng validation cho tất cả các hàng trong cột 2 (C)
        CellRangeAddressList addressList = new CellRangeAddressList(1, 1000, 2, 2);
        XSSFDataValidation validation = (XSSFDataValidation)
                dvHelper.createValidation(dvConstraint, addressList);

        // Thiết lập hiển thị rõ ràng
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Lỗi dữ liệu", "Vui lòng chọn từ danh sách Màu Sắc");
        validation.setSuppressDropDownArrow(false);

        variantSheet.addValidationData(validation);
    }

    /**
     * Tạo sheet danh sách kích thước
     */
    private Sheet createSizesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Kích Thước");
        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 6000);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("Kích Thước");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Mô Tả");
        cell1.setCellStyle(headerStyle);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < SIZE_OPTIONS.length; i++) {
            Row row = sheet.createRow(i + 1);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(SIZE_OPTIONS[i]);
            nameCell.setCellStyle(dataStyle);

            Cell descCell = row.createCell(1);
            descCell.setCellValue(getDescriptionForSize(SIZE_OPTIONS[i]));
            descCell.setCellStyle(dataStyle);
        }

        return sheet;
    }

    /**
     * Tạo sheet danh sách màu sắc
     */
    private Sheet createColorsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Màu Sắc");
        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 3000);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        Cell cell0 = headerRow.createCell(0);
        cell0.setCellValue("Màu Sắc");
        cell0.setCellStyle(headerStyle);

        Cell cell1 = headerRow.createCell(1);
        cell1.setCellValue("Mã Màu (Tham Khảo)");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = headerRow.createCell(2);
        cell2.setCellValue("Mã SKU Không Dấu");
        cell2.setCellStyle(headerStyle);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            Row row = sheet.createRow(i + 1);

            Cell nameCell = row.createCell(0);
            nameCell.setCellValue(COLOR_OPTIONS[i]);
            nameCell.setCellStyle(dataStyle);

            Cell descCell = row.createCell(1);
            descCell.setCellValue(getColorCode(COLOR_OPTIONS[i]));
            descCell.setCellStyle(dataStyle);

            Cell skuCell = row.createCell(2);
            skuCell.setCellValue(getColorSkuCode(COLOR_OPTIONS[i]));
            skuCell.setCellStyle(dataStyle);
        }

        return sheet;
    }

    /**
     * Lấy mô tả cho kích thước
     */
    private String getDescriptionForSize(String size) {
        switch (size) {
            case "S":
                return "Small (Nhỏ)";
            case "M":
                return "Medium (Vừa)";
            case "L":
                return "Large (Lớn)";
            case "XL":
                return "Extra Large (Rất lớn)";
            case "XXL":
                return "Double Extra Large (Cực lớn)";
            case "XXXL":
                return "Triple Extra Large (Siêu lớn)";
            default:
                if (size.matches("\\d+")) {
                    return "Size " + size;
                }
                return size;
        }
    }

    /**
     * Lấy mã màu tham khảo
     */
    private String getColorCode(String color) {
        switch (color) {
            case "Đen":
                return "#000000";
            case "Trắng":
                return "#FFFFFF";
            case "Đỏ":
                return "#FF0000";
            case "Xanh lá":
                return "#00FF00";
            case "Xanh dương":
                return "#0000FF";
            case "Vàng":
                return "#FFFF00";
            case "Cam":
                return "#FFA500";
            case "Hồng":
                return "#FFC0CB";
            case "Tím":
                return "#800080";
            case "Nâu":
                return "#A52A2A";
            case "Xám":
                return "#808080";
            case "Be":
                return "#F5F5DC";
            case "Xanh ngọc":
                return "#40E0D0";
            case "Đỏ đô":
                return "#800000";
            default:
                return "#000000";
        }
    }

    /**
     * Lấy mã màu không dấu cho SKU
     */
    private String getColorSkuCode(String color) {
        switch (color) {
            case "Đen":
                return "DEN";
            case "Trắng":
                return "TRG";
            case "Đỏ":
                return "DO";
            case "Xanh lá":
                return "XLA";
            case "Xanh dương":
                return "XDG";
            case "Vàng":
                return "VAN";
            case "Cam":
                return "CAM";
            case "Hồng":
                return "HON";
            case "Tím":
                return "TIM";
            case "Nâu":
                return "NAU";
            case "Xám":
                return "XAM";
            case "Be":
                return "BE";
            case "Xanh ngọc":
                return "XNG";
            case "Đỏ đô":
                return "DDO";
            default:
                return color.substring(0, Math.min(color.length(), 3)).toUpperCase();
        }
    }

    /**
     * Tạo sheet danh sách loại sản phẩm
     */
    private Sheet createCategoriesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Loại SP");

        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

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

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

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
                    descCell.setCellValue(category.getDescription() != null ? category.getDescription() : "");
                    descCell.setCellStyle(dataStyle);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách loại sản phẩm: ", e);
            createDummyCategoryData(sheet, dataStyle);
        }

        return sheet;
    }

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

    private Sheet createBrandsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Thương Hiệu");

        sheet.setColumnWidth(0, 2000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

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

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

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
                    descCell.setCellValue(brand.getDescription() != null ? brand.getDescription() : "");
                    descCell.setCellStyle(dataStyle);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thương hiệu: ", e);
            createDummyBrandData(sheet, dataStyle);
        }

        return sheet;
    }

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

        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("HƯỚNG DẪN NHẬP SẢN PHẨM");
        titleCell.setCellStyle(titleStyle);

        String[] instructions = {
                "1. File này chứa 10 sheet: Sản Phẩm, Biến Thể, Danh Sách SKU, Danh Sách Loại SP, Danh Sách Thương Hiệu, Danh Sách Kích Thước, Danh Sách Màu Sắc, Cấu Trúc Thư Mục Ảnh, Hướng Dẫn và một sheet ẩn để xử lý tiếng Việt.",
                "2. Nhập thông tin sản phẩm vào sheet 'Sản Phẩm' trước. Các trường đánh dấu (*) là bắt buộc.",
                "3. Sử dụng dropdown để chọn Loại Sản Phẩm và Thương Hiệu từ danh sách có sẵn (các ô có nền màu vàng).",
                "4. Nhập tên file ảnh thumbnail cho sản phẩm vào cột 'Ảnh Thumbnail' (ô có nền màu xanh lá). File ảnh phải được đặt trong thư mục Images/thumbnails/.",
                "5. Trường Slug sẽ tự động tạo từ tên sản phẩm, bạn có thể để trống hoặc tùy chỉnh nếu muốn.",
                "6. Nhập biến thể sản phẩm vào sheet 'Biến Thể'. Sử dụng STT sản phẩm để liên kết với sản phẩm trong sheet 'Sản Phẩm'.",
                "7. Sử dụng dropdown để chọn Kích Thước và Màu Sắc từ danh sách có sẵn (các ô có nền màu vàng).",
                "8. LƯU Ý QUAN TRỌNG: SKU sẽ được tự động tạo và hiển thị trong sheet 'Danh Sách SKU'. SKU này sẽ KHÔNG CÓ DẤU TIẾNG VIỆT.",
                "9. Tạo thư mục ảnh cho mỗi biến thể với tên chính xác như trong cột 'Thư Mục Ảnh' của sheet 'Danh Sách SKU'.",
                "10. Xem sheet 'Cấu Trúc Thư Mục Ảnh' để hiểu cách tổ chức thư mục ảnh cho các biến thể sản phẩm.",
                "11. Mỗi thư mục biến thể cần có file main.jpg (ảnh chính) và có thể có các ảnh phụ đánh số từ 1.jpg, 2.jpg,...",
                "12. Nén thư mục Images và file Excel thành file ZIP trước khi tải lên hệ thống."
        };

        for (int i = 0; i < instructions.length; i++) {
            Row row = sheet.createRow(i + 2);
            Cell cell = row.createCell(0);
            cell.setCellValue(instructions[i]);
            cell.setCellStyle(contentStyle);
        }

        // Thêm mục giải thích chuyển đổi tiếng Việt
        Row headerRow = sheet.createRow(instructions.length + 3);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("CHUYỂN ĐỔI TIẾNG VIỆT CÓ DẤU THÀNH KHÔNG DẤU");
        headerCell.setCellStyle(subtitleStyle);

        Row infoRow = sheet.createRow(instructions.length + 4);
        Cell infoCell = infoRow.createCell(0);
        infoCell.setCellValue("Hệ thống sẽ tự động bỏ dấu tiếng Việt khi tạo SKU và URL Slug. Dưới đây là một số ví dụ chuyển đổi:");
        infoCell.setCellStyle(contentStyle);

        String[][] conversionExamples = {
                {"Tên sản phẩm có dấu", "-> SKU không dấu / Slug không dấu"},
                {"Áo Thun Nam", "-> ATN001 / ao-thun-nam"},
                {"Đầm Công Sở", "-> DCS001 / dam-cong-so"},
                {"Giày Thể Thao", "-> GTT001 / giay-the-thao"},
                {"Màu sắc có dấu", "-> Mã màu không dấu"},
                {"Đỏ", "-> DO"},
                {"Xanh Dương", "-> XDG"},
                {"Tím", "-> TIM"},
        };

        for (int i = 0; i < conversionExamples.length; i++) {
            Row row = sheet.createRow(instructions.length + 5 + i);

            Cell cell1 = row.createCell(0);
            cell1.setCellValue(conversionExamples[i][0]);
            cell1.setCellStyle(contentStyle);

            CellStyle rightAlignStyle = workbook.createCellStyle();
            rightAlignStyle.cloneStyleFrom(contentStyle);
            rightAlignStyle.setAlignment(HorizontalAlignment.RIGHT);

            Cell cell2 = row.createCell(1);
            cell2.setCellValue(conversionExamples[i][1]);
            cell2.setCellStyle(rightAlignStyle);
        }
    }

    /**
     * Tạo hướng dẫn
     */
    public String createInstructionText() {
        StringBuilder content = new StringBuilder();
        content.append("HƯỚNG DẪN NHẬP SẢN PHẨM BẰNG FILE EXCEL\n");
        content.append("=========================================\n\n");
        content.append("I. QUY TRÌNH NHẬP SẢN PHẨM\n");
        content.append("1. Điền thông tin sản phẩm vào sheet 'Sản Phẩm' trước\n");
        content.append("2. Chọn Loại Sản Phẩm và Thương Hiệu từ dropdown đã được thiết lập (các ô màu vàng)\n");
        content.append("3. Nhập tên file ảnh thumbnail vào cột Ảnh Thumbnail (ô màu xanh lá)\n");
        content.append("4. Điền thông tin biến thể vào sheet 'Biến Thể', sử dụng STT sản phẩm để liên kết\n");
        content.append("5. Chọn Kích Thước và Màu Sắc từ dropdown (các ô màu vàng)\n");
        content.append("6. Xem danh sách SKU được tạo tự động trong sheet 'Danh Sách SKU'\n");
        content.append("7. LƯU Ý: SKU sẽ được tạo KHÔNG CÓ DẤU TIẾNG VIỆT (Áo Thun Nam -> ATN001)\n");
        content.append("8. URL Slug cũng được tạo tự động từ tên sản phẩm (Áo Thun Nam -> ao-thun-nam)\n");
        content.append("9. Chuẩn bị ảnh thumbnail và ảnh biến thể theo cấu trúc thư mục\n");
        content.append("10. Nén thư mục Images và file Excel thành file ZIP\n");
        content.append("11. Tải lên file ZIP qua chức năng 'Nhập sản phẩm từ Excel' trên hệ thống\n\n");

        content.append("II. CẤU TRÚC THƯ MỤC ẢNH\n");
        content.append("Tạo thư mục Images với cấu trúc như sau:\n\n");
        content.append("Images/\n");
        content.append("  ├── thumbnails/           (Chứa ảnh thumbnail cho sản phẩm)\n");
        content.append("  │    ├── thumbnail1.jpg    (Ảnh thumbnail cho sản phẩm 1)\n");
        content.append("  │    └── thumbnail2.jpg    (Ảnh thumbnail cho sản phẩm 2)\n");
        content.append("  │\n");
        content.append("  ├── [SKU]/            (SKU của biến thể từ sheet 'Danh Sách SKU')\n");
        content.append("  │    ├── main.jpg      (Ảnh chính của biến thể)\n");
        content.append("  │    ├── 1.jpg         (Ảnh phụ 1)\n");
        content.append("  │    └── 2.jpg         (Ảnh phụ 2)\n");
        content.append("  ├── [SKU]/\n");
        content.append("  │    ├── main.jpg\n");
        content.append("  │    └── 1.jpg\n");
        content.append("  └── ...\n\n");

        content.append("Ví dụ:\n");
        content.append("Images/\n");
        content.append("  ├── thumbnails/\n");
        content.append("  │    ├── thumbnail1.jpg\n");
        content.append("  │    └── thumbnail2.jpg\n");
        content.append("  ├── ATN001-S-DEN/      (Áo Thun Nam, size S, màu Đen)\n");
        content.append("  │    ├── main.jpg\n");
        content.append("  │    ├── 1.jpg\n");
        content.append("  │    └── 2.jpg\n");
        content.append("  └── ATN001-M-TRG/      (Áo Thun Nam, size M, màu Trắng)\n");
        content.append("       ├── main.jpg\n");
        content.append("       └── 1.jpg\n\n");

        content.append("III. CHUYỂN ĐỔI TIẾNG VIỆT SANG KHÔNG DẤU\n");
        content.append("- Tên sản phẩm: 'Áo Thun Nam' -> 'ATN001' (SKU) và 'ao-thun-nam' (Slug)\n");
        content.append("- Màu sắc: 'Đỏ' -> 'DO', 'Xanh Dương' -> 'XDG'\n\n");

        content.append("IV. LƯU Ý QUAN TRỌNG\n");
        content.append("- Phải chọn từ dropdown cho Loại Sản Phẩm, Thương Hiệu, Kích Thước và Màu Sắc (các ô màu vàng)\n");
        content.append("- Nhập tên file ảnh thumbnail vào cột tương ứng (ô màu xanh lá)\n");
        content.append("- SKU được tự động tạo dựa trên tên sản phẩm, kích thước và màu sắc\n");
        content.append("- URL Slug được tự động tạo dựa trên tên sản phẩm\n");
        content.append("- SKU, Slug và tên thư mục KHÔNG CÓ DẤU TIẾNG VIỆT\n");
        content.append("- Xem sheet 'Danh Sách SKU' để lấy đúng tên thư mục cần tạo\n");
        content.append("- File main.jpg là bắt buộc cho mỗi biến thể\n");
        content.append("- Các file ảnh phụ (1.jpg, 2.jpg, vv) sẽ được liên kết với sản phẩm\n");
        content.append("- Định dạng ảnh hỗ trợ: JPG, PNG, WebP\n");
        content.append("- Kích thước ảnh tối đa: 5MB\n");
        content.append("- Không sử dụng ký tự đặc biệt trong tên thư mục và tên file\n");

        return content.toString();
    }

    /**
     * Hàm giả lập tạo dữ liệu ảnh mẫu
     */
    private byte[] createSampleImage() {
        // Đây là dữ liệu giả lập, bạn có thể thay bằng ảnh thực tế từ resource
        return "Sample Image Content".getBytes(StandardCharsets.UTF_8);
    }
}