package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ProductVariant.BulkProductVariantCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.ProductVariantResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductResponseDTO;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import com.example.api_sell_clothes_v1.Repository.ProductVariantRepository;
import com.example.api_sell_clothes_v1.Utils.ExcelErrorReport;
import com.example.api_sell_clothes_v1.Utils.ZipImportProcessor;
import jakarta.persistence.EntityNotFoundException;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Dịch vụ quản lý import/export biến thể sản phẩm qua Excel
 * Cho phép xử lý biến thể độc lập với sản phẩm
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantExcelService {
    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final ProductVariantImageService variantImageService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final SkuGeneratorService skuGeneratorService;

    private static final String[] SIZE_OPTIONS = {
            "S", "M", "L", "XL", "XXL", "XXXL",
            "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45"
    };

    private static final String[] COLOR_OPTIONS = {
            "Đen", "Trắng", "Đỏ", "Xanh lá", "Xanh dương", "Vàng", "Cam",
            "Hồng", "Tím", "Nâu", "Xám", "Be", "Xanh ngọc", "Đỏ đô"
    };

    private static final String[] VARIANT_HEADERS = {
            "Kích Thước (*)", "Màu Sắc (*)", "Số Lượng Tồn Kho (*)", "SKU (Tự động)"
    };

    /**
     * Lớp chứa kết quả import biến thể
     */
    @Getter
    @Setter
    public static class VariantImportResult {
        private boolean success;
        private String message;
        private int totalErrors;
        private int errorRowCount;
        private byte[] errorReportBytes;
        private int totalImported;
        private List<String> skuList;
    }

    /**
     * Tạo file Excel mẫu cho biến thể của một sản phẩm cụ thể
     */
    public void generateVariantTemplateForProduct(Long productId, HttpServletResponse response) throws IOException {
        // Lấy thông tin sản phẩm
        ProductResponseDTO product = productService.getProductById(productId);
        if (product == null) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        String fileName = "variant_template_" + product.getSlug() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (Workbook workbook = createVariantTemplateWorkbook(product)) {
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo template biến thể: ", e);
            throw new IOException("Không thể tạo template biến thể: " + e.getMessage());
        }
    }

    /**
     * Tạo file ZIP chứa template Excel và cấu trúc thư mục ảnh
     */
    public void generateVariantTemplatePackageForProduct(Long productId, HttpServletResponse response) throws IOException {
        // Lấy thông tin sản phẩm
        ProductResponseDTO product = productService.getProductById(productId);
        if (product == null) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        String fileName = "variant_template_" + product.getSlug() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // 1. Tạo và thêm file Excel vào ZIP
            try (Workbook workbook = createVariantTemplateWorkbook(product)) {
                ZipEntry excelEntry = new ZipEntry("variant_template.xlsx");
                zipOut.putNextEntry(excelEntry);
                workbook.write(zipOut);
                zipOut.closeEntry();
            }

            // 2. Tạo cấu trúc thư mục ảnh mẫu
            // Tạo thư mục Images
            ZipEntry imagesFolder = new ZipEntry("Images/");
            zipOut.putNextEntry(imagesFolder);
            zipOut.closeEntry();

            // Tạo thư mục mẫu cho biến thể
            // Lấy 2 kích thước và màu đầu tiên làm mẫu
            String[] sampleSizes = Arrays.copyOf(SIZE_OPTIONS, 2);
            String[] sampleColors = Arrays.copyOf(COLOR_OPTIONS, 2);

            for (String size : sampleSizes) {
                for (String color : sampleColors) {
                    // Tạo SKU mẫu
                    String sku = skuGeneratorService.generateSku(product.getName(), size, color);

                    // Tạo thư mục con cho SKU
                    ZipEntry skuFolder = new ZipEntry("Images/" + sku + "/");
                    zipOut.putNextEntry(skuFolder);
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
                }
            }

            // 3. Thêm file hướng dẫn
            ZipEntry readmeEntry = new ZipEntry("README.txt");
            zipOut.putNextEntry(readmeEntry);
            String instructions = createInstructionText(product);
            zipOut.write(instructions.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();

            zipOut.finish();
            zipOut.flush();
        } catch (Exception e) {
            log.error("Lỗi khi tạo package template biến thể: ", e);
            throw new IOException("Không thể tạo package template biến thể: " + e.getMessage());
        }
    }

    /**
     * Xuất danh sách biến thể hiện có của sản phẩm ra Excel
     */
    public void exportProductVariants(Long productId, HttpServletResponse response) throws IOException {
        // Lấy thông tin sản phẩm
        ProductResponseDTO product = productService.getProductById(productId);
        if (product == null) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }

        // Lấy danh sách biến thể
        List<ProductVariantResponseDTO> variants = productVariantService.getVariantsByProductId(productId);

        String fileName = "variants_" + product.getSlug() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (Workbook workbook = createVariantExportWorkbook(product, variants)) {
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            }
        } catch (Exception e) {
            log.error("Lỗi khi xuất biến thể: ", e);
            throw new IOException("Không thể xuất biến thể: " + e.getMessage());
        }
    }

    /**
     * Import biến thể từ file Excel
     */
    public VariantImportResult importVariantsFromExcel(Long productId, MultipartFile file) {
        // Lấy thông tin sản phẩm
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet variantSheet = workbook.getSheetAt(0);
            if (variantSheet == null) {
                throw new FileHandlingException("File Excel không chứa sheet biến thể");
            }

            ExcelErrorReport errorReport = new ExcelErrorReport(workbook);
            Map<Integer, BulkProductVariantCreateDTO.VariantDetail> rowToVariantMap = new HashMap<>();
            List<BulkProductVariantCreateDTO.VariantDetail> variants = new ArrayList<>();

            int numRows = variantSheet.getPhysicalNumberOfRows();
            // Bỏ qua dòng tiêu đề
            for (int i = 1; i < numRows; i++) {
                Row row = variantSheet.getRow(i);
                if (row == null) continue;

                String size = getCellStringValue(row.getCell(0));
                String color = getCellStringValue(row.getCell(1));
                Integer stockQuantity = getCellIntValue(row.getCell(2));
                String sku = getCellStringValue(row.getCell(3)); // Có thể null nếu sử dụng SKU tự động

                boolean hasError = false;

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

                // Nếu có SKU, kiểm tra xem đã tồn tại chưa
                if (sku != null && !sku.isEmpty() && variantRepository.existsBySku(sku)) {
                    errorReport.addError("Biến Thể", i, "SKU đã tồn tại: " + sku);
                    hasError = true;
                }

                if (hasError) {
                    continue;
                }

                // Tạo SKU tự động nếu chưa có
                if (sku == null || sku.isEmpty()) {
                    sku = skuGeneratorService.generateSku(productId, size, color);
                }

                BulkProductVariantCreateDTO.VariantDetail variantDetail = new BulkProductVariantCreateDTO.VariantDetail();
                variantDetail.setSize(size);
                variantDetail.setColor(color);
                variantDetail.setSku(sku);
                variantDetail.setStockQuantity(stockQuantity);

                variants.add(variantDetail);
                rowToVariantMap.put(i, variantDetail);
            }

            if (errorReport.hasErrors()) {
                errorReport.markErrors();
                VariantImportResult result = new VariantImportResult();
                result.setSuccess(false);
                result.setTotalErrors(errorReport.getTotalErrors());
                result.setErrorRowCount(errorReport.getErrorRowCount());
                result.setErrorReportBytes(errorReport.toByteArray());
                result.setMessage("Import không thành công. Có " + errorReport.getTotalErrors() +
                        " lỗi trong " + errorReport.getErrorRowCount() + " dòng.");
                return result;
            }

            // Nếu không có lỗi, tiến hành tạo biến thể
            if (!variants.isEmpty()) {
                BulkProductVariantCreateDTO bulkDTO = new BulkProductVariantCreateDTO();
                bulkDTO.setProductId(productId);
                bulkDTO.setVariants(variants);

                List<String> createdSkus = productVariantService.createBulkVariants(bulkDTO);

                VariantImportResult result = new VariantImportResult();
                result.setSuccess(true);
                result.setTotalImported(createdSkus.size());
                result.setMessage("Import thành công. Đã nhập " + createdSkus.size() + " biến thể.");
                result.setSkuList(createdSkus);
                return result;
            } else {
                VariantImportResult result = new VariantImportResult();
                result.setSuccess(true);
                result.setTotalImported(0);
                result.setMessage("Không có biến thể nào được import.");
                result.setSkuList(Collections.emptyList());
                return result;
            }
        } catch (Exception e) {
            log.error("Lỗi khi import biến thể từ Excel: ", e);
            VariantImportResult result = new VariantImportResult();
            result.setSuccess(false);
            result.setMessage("Lỗi khi import biến thể: " + e.getMessage());
            return result;
        }
    }

    /**
     * Import biến thể từ file ZIP (Excel + ảnh)
     */
    public VariantImportResult importVariantsFromZip(Long productId, MultipartFile zipFile) {
        try {
            ZipImportProcessor.ImportData importData = new ZipImportProcessor().processZipFile(zipFile);
            Workbook workbook = importData.getWorkbook();

            if (workbook == null) {
                throw new FileHandlingException("Không thể đọc file Excel từ ZIP");
            }

            // Import biến thể từ Excel
            VariantImportResult result = importVariantsFromWorkbook(productId, workbook);

            // Nếu import thành công, xử lý ảnh cho từng biến thể
            if (result.isSuccess() && result.getSkuList() != null && !result.getSkuList().isEmpty()) {
                int imagesProcessed = 0;

                for (String sku : result.getSkuList()) {
                    if (importData.hasImagesForSku(sku)) {
                        // Xử lý ảnh chính
                        byte[] mainImageData = importData.getMainImageForSku(sku);
                        if (mainImageData != null) {
                            MultipartFile mainImageFile = createMultipartFile(mainImageData, "main.jpg", "image/jpeg");
                            productVariantService.updateVariantImageBySku(sku, mainImageFile);
                            imagesProcessed++;
                        }

                        // Xử lý ảnh phụ
                        processSingleVariantSecondaryImages(sku, importData);
                    }
                }

                // Cập nhật thông báo
                result.setMessage(result.getMessage() + " Đã xử lý ảnh cho " + imagesProcessed + " biến thể.");
            }

            return result;
        } catch (Exception e) {
            log.error("Lỗi khi import biến thể từ ZIP: ", e);
            VariantImportResult result = new VariantImportResult();
            result.setSuccess(false);
            result.setMessage("Lỗi khi import biến thể: " + e.getMessage());
            return result;
        }
    }

    /**
     * Xử lý ảnh phụ cho một biến thể
     */
    private void processSingleVariantSecondaryImages(String sku, ZipImportProcessor.ImportData importData) {
        try {
            List<byte[]> secondaryImagesData = importData.getSecondaryImagesForSku(sku);
            if (!secondaryImagesData.isEmpty()) {
                ProductVariantResponseDTO variant = productVariantService.getVariantBySku(sku);
                if (variant != null) {
                    Long productId = variant.getProduct().getProductId();

                    // Xử lý ảnh phụ
                    // Tùy thuộc vào cách bạn lưu trữ ảnh phụ, bạn có thể cần điều chỉnh đoạn code này
                    // Ví dụ: gắn ảnh phụ vào sản phẩm hoặc biến thể
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý ảnh phụ cho biến thể có SKU {}: {}", sku, e.getMessage());
        }
    }

    /**
     * Import biến thể từ Workbook
     */
    private VariantImportResult importVariantsFromWorkbook(Long productId, Workbook workbook) {
        // Lấy thông tin sản phẩm
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

        Sheet variantSheet = workbook.getSheetAt(0);
        if (variantSheet == null) {
            throw new FileHandlingException("File Excel không chứa sheet biến thể");
        }

        ExcelErrorReport errorReport = new ExcelErrorReport(workbook);
        List<BulkProductVariantCreateDTO.VariantDetail> variants = new ArrayList<>();

        int numRows = variantSheet.getPhysicalNumberOfRows();
        // Bỏ qua dòng tiêu đề
        for (int i = 1; i < numRows; i++) {
            Row row = variantSheet.getRow(i);
            if (row == null) continue;

            String size = getCellStringValue(row.getCell(0));
            String color = getCellStringValue(row.getCell(1));
            Integer stockQuantity = getCellIntValue(row.getCell(2));
            String sku = getCellStringValue(row.getCell(3)); // Có thể null nếu sử dụng SKU tự động

            boolean hasError = false;

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

            // Nếu có SKU, kiểm tra xem đã tồn tại chưa
            if (sku != null && !sku.isEmpty() && variantRepository.existsBySku(sku)) {
                errorReport.addError("Biến Thể", i, "SKU đã tồn tại: " + sku);
                hasError = true;
            }

            if (hasError) {
                continue;
            }

            // Tạo SKU tự động nếu chưa có
            if (sku == null || sku.isEmpty()) {
                sku = skuGeneratorService.generateSku(productId, size, color);
            }

            BulkProductVariantCreateDTO.VariantDetail variantDetail = new BulkProductVariantCreateDTO.VariantDetail();
            variantDetail.setSize(size);
            variantDetail.setColor(color);
            variantDetail.setSku(sku);
            variantDetail.setStockQuantity(stockQuantity);

            variants.add(variantDetail);
        }

        if (errorReport.hasErrors()) {
            errorReport.markErrors();
            VariantImportResult result = new VariantImportResult();
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
            return result;
        }

        // Nếu không có lỗi, tiến hành tạo biến thể
        if (!variants.isEmpty()) {
            BulkProductVariantCreateDTO bulkDTO = new BulkProductVariantCreateDTO();
            bulkDTO.setProductId(productId);
            bulkDTO.setVariants(variants);

            List<String> createdSkus = productVariantService.createBulkVariants(bulkDTO);

            VariantImportResult result = new VariantImportResult();
            result.setSuccess(true);
            result.setTotalImported(createdSkus.size());
            result.setMessage("Import thành công. Đã nhập " + createdSkus.size() + " biến thể.");
            result.setSkuList(createdSkus);
            return result;
        } else {
            VariantImportResult result = new VariantImportResult();
            result.setSuccess(true);
            result.setTotalImported(0);
            result.setMessage("Không có biến thể nào được import.");
            result.setSkuList(Collections.emptyList());
            return result;
        }
    }

    /**
     * Tạo Workbook Excel mẫu cho biến thể
     */
    private Workbook createVariantTemplateWorkbook(ProductResponseDTO product) {
        Workbook workbook = new XSSFWorkbook();

        // Tạo sheet tham chiếu trước
        Sheet sizesSheet = createSizesSheet(workbook);
        Sheet colorsSheet = createColorsSheet(workbook);

        // Tạo sheet biến thể
        Sheet variantSheet = workbook.createSheet("Biến Thể");

        // Thiết lập độ rộng cột
        for (int i = 0; i < VARIANT_HEADERS.length; i++) {
            variantSheet.setColumnWidth(i, 4000);
        }

        // Tạo style cho tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Thêm phần thông tin sản phẩm
        Row productInfoRow1 = variantSheet.createRow(0);
        Cell productInfoCell1 = productInfoRow1.createCell(0);
        productInfoCell1.setCellValue("Sản phẩm: " + product.getName());
        CellStyle productInfoStyle = workbook.createCellStyle();
        Font productInfoFont = workbook.createFont();
        productInfoFont.setBold(true);
        productInfoStyle.setFont(productInfoFont);
        productInfoCell1.setCellStyle(productInfoStyle);
        variantSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row productInfoRow2 = variantSheet.createRow(1);
        Cell productInfoCell2 = productInfoRow2.createCell(0);
        productInfoCell2.setCellValue("ID: " + product.getProductId());
        productInfoCell2.setCellStyle(productInfoStyle);
        variantSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

        // Tạo row tiêu đề
        Row headerRow = variantSheet.createRow(3);
        for (int i = 0; i < VARIANT_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(VARIANT_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        // Style cho các ô dropdown
        CellStyle dropdownStyle = workbook.createCellStyle();
        dropdownStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        dropdownStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Thêm dropdown cho kích thước và màu sắc
        createSizeDropdown(variantSheet, sizesSheet);
        createColorDropdown(variantSheet, colorsSheet);

        // Style cho các ô thường
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Style cho ô SKU (tự động tạo)
        CellStyle skuStyle = workbook.createCellStyle();
        skuStyle.cloneStyleFrom(dataStyle);
        Font grayFont = workbook.createFont();
        grayFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        grayFont.setItalic(true);
        skuStyle.setFont(grayFont);

        // Thêm 10 dòng mẫu
        for (int i = 4; i <= 14; i++) {
            Row row = variantSheet.createRow(i);

            // Kích thước - cần chọn từ dropdown
            Cell sizeCell = row.createCell(0);
            sizeCell.setCellStyle(dropdownStyle);

            // Màu sắc - cần chọn từ dropdown
            Cell colorCell = row.createCell(1);
            colorCell.setCellStyle(dropdownStyle);

            // Số lượng tồn kho
            Cell stockCell = row.createCell(2);
            stockCell.setCellValue(100); // Giá trị mặc định
            stockCell.setCellStyle(dataStyle);

            // SKU (tự động sinh)
            Cell skuCell = row.createCell(3);
            // Công thức tính SKU dựa trên tên sản phẩm, kích thước và màu sắc
            String generateSkuFormula = "IF(AND(A" + (i+1) + "<>\"\", B" + (i+1) + "<>\"\"), " +
                    "\"[SKU sẽ được tự động tạo]\", \"\")";
            skuCell.setCellFormula(generateSkuFormula);
            skuCell.setCellStyle(skuStyle);
        }

        // Thêm hướng dẫn
        Row guideRow1 = variantSheet.createRow(16);
        Cell guideCell1 = guideRow1.createCell(0);
        guideCell1.setCellValue("Hướng dẫn:");
        guideCell1.setCellStyle(productInfoStyle);
        variantSheet.addMergedRegion(new CellRangeAddress(16, 16, 0, 3));

        Row guideRow2 = variantSheet.createRow(17);
        Cell guideCell2 = guideRow2.createCell(0);
        guideCell2.setCellValue("1. Chọn Kích Thước và Màu Sắc từ dropdown (ô màu vàng)");
        variantSheet.addMergedRegion(new CellRangeAddress(17, 17, 0, 3));

        Row guideRow3 = variantSheet.createRow(18);
        Cell guideCell3 = guideRow3.createCell(0);
        guideCell3.setCellValue("2. Nhập Số Lượng Tồn Kho");
        variantSheet.addMergedRegion(new CellRangeAddress(18, 18, 0, 3));

        Row guideRow4 = variantSheet.createRow(19);
        Cell guideCell4 = guideRow4.createCell(0);
        guideCell4.setCellValue("3. SKU sẽ được tự động tạo khi import");
        variantSheet.addMergedRegion(new CellRangeAddress(19, 19, 0, 3));

        Row guideRow5 = variantSheet.createRow(20);
        Cell guideCell5 = guideRow5.createCell(0);
        guideCell5.setCellValue("4. Tạo thư mục ảnh theo cấu trúc: Images/[SKU]/main.jpg, 1.jpg, ...");
        variantSheet.addMergedRegion(new CellRangeAddress(20, 20, 0, 3));

        return workbook;
    }

    /**
     * Tạo Workbook xuất danh sách biến thể
     */
    private Workbook createVariantExportWorkbook(ProductResponseDTO product, List<ProductVariantResponseDTO> variants) {
        Workbook workbook = new XSSFWorkbook();

        // Tạo sheet biến thể
        Sheet variantSheet = workbook.createSheet("Biến Thể");

        // Thiết lập độ rộng cột
        variantSheet.setColumnWidth(0, 4000); // Kích thước
        variantSheet.setColumnWidth(1, 4000); // Màu sắc
        variantSheet.setColumnWidth(2, 4000); // Số lượng tồn kho
        variantSheet.setColumnWidth(3, 5000); // SKU
        variantSheet.setColumnWidth(4, 4000); // Ngày tạo
        variantSheet.setColumnWidth(5, 3000); // Trạng thái

        // Tạo style cho tiêu đề
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Thêm phần thông tin sản phẩm
        Row productInfoRow1 = variantSheet.createRow(0);
        Cell productInfoCell1 = productInfoRow1.createCell(0);
        productInfoCell1.setCellValue("Sản phẩm: " + product.getName());
        CellStyle productInfoStyle = workbook.createCellStyle();
        Font productInfoFont = workbook.createFont();
        productInfoFont.setBold(true);
        productInfoStyle.setFont(productInfoFont);
        productInfoCell1.setCellStyle(productInfoStyle);
        variantSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        Row productInfoRow2 = variantSheet.createRow(1);
        Cell productInfoCell2 = productInfoRow2.createCell(0);
        productInfoCell2.setCellValue("ID: " + product.getProductId());
        productInfoCell2.setCellStyle(productInfoStyle);
        variantSheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        // Tạo row tiêu đề
        Row headerRow = variantSheet.createRow(3);
        String[] exportHeaders = {"Kích Thước", "Màu Sắc", "Số Lượng Tồn Kho", "SKU", "Ngày Tạo", "Trạng Thái"};
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

        // Thêm dữ liệu biến thể
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (int i = 0; i < variants.size(); i++) {
            ProductVariantResponseDTO variant = variants.get(i);
            Row row = variantSheet.createRow(i + 4);

            Cell sizeCell = row.createCell(0);
            sizeCell.setCellValue(variant.getSize());
            sizeCell.setCellStyle(dataStyle);

            Cell colorCell = row.createCell(1);
            colorCell.setCellValue(variant.getColor());
            colorCell.setCellStyle(dataStyle);

            Cell stockCell = row.createCell(2);
            stockCell.setCellValue(variant.getStockQuantity());
            stockCell.setCellStyle(dataStyle);

            Cell skuCell = row.createCell(3);
            skuCell.setCellValue(variant.getSku());
            skuCell.setCellStyle(dataStyle);

            Cell dateCell = row.createCell(4);
            LocalDateTime createdAt = variant.getCreatedAt();
            if (createdAt != null) {
                dateCell.setCellValue(createdAt.format(formatter));
            } else {
                dateCell.setCellValue("");
            }
            dateCell.setCellStyle(dataStyle);

            Cell statusCell = row.createCell(5);
            boolean isActive = variant.getStatus() != null && variant.getStatus();
            statusCell.setCellValue(isActive ? "Hoạt động" : "Không hoạt động");
            statusCell.setCellStyle(isActive ? activeStyle : inactiveStyle);
        }

        // Thêm thông tin tổng hợp
        Row summaryRow1 = variantSheet.createRow(variants.size() + 6);
        Cell summaryCell1 = summaryRow1.createCell(0);
        summaryCell1.setCellValue("Tổng số biến thể:");
        summaryCell1.setCellStyle(productInfoStyle);

        Cell summaryValueCell1 = summaryRow1.createCell(1);
        summaryValueCell1.setCellValue(variants.size());
        summaryValueCell1.setCellStyle(dataStyle);

        // Đếm biến thể hoạt động
        long activeCount = variants.stream().filter(v -> v.getStatus() != null && v.getStatus()).count();

        Row summaryRow2 = variantSheet.createRow(variants.size() + 7);
        Cell summaryCell2 = summaryRow2.createCell(0);
        summaryCell2.setCellValue("Biến thể hoạt động:");
        summaryCell2.setCellStyle(productInfoStyle);

        Cell summaryValueCell2 = summaryRow2.createCell(1);
        summaryValueCell2.setCellValue(activeCount);
        summaryValueCell2.setCellStyle(dataStyle);

        return workbook;
    }

    /**
     * Tạo sheet danh sách kích thước
     */
    private Sheet createSizesSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Kích Thước");
        workbook.setSheetHidden(workbook.getSheetIndex("Danh Sách Kích Thước"), true);

        sheet.setColumnWidth(0, 3000);

        for (int i = 0; i < SIZE_OPTIONS.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(SIZE_OPTIONS[i]);
        }

        return sheet;
    }

    /**
     * Tạo sheet danh sách màu sắc
     */
    private Sheet createColorsSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("Danh Sách Màu Sắc");
        workbook.setSheetHidden(workbook.getSheetIndex("Danh Sách Màu Sắc"), true);

        sheet.setColumnWidth(0, 3000);

        for (int i = 0; i < COLOR_OPTIONS.length; i++) {
            Row row = sheet.createRow(i);
            Cell cell = row.createCell(0);
            cell.setCellValue(COLOR_OPTIONS[i]);
        }

        return sheet;
    }

    /**
     * Tạo dropdown cho cột Kích Thước
     */
    private void createSizeDropdown(Sheet variantSheet, Sheet sizesSheet) {
        // Chuẩn bị danh sách các giá trị hiển thị trong dropdown
        String[] sizes = new String[SIZE_OPTIONS.length];
        System.arraycopy(SIZE_OPTIONS, 0, sizes, 0, SIZE_OPTIONS.length);

        // Sử dụng constraint kiểu explicit list
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) variantSheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                dvHelper.createExplicitListConstraint(sizes);

        // Áp dụng validation cho tất cả các hàng trong cột Kích Thước (cột A)
        CellRangeAddressList addressList = new CellRangeAddressList(4, 14, 0, 0);
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
        String[] colors = new String[COLOR_OPTIONS.length];
        System.arraycopy(COLOR_OPTIONS, 0, colors, 0, COLOR_OPTIONS.length);

        // Sử dụng constraint kiểu explicit list
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper((XSSFSheet) variantSheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                dvHelper.createExplicitListConstraint(colors);

        // Áp dụng validation cho tất cả các hàng trong cột Màu Sắc (cột B)
        CellRangeAddressList addressList = new CellRangeAddressList(4, 14, 1, 1);
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
     * Tạo file hướng dẫn
     */
    private String createInstructionText(ProductResponseDTO product) {
        StringBuilder content = new StringBuilder();
        content.append("HƯỚNG DẪN NHẬP BIẾN THỂ CHO SẢN PHẨM\n");
        content.append("======================================\n\n");
        content.append("Thông tin sản phẩm:\n");
        content.append("- Tên sản phẩm: ").append(product.getName()).append("\n");
        content.append("- ID sản phẩm: ").append(product.getProductId()).append("\n\n");

        content.append("I. HƯỚNG DẪN SỬ DỤNG FILE EXCEL\n");
        content.append("1. Mở file variant_template.xlsx\n");
        content.append("2. Nhập thông tin biến thể vào sheet 'Biến Thể'\n");
        content.append("3. Chọn Kích Thước và Màu Sắc từ dropdown có sẵn\n");
        content.append("4. Nhập Số Lượng Tồn Kho\n");
        content.append("5. Cột SKU sẽ tự động được điền khi import\n");
        content.append("6. Lưu file Excel sau khi hoàn thành\n\n");

        content.append("II. HƯỚNG DẪN CHUẨN BỊ ẢNH BIẾN THỂ\n");
        content.append("1. Tạo thư mục Images/\n");
        content.append("2. Cho mỗi biến thể, tạo thư mục con theo tên SKU trong thư mục Images/\n");
        content.append("   Lưu ý: SKU sẽ được tạo theo định dạng: 3 chữ cái đầu + 3 số + size + màu sắc\n");
        content.append("   Ví dụ: ABC001-L-DEN (Áo Basic, size L, màu Đen)\n");
        content.append("3. Trong mỗi thư mục biến thể, cần có các file sau:\n");
        content.append("   - main.jpg: Ảnh chính của biến thể (bắt buộc)\n");
        content.append("   - 1.jpg, 2.jpg, ...: Các ảnh phụ (tùy chọn)\n\n");

        content.append("III. HƯỚNG DẪN IMPORT\n");
        content.append("1. Nén file Excel và thư mục Images thành file ZIP\n");
        content.append("2. Tải lên file ZIP qua chức năng 'Import Biến Thể' trên hệ thống\n");
        content.append("3. Hệ thống sẽ xử lý file Excel và import biến thể\n");
        content.append("4. Nếu có lỗi, hệ thống sẽ trả về file báo cáo lỗi\n\n");

        content.append("IV. LƯU Ý QUAN TRỌNG\n");
        content.append("- Kích Thước và Màu Sắc là bắt buộc\n");
        content.append("- Số Lượng Tồn Kho không được âm\n");
        content.append("- Mỗi biến thể phải có ảnh main.jpg\n");
        content.append("- SKU sẽ tự động được tạo, không cần nhập thủ công\n");
        content.append("- SKU sẽ không có dấu tiếng Việt\n");

        return content.toString();
    }

    /**
     * Tạo hình ảnh mẫu
     */
    private byte[] createSampleImage() {
        return "Sample Image Content".getBytes(StandardCharsets.UTF_8);
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
     * Lấy giá trị số nguyên từ cell
     */
    private Integer getCellIntValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    try {
                        return Integer.parseInt(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                case FORMULA:
                    try {
                        return (int) cell.getNumericCellValue();
                    } catch (Exception e) {
                        try {
                            return Integer.parseInt(cell.getStringCellValue().trim());
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
}