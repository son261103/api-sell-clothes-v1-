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
import com.example.api_sell_clothes_v1.Utils.ProductExcelTemplateGenerator;
import com.example.api_sell_clothes_v1.Utils.SlugGenerator;
import com.example.api_sell_clothes_v1.Utils.ZipImportProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ProductExcelTemplateGenerator templateGenerator;
    private final SlugGenerator slugGenerator;

    private static final String TEMPLATE_VERSION = "1.3";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Trả về thông tin về template hiện tại
     */
    public ProductExcelTemplateResponseDTO getTemplateInfo() {
        List<String> supportedFeatures = Arrays.asList(
                "Nhập sản phẩm cơ bản",
                "Nhập biến thể sản phẩm",
                "Tự động tạo SKU",
                "Tự động tạo URL Slug",
                "Nhập hàng loạt hình ảnh",
                "Kiểm tra lỗi dữ liệu",
                "Dropdown chọn danh mục và thương hiệu",
                "Dropdown chọn kích thước và màu sắc",
                "Danh sách SKU tự động theo tên sản phẩm, kích thước và màu sắc",
                "Hướng dẫn cấu trúc thư mục ảnh",
                "Thêm thumbnail cho sản phẩm"
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
     * Tạo file Excel mẫu và hướng dẫn
     */
    public void generateTemplatePackage(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=product_import_template.xlsx");

        try (Workbook workbook = templateGenerator.createTemplateWorkbook()) {
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
     * Tạo file ZIP chứa file Excel mẫu và thư mục ảnh mẫu
     */
    public void generateFullTemplatePackage(HttpServletResponse response) throws IOException {
        templateGenerator.createFullTemplatePackage(response);
    }

    /**
     * Tạo hướng dẫn dạng text và gửi đến client
     */
    public void generateInstructionText(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=huong_dan_nhap_san_pham.txt");

        String instructions = templateGenerator.createInstructionText();

        try (Writer writer = response.getWriter()) {
            writer.write(instructions);
            writer.flush();
        } catch (Exception e) {
            log.error("Lỗi khi tạo file hướng dẫn: ", e);
            throw new IOException("Không thể tạo file hướng dẫn: " + e.getMessage());
        }
    }

    @Getter
    @Setter
    public static class ImportResult {
        private boolean success;
        private String message;
        private int totalErrors;
        private int errorRowCount;
        private byte[] errorReportBytes;
        private int totalImported;
    }

    public ImportResult importProductsFromExcel(MultipartFile zipFile) {
        Workbook workbook = null;

        try {
            ZipImportProcessor.ImportData importData = new ZipImportProcessor().processZipFile(zipFile);
            workbook = importData.getWorkbook();

            if (workbook == null) {
                throw new FileHandlingException("Không thể đọc file Excel từ ZIP");
            }

            ExcelErrorReport errorReport = new ExcelErrorReport(workbook);

            Sheet productSheet = workbook.getSheet("Sản Phẩm");
            if (productSheet == null) {
                throw new FileHandlingException("File Excel không chứa sheet 'Sản Phẩm'");
            }

            Sheet variantSheet = workbook.getSheet("Biến Thể");
            if (variantSheet == null) {
                throw new FileHandlingException("File Excel không chứa sheet 'Biến Thể'");
            }

            Map<String, Long> categoryMap = getCategoryMapByName();
            Map<String, Long> brandMap = getBrandMapByName();

            Map<Integer, Long> productRowIdMap = processProductSheetWithValidation(productSheet, categoryMap, brandMap, errorReport, importData);

            processVariantSheetWithSkuImagesAndValidation(variantSheet, productSheet, productRowIdMap, importData, errorReport);

            errorReport.markErrors();

            ImportResult result = new ImportResult();
            result.setSuccess(!errorReport.hasErrors());
            result.setTotalErrors(errorReport.getTotalErrors());
            result.setErrorRowCount(errorReport.getErrorRowCount());
            result.setTotalImported(productRowIdMap.size());

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
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    log.error("Lỗi khi đóng workbook: ", e);
                }
            }
        }
    }

    private Map<Integer, Long> processProductSheetWithValidation(
            Sheet productSheet,
            Map<String, Long> categoryMap,
            Map<String, Long> brandMap,
            ExcelErrorReport errorReport,
            ZipImportProcessor.ImportData importData) {

        Map<Integer, Long> productRowIdMap = new HashMap<>();
        int numRows = productSheet.getPhysicalNumberOfRows();

        for (int i = 1; i < numRows; i++) {
            Row row = productSheet.getRow(i);
            if (row == null) continue;

            String name = getCellStringValue(row.getCell(0));
            String categoryName = getCellStringValue(row.getCell(1));
            String brandName = getCellStringValue(row.getCell(2));
            String description = getCellStringValue(row.getCell(3));
            BigDecimal price = getCellBigDecimalValue(row.getCell(4));
            BigDecimal salePrice = getCellBigDecimalValue(row.getCell(5));
            String slug = getCellStringValue(row.getCell(6));
            String thumbnailPath = getCellStringValue(row.getCell(7)); // Lấy đường dẫn thumbnail

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

            if (hasError) {
                continue;
            }

            Long categoryId = categoryMap.get(categoryName);
            Long brandId = brandMap.get(brandName);

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
                if (thumbnailPath != null && !thumbnailPath.isEmpty()) {
                    try {
                        // Tìm thumbnail trong thư mục thumbnails - thử nhiều cách tìm
                        byte[] thumbnailData = null;

                        // Cách 1: Tìm đường dẫn cụ thể
                        String thumbnailFullPath = "thumbnails/" + thumbnailPath;
                        thumbnailData = importData.getFileContent(thumbnailFullPath);

                        // Cách 2: Tìm đường dẫn đầy đủ với Images/
                        if (thumbnailData == null) {
                            thumbnailFullPath = "Images/thumbnails/" + thumbnailPath;
                            thumbnailData = importData.getFileContent(thumbnailFullPath);
                        }

                        // Cách 3: Tìm theo tên file
                        if (thumbnailData == null) {
                            for (String path : importData.getAllFilePaths()) {
                                if (path.contains("thumbnails/") && path.endsWith(thumbnailPath)) {
                                    thumbnailData = importData.getFileContent(path);
                                    thumbnailFullPath = path;
                                    break;
                                }
                            }
                        }

                        if (thumbnailData != null) {
                            thumbnailFile = createMultipartFile(thumbnailData, thumbnailPath,
                                    determineContentType(thumbnailPath));
                            log.info("Đã tìm thấy thumbnail: {}", thumbnailFullPath);
                        } else {
                            log.warn("Không tìm thấy thumbnail cho sản phẩm '{}'", name);
                        }
                    } catch (Exception e) {
                        log.warn("Lỗi khi xử lý thumbnail {}: {}", thumbnailPath, e.getMessage());
                    }
                }

                // Tạo sản phẩm với thumbnail (nếu có)
                ProductResponseDTO savedProduct = productService.createProduct(productDTO, thumbnailFile);
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

    private void processVariantSheetWithSkuImagesAndValidation(
            Sheet variantSheet,
            Sheet productSheet,
            Map<Integer, Long> productRowIdMap,
            ZipImportProcessor.ImportData importData,
            ExcelErrorReport errorReport) {

        int numRows = variantSheet.getPhysicalNumberOfRows();
        Map<Long, List<BulkProductVariantCreateDTO.VariantDetail>> productVariantsMap = new HashMap<>();
        Map<Integer, Long> rowToProductIdMap = new HashMap<>();
        Map<Integer, BulkProductVariantCreateDTO.VariantDetail> rowToVariantMap = new HashMap<>();

        for (int i = 1; i < numRows; i++) {
            Row row = variantSheet.getRow(i);
            if (row == null) continue;

            Double productRowId = getCellDoubleValue(row.getCell(0));
            String size = getCellStringValue(row.getCell(1));
            String color = getCellStringValue(row.getCell(2));
            Integer stockQuantity = getCellIntValue(row.getCell(3));

            boolean hasError = false;

            if (productRowId == null) {
                errorReport.addError("Biến Thể", i, "STT sản phẩm không được để trống");
                hasError = true;
            } else if (!productRowIdMap.containsKey(productRowId.intValue())) {
                errorReport.addError("Biến Thể", i, "Không tìm thấy sản phẩm với STT: " + productRowId.intValue());
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

            if (hasError) {
                continue;
            }

            Long productId = productRowIdMap.get(productRowId.intValue());

            // Lấy tên sản phẩm từ sheet sản phẩm
            Row productRow = productSheet.getRow(productRowId.intValue());
            String productName = getCellStringValue(productRow.getCell(0));

            // Tạo SKU tự động bằng skuGeneratorService - đảm bảo loại bỏ dấu tiếng Việt
            String sku = skuGeneratorService.generateSku(productName, size, color);
            log.info("Đã tạo SKU không dấu: {} cho sản phẩm: '{}', kích thước: '{}', màu: '{}'",
                    sku, productName, size, color);

            BulkProductVariantCreateDTO.VariantDetail variantDetail = new BulkProductVariantCreateDTO.VariantDetail();
            variantDetail.setSize(size);
            variantDetail.setColor(color);
            variantDetail.setSku(sku);
            variantDetail.setStockQuantity(stockQuantity);

            if (!productVariantsMap.containsKey(productId)) {
                productVariantsMap.put(productId, new ArrayList<>());
            }
            productVariantsMap.get(productId).add(variantDetail);

            rowToProductIdMap.put(i, productId);
            rowToVariantMap.put(i, variantDetail);
        }

        Map<Integer, String> rowToSkuMap = new HashMap<>();

        for (Map.Entry<Long, List<BulkProductVariantCreateDTO.VariantDetail>> entry : productVariantsMap.entrySet()) {
            Long productId = entry.getKey();
            List<BulkProductVariantCreateDTO.VariantDetail> variants = entry.getValue();

            try {
                BulkProductVariantCreateDTO bulkDTO = new BulkProductVariantCreateDTO();
                bulkDTO.setProductId(productId);
                bulkDTO.setVariants(variants);

                List<String> createdSkus = productVariantService.createBulkVariants(bulkDTO);

                for (int i = 0; i < variants.size(); i++) {
                    for (Map.Entry<Integer, BulkProductVariantCreateDTO.VariantDetail> variantEntry : rowToVariantMap.entrySet()) {
                        if (variantEntry.getValue() == variants.get(i) &&
                                productId.equals(rowToProductIdMap.get(variantEntry.getKey())) &&
                                i < createdSkus.size()) {
                            rowToSkuMap.put(variantEntry.getKey(), createdSkus.get(i));
                            break;
                        }
                    }
                }

                // Xử lý ảnh cho từng SKU
                for (String sku : createdSkus) {
                    log.info("Đang tìm ảnh cho SKU: {}", sku);

                    boolean foundImages = processSingleVariantImages(sku, importData, productId);
                    if (!foundImages) {
                        log.warn("Không tìm thấy ảnh cho biến thể có SKU: {}", sku);
                    } else {
                        log.info("Đã tìm thấy và xử lý ảnh cho SKU: {}", sku);
                    }
                }

                log.info("Đã tạo {} biến thể cho sản phẩm ID: {}", variants.size(), productId);
            } catch (Exception e) {
                log.error("Lỗi khi tạo biến thể cho sản phẩm ID {}: {}", productId, e.getMessage());
                for (Map.Entry<Integer, Long> entry2 : rowToProductIdMap.entrySet()) {
                    if (productId.equals(entry2.getValue())) {
                        errorReport.addError("Biến Thể", entry2.getKey(),
                                "Lỗi khi tạo biến thể: " + e.getMessage());
                    }
                }
            }
        }

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
     * Xử lý ảnh cho một biến thể theo SKU
     * Trả về true nếu tìm thấy ảnh, false nếu không tìm thấy
     */
    private boolean processSingleVariantImages(String sku, ZipImportProcessor.ImportData importData, Long productId) {
        try {
            if (importData.hasImagesForSku(sku)) {
                byte[] mainImageData = importData.getMainImageForSku(sku);
                if (mainImageData != null) {
                    MultipartFile mainImageFile = createMultipartFile(mainImageData, "main.jpg", "image/jpeg");
                    productVariantService.updateVariantImageBySku(sku, mainImageFile);
                }

                List<byte[]> secondaryImagesData = importData.getSecondaryImagesForSku(sku);
                if (!secondaryImagesData.isEmpty()) {
                    ProductVariantResponseDTO variant = productVariantService.getVariantBySku(sku);
                    if (variant != null) {
                        Long variantProductId = variant.getProduct().getProductId();

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

                        if (!secondaryImageFiles.isEmpty()) {
                            productImageService.uploadProductImages(variantProductId, secondaryImageFiles);
                        }
                    }
                }
                return true;
            } else {
                log.warn("Không tìm thấy ảnh cho biến thể có SKU: {}", sku);
                return false;
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý ảnh cho biến thể có SKU {}: {}", sku, e.getMessage());
            return false;
        }
    }

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

    private Map<String, Long> getCategoryMapByName() {
        try {
            List<Categories> categories = categoryRepository.findAll();
            return categories.stream()
                    .filter(c -> Boolean.TRUE.equals(c.getStatus()))
                    .collect(Collectors.toMap(
                            Categories::getName,
                            Categories::getCategoryId,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách category: ", e);
            return new HashMap<>();
        }
    }

    private Map<String, Long> getBrandMapByName() {
        try {
            List<Brands> brands = brandRepository.findAll();
            return brands.stream()
                    .filter(b -> Boolean.TRUE.equals(b.getStatus()))
                    .collect(Collectors.toMap(
                            Brands::getName,
                            Brands::getBrandId,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách brand: ", e);
            return new HashMap<>();
        }
    }

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

    private Integer getCellIntValue(Cell cell) {
        Double value = getCellDoubleValue(cell);
        return value != null ? value.intValue() : null;
    }

    private BigDecimal getCellBigDecimalValue(Cell cell) {
        Double value = getCellDoubleValue(cell);
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}