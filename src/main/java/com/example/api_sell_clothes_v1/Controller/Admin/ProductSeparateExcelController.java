package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ErrorResponse;
import com.example.api_sell_clothes_v1.Service.ProductSingleExcelService;
import com.example.api_sell_clothes_v1.Service.ProductSingleExcelService.ProductImportResult;
import com.example.api_sell_clothes_v1.Service.ProductVariantExcelService;
import com.example.api_sell_clothes_v1.Service.ProductVariantExcelService.VariantImportResult;
import com.example.api_sell_clothes_v1.Utils.ZipImportProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Controller quản lý import/export riêng biệt cho sản phẩm và biến thể
 * Cho phép người dùng thêm sản phẩm trước, sau đó thêm biến thể riêng cho từng sản phẩm
 */
@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCTS + "/separate")
@RequiredArgsConstructor
public class ProductSeparateExcelController {

    private final ProductSingleExcelService productSingleExcelService;
    private final ProductVariantExcelService productVariantExcelService;

    // Bộ nhớ tạm lưu trữ báo cáo lỗi
    private static final ConcurrentHashMap<String, byte[]> errorReports = new ConcurrentHashMap<>();

    //==========================================================================
    // ENDPOINTS QUẢN LÝ SẢN PHẨM
    //==========================================================================

    /**
     * Tải về template Excel cho sản phẩm (không bao gồm biến thể)
     */
    @GetMapping("/product/template")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadProductTemplate(HttpServletResponse response) {
        try {
            log.info("Bắt đầu tạo file template Excel sản phẩm");
            productSingleExcelService.generateProductTemplateFile(response);
            log.info("Đã tạo file template Excel sản phẩm thành công");
        } catch (IOException e) {
            log.error("Lỗi khi tạo file template sản phẩm: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Không thể tạo template: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
            }
        }
    }

    /**
     * Tải về file ZIP chứa template Excel và thư mục thumbnail mẫu cho sản phẩm
     */
    @GetMapping("/product/template-package")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadProductTemplatePackage(HttpServletResponse response) {
        try {
            log.info("Bắt đầu tạo file ZIP template sản phẩm");
            productSingleExcelService.generateProductTemplatePackage(response);
            log.info("Đã tạo file ZIP template sản phẩm thành công");
        } catch (IOException e) {
            log.error("Lỗi khi tạo file ZIP template sản phẩm: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Không thể tạo template ZIP: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
            }
        }
    }

    /**
     * Xuất danh sách sản phẩm đã chọn ra Excel
     */
    @PostMapping("/product/export")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void exportProducts(
            HttpServletResponse response,
            @RequestBody(required = false) Map<String, List<Long>> requestBody) {

        List<Long> productIds = requestBody != null ? requestBody.get("productIds") : null;
        int totalProducts = productIds != null ? productIds.size() : 0;

        log.info("Bắt đầu xuất {} sản phẩm ra Excel", totalProducts);

        try {
            productSingleExcelService.exportProducts(productIds, response);
            log.info("Đã xuất {} sản phẩm ra Excel thành công", totalProducts);
        } catch (IOException e) {
            log.error("Lỗi khi xuất sản phẩm ra Excel: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Không thể xuất sản phẩm: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
            }
        }
    }

    /**
     * Import sản phẩm cơ bản từ file Excel (không bao gồm biến thể)
     */
    @PostMapping(value = "/product/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<?> importProducts(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Bắt đầu import sản phẩm từ Excel: filename={}, size={} bytes",
                    file.getOriginalFilename(), file.getSize());

            ProductImportResult result = productSingleExcelService.importProductsFromExcel(file);

            // Nếu có lỗi, lưu báo cáo lỗi và trả về URL để tải
            if (!result.isSuccess() && result.getErrorReportBytes() != null) {
                // Tạo ID duy nhất cho báo cáo lỗi
                String errorReportId = UUID.randomUUID().toString();

                // Lưu báo cáo lỗi vào bộ nhớ tạm
                errorReports.put(errorReportId, result.getErrorReportBytes());

                // Tạo URL cho báo cáo lỗi
                String errorUrl = "/api/v1/products/separate/error-report/" + errorReportId;
                log.warn("Import từ Excel không thành công: {} lỗi, {} dòng có lỗi, báo cáo lỗi: {}",
                        result.getTotalErrors(), result.getErrorRowCount(), errorUrl);

                // Tạo phản hồi lỗi với thông tin chi tiết
                ErrorResponse errorResponse = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.OK.value())
                        .error("Import Error")
                        .message(result.getMessage())
                        .build();

                return ResponseEntity.ok()
                        .header("X-Error-Total", String.valueOf(result.getTotalErrors()))
                        .header("X-Error-Rows", String.valueOf(result.getErrorRowCount()))
                        .header("X-Error-Report-Url", errorUrl)
                        .header("X-Products-Imported", String.valueOf(result.getTotalImported()))
                        .header("X-Import-Status", "Error") // Thêm header trạng thái rõ ràng
                        .body(errorResponse);
            }

            // Nếu thành công, trả về thông báo kết quả
            ApiResponse response = new ApiResponse(true, result.getMessage());
            log.info("Import từ Excel thành công: {} sản phẩm đã được nhập", result.getTotalImported());

            return ResponseEntity.ok()
                    .header("X-Total-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Products-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Import-Status", "Success") // Thêm header trạng thái rõ ràng
                    .body(response);

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi import sản phẩm từ Excel: ", e);

            // Tạo phản hồi lỗi với thông tin chi tiết
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .error("Import Fatal Error")
                    .message("Lỗi khi import sản phẩm từ Excel: " + e.getMessage() +
                            ". Vui lòng kiểm tra định dạng file và thử lại.")
                    .build();

            // Vẫn trả về HTTP 200 để frontend dễ xử lý
            return ResponseEntity.ok()
                    .header("X-Import-Status", "Fatal-Error") // Thêm header trạng thái lỗi nghiêm trọng
                    .body(errorResponse);
        }
    }

    /**
     * Import sản phẩm cơ bản từ file ZIP (Excel + thumbnails)
     */
    @PostMapping(value = "/product/import-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<?> importProductsFromZip(@RequestParam("file") MultipartFile zipFile) {
        try {
            log.info("Bắt đầu request import sản phẩm từ ZIP: filename={}, size={}",
                    zipFile.getOriginalFilename(), zipFile.getSize());

            ProductImportResult result = productSingleExcelService.importProductsFromZip(zipFile);

            // Nếu có lỗi, lưu báo cáo lỗi và trả về URL để tải
            if (!result.isSuccess() && result.getErrorReportBytes() != null) {
                // Tạo ID duy nhất cho báo cáo lỗi
                String errorReportId = UUID.randomUUID().toString();

                // Lưu báo cáo lỗi vào bộ nhớ tạm
                errorReports.put(errorReportId, result.getErrorReportBytes());

                // Tạo URL cho báo cáo lỗi
                String errorUrl = "/api/v1/products/separate/error-report/" + errorReportId;
                log.warn("Import từ ZIP không thành công: {} lỗi, {} dòng có lỗi, báo cáo lỗi: {}",
                        result.getTotalErrors(), result.getErrorRowCount(), errorUrl);

                // Tạo phản hồi lỗi với thông tin chi tiết
                ErrorResponse errorResponse = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.OK.value())
                        .error("Import Error")
                        .message(result.getMessage())
                        .build();

                return ResponseEntity.ok()
                        .header("X-Error-Total", String.valueOf(result.getTotalErrors()))
                        .header("X-Error-Rows", String.valueOf(result.getErrorRowCount()))
                        .header("X-Error-Report-Url", errorUrl)
                        .header("X-Products-Imported", String.valueOf(result.getTotalImported()))
                        .header("X-Import-Status", "Error") // Thêm header trạng thái rõ ràng
                        .body(errorResponse);
            }

            // Nếu thành công, trả về thông báo kết quả
            ApiResponse response = new ApiResponse(true, result.getMessage());
            log.info("Import từ ZIP thành công: {} sản phẩm đã được nhập", result.getTotalImported());

            return ResponseEntity.ok()
                    .header("X-Total-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Products-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Import-Status", "Success") // Thêm header trạng thái rõ ràng
                    .body(response);

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi import sản phẩm từ ZIP: ", e);

            // Tạo phản hồi lỗi với thông tin chi tiết
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .error("Import Fatal Error")
                    .message("Lỗi khi import sản phẩm từ ZIP: " + e.getMessage() +
                            ". Vui lòng kiểm tra định dạng file và thử lại.")
                    .build();

            // Vẫn trả về HTTP 200 để frontend dễ xử lý
            return ResponseEntity.ok()
                    .header("X-Import-Status", "Fatal-Error") // Thêm header trạng thái lỗi nghiêm trọng
                    .body(errorResponse);
        }
    }

    //==========================================================================
    // ENDPOINTS QUẢN LÝ BIẾN THỂ
    //==========================================================================

    /**
     * Tải về template Excel cho biến thể của một sản phẩm cụ thể
     */
    @GetMapping("/variant/template/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadVariantTemplate(
            @PathVariable Long productId,
            HttpServletResponse response) {
        try {
            log.info("Bắt đầu tạo file template Excel biến thể cho sản phẩm ID: {}", productId);
            productVariantExcelService.generateVariantTemplateForProduct(productId, response);
            log.info("Đã tạo file template Excel biến thể thành công cho sản phẩm ID: {}", productId);
        } catch (Exception e) {
            log.error("Lỗi khi tạo file template biến thể: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Không thể tạo template biến thể: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
            }
        }
    }

    /**
     * Tải về file ZIP chứa template Excel và cấu trúc thư mục ảnh mẫu cho biến thể
     */
    @GetMapping("/variant/template-package/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadVariantTemplatePackage(
            @PathVariable Long productId,
            HttpServletResponse response) {
        try {
            log.info("Bắt đầu tạo file ZIP template biến thể cho sản phẩm ID: {}", productId);
            productVariantExcelService.generateVariantTemplatePackageForProduct(productId, response);
            log.info("Đã tạo file ZIP template biến thể thành công cho sản phẩm ID: {}", productId);
        } catch (Exception e) {
            log.error("Lỗi khi tạo file ZIP template biến thể: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Không thể tạo template ZIP biến thể: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
            }
        }
    }

    /**
     * Xuất danh sách biến thể của một sản phẩm ra Excel
     */
    @GetMapping("/variant/export/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void exportVariants(
            @PathVariable Long productId,
            HttpServletResponse response) {
        try {
            log.info("Bắt đầu xuất biến thể ra Excel cho sản phẩm ID: {}", productId);
            productVariantExcelService.exportProductVariants(productId, response);
            log.info("Đã xuất biến thể ra Excel thành công cho sản phẩm ID: {}", productId);
        } catch (Exception e) {
            log.error("Lỗi khi xuất biến thể ra Excel: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("Không thể xuất biến thể ra Excel: " + e.getMessage());
            } catch (IOException ex) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
            }
        }
    }

    /**
     * Import biến thể cho một sản phẩm cụ thể từ file Excel
     */
    @PostMapping(value = "/variant/import/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<?> importVariants(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Bắt đầu import biến thể từ Excel cho sản phẩm ID: {}, filename={}, size={}",
                    productId, file.getOriginalFilename(), file.getSize());

            VariantImportResult result = productVariantExcelService.importVariantsFromExcel(productId, file);

            // Nếu có lỗi, lưu báo cáo lỗi và trả về URL để tải
            if (!result.isSuccess() && result.getErrorReportBytes() != null) {
                // Tạo ID duy nhất cho báo cáo lỗi
                String errorReportId = UUID.randomUUID().toString();

                // Lưu báo cáo lỗi vào bộ nhớ tạm
                errorReports.put(errorReportId, result.getErrorReportBytes());

                // Tạo URL cho báo cáo lỗi
                String errorUrl = "/api/v1/products/separate/error-report/" + errorReportId;
                log.warn("Import biến thể từ Excel không thành công: {} lỗi, {} dòng có lỗi, báo cáo lỗi: {}",
                        result.getTotalErrors(), result.getErrorRowCount(), errorUrl);

                // Tạo phản hồi lỗi với thông tin chi tiết
                ErrorResponse errorResponse = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.OK.value())
                        .error("Variant Import Error")
                        .message(result.getMessage())
                        .build();

                return ResponseEntity.ok()
                        .header("X-Error-Total", String.valueOf(result.getTotalErrors()))
                        .header("X-Error-Rows", String.valueOf(result.getErrorRowCount()))
                        .header("X-Error-Report-Url", errorUrl)
                        .header("X-Variants-Imported", String.valueOf(result.getTotalImported()))
                        .header("X-Import-Status", "Error") // Thêm header trạng thái rõ ràng
                        .body(errorResponse);
            }

            // Nếu thành công, trả về thông báo kết quả
            ApiResponse response = new ApiResponse(true, result.getMessage());
            log.info("Import biến thể từ Excel thành công: {} biến thể đã được nhập cho sản phẩm ID: {}",
                    result.getTotalImported(), productId);

            return ResponseEntity.ok()
                    .header("X-Total-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Variants-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Import-Status", "Success") // Thêm header trạng thái rõ ràng
                    .body(response);

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi import biến thể từ Excel: ", e);

            // Tạo phản hồi lỗi với thông tin chi tiết
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .error("Variant Import Fatal Error")
                    .message("Lỗi khi import biến thể: " + e.getMessage() +
                            ". Vui lòng kiểm tra định dạng file và thử lại.")
                    .build();

            // Vẫn trả về HTTP 200 để frontend dễ xử lý
            return ResponseEntity.ok()
                    .header("X-Import-Status", "Fatal-Error") // Thêm header trạng thái lỗi nghiêm trọng
                    .body(errorResponse);
        }
    }

    /**
     * Import biến thể cho một sản phẩm cụ thể từ file ZIP (Excel + ảnh)
     */
    @PostMapping(value = "/variant/import-zip/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<?> importVariantsFromZip(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile zipFile) {
        try {
            log.info("Bắt đầu import biến thể từ ZIP cho sản phẩm ID: {}, filename={}, size={}",
                    productId, zipFile.getOriginalFilename(), zipFile.getSize());

            VariantImportResult result = productVariantExcelService.importVariantsFromZip(productId, zipFile);

            // Nếu có lỗi, lưu báo cáo lỗi và trả về URL để tải
            if (!result.isSuccess() && result.getErrorReportBytes() != null) {
                // Tạo ID duy nhất cho báo cáo lỗi
                String errorReportId = UUID.randomUUID().toString();

                // Lưu báo cáo lỗi vào bộ nhớ tạm
                errorReports.put(errorReportId, result.getErrorReportBytes());

                // Tạo URL cho báo cáo lỗi
                String errorUrl = "/api/v1/products/separate/error-report/" + errorReportId;
                log.warn("Import biến thể từ ZIP không thành công: {} lỗi, {} dòng có lỗi, báo cáo lỗi: {}",
                        result.getTotalErrors(), result.getErrorRowCount(), errorUrl);

                // Tạo phản hồi lỗi với thông tin chi tiết
                ErrorResponse errorResponse = ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.OK.value())
                        .error("Variant Import Error")
                        .message(result.getMessage())
                        .build();

                return ResponseEntity.ok()
                        .header("X-Error-Total", String.valueOf(result.getTotalErrors()))
                        .header("X-Error-Rows", String.valueOf(result.getErrorRowCount()))
                        .header("X-Error-Report-Url", errorUrl)
                        .header("X-Variants-Imported", String.valueOf(result.getTotalImported()))
                        .header("X-Import-Status", "Error") // Thêm header trạng thái rõ ràng
                        .body(errorResponse);
            }

            // Nếu thành công, trả về thông báo kết quả
            ApiResponse response = new ApiResponse(true, result.getMessage());
            log.info("Import biến thể từ ZIP thành công: {} biến thể đã được nhập cho sản phẩm ID: {}",
                    result.getTotalImported(), productId);

            return ResponseEntity.ok()
                    .header("X-Total-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Variants-Imported", String.valueOf(result.getTotalImported()))
                    .header("X-Variants-With-Images", result.getSkuList() != null ? String.valueOf(result.getSkuList().size()) : "0")
                    .header("X-Import-Status", "Success") // Thêm header trạng thái rõ ràng
                    .body(response);

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi import biến thể từ ZIP: ", e);

            // Tạo phản hồi lỗi với thông tin chi tiết
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .error("Variant Import Fatal Error")
                    .message("Lỗi khi import biến thể từ ZIP: " + e.getMessage() +
                            ". Vui lòng kiểm tra định dạng file và thử lại.")
                    .build();

            // Vẫn trả về HTTP 200 để frontend dễ xử lý
            return ResponseEntity.ok()
                    .header("X-Import-Status", "Fatal-Error") // Thêm header trạng thái lỗi nghiêm trọng
                    .body(errorResponse);
        }
    }

    //==========================================================================
    // ENDPOINTS CHUNG
    //==========================================================================

    /**
     * Tải báo cáo lỗi theo ID
     */
    @GetMapping("/error-report/{reportId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadErrorReport(@PathVariable String reportId, HttpServletResponse response) {
        log.info("Bắt đầu tải báo cáo lỗi với ID: {}", reportId);

        byte[] errorReportBytes = errorReports.get(reportId);

        if (errorReportBytes != null) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=error_report.xlsx");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            response.setContentLength(errorReportBytes.length);

            try {
                response.getOutputStream().write(errorReportBytes);
                response.getOutputStream().flush();

                // Xóa báo cáo lỗi khỏi bộ nhớ tạm sau khi tải xuống
                errorReports.remove(reportId);
                log.info("Đã tải báo cáo lỗi thành công với ID: {}", reportId);
            } catch (IOException e) {
                log.error("Lỗi khi tải báo cáo lỗi: ", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try {
                    response.getWriter().write("Không thể tải báo cáo lỗi: " + e.getMessage());
                } catch (IOException ex) {
                    log.error("Lỗi khi ghi phản hồi lỗi: ", ex);
                }
            }
        } else {
            log.warn("Báo cáo lỗi không tồn tại hoặc đã hết hạn với ID: {}", reportId);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("Báo cáo lỗi không tồn tại hoặc đã hết hạn");
            } catch (IOException e) {
                log.error("Lỗi khi ghi phản hồi lỗi: ", e);
            }
        }
    }

    /**
     * Kiểm tra trạng thái tiến trình nhập sản phẩm/biến thể
     */
    @GetMapping("/import/status")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<?> getImportStatus(@RequestParam(required = false) String jobId) {
        log.info("Kiểm tra trạng thái import với jobId: {}", jobId != null ? jobId : "không có");
        // Có thể triển khai hệ thống theo dõi tiến trình nhập sản phẩm/biến thể ở đây
        return ResponseEntity.ok(new ApiResponse(true, "Lấy trạng thái nhập thành công"));
    }

    /**
     * API debug để kiểm tra cấu trúc file ZIP
     */
    @PostMapping(value = "/debug/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<?> debugZipStructure(@RequestParam("file") MultipartFile zipFile) {
        try {
            log.info("Phân tích cấu trúc ZIP debug: filename={}, size={}",
                    zipFile.getOriginalFilename(), zipFile.getSize());

            ZipImportProcessor processor = new ZipImportProcessor();
            ZipImportProcessor.ImportData importData = processor.processZipFile(zipFile);

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("excelFileName", importData.getExcelFileName());
            debugInfo.put("totalFiles", importData.getAllFilePaths().size());
            debugInfo.put("allPaths", importData.getAllFilePaths());
            debugInfo.put("hasExcel", importData.getWorkbook() != null);

            // Tìm các file thumbnail
            List<String> thumbnailPaths = importData.getAllFilePaths().stream()
                    .filter(path -> path.toLowerCase().contains("thumbnail"))
                    .collect(Collectors.toList());

            debugInfo.put("thumbnailPaths", thumbnailPaths);
            debugInfo.put("hasThumbnails", !thumbnailPaths.isEmpty());

            log.info("Phân tích ZIP thành công: {} file, có Excel: {}, có thumbnails: {}",
                    importData.getAllFilePaths().size(),
                    importData.getWorkbook() != null,
                    !thumbnailPaths.isEmpty());

            return ResponseEntity.ok(new ApiResponse(true, "Phân tích ZIP thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi phân tích cấu trúc ZIP: ", e);

            // Tạo phản hồi lỗi với thông tin chi tiết
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .error("ZIP Analysis Error")
                    .message("Lỗi khi phân tích ZIP: " + e.getMessage())
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Xử lý lỗi 404 - Endpoint không tồn tại
     */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(org.springframework.web.servlet.NoHandlerFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("Không tìm thấy endpoint: " + ex.getRequestURL())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Xử lý lỗi chung
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Lỗi không xử lý được: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Đã xảy ra lỗi: " + ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}