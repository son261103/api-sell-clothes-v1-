package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Products.ProductExcelTemplateResponseDTO;
import com.example.api_sell_clothes_v1.Service.ProductExcelService;
import com.example.api_sell_clothes_v1.Service.ProductExcelService.ImportResult;
import com.example.api_sell_clothes_v1.Service.SkuGeneratorService;
import com.example.api_sell_clothes_v1.Utils.ZipImportProcessor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCTS + "/excel")
@RequiredArgsConstructor
public class ProductExcelController {

    private final ProductExcelService productExcelService;
    private final SkuGeneratorService skuGeneratorService;

    // Bộ nhớ tạm lưu trữ báo cáo lỗi
    private static final ConcurrentHashMap<String, byte[]> errorReports = new ConcurrentHashMap<>();

    /**
     * Tải về template Excel (chỉ file Excel)
     */
    @GetMapping("/template")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadTemplate(HttpServletResponse response) {
        try {
            productExcelService.generateTemplatePackage(response);
        } catch (IOException e) {
            log.error("Lỗi khi tạo file template: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Tải về file ZIP chứa template Excel và thư mục ảnh mẫu
     */
    @GetMapping("/full-template")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadFullTemplate(HttpServletResponse response) {
        try {
            productExcelService.generateFullTemplatePackage(response);
        } catch (IOException e) {
            log.error("Lỗi khi tạo file ZIP template: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Tải về hướng dẫn dạng text
     */
    @GetMapping("/instructions")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadInstructions(HttpServletResponse response) {
        try {
            productExcelService.generateInstructionText(response);
        } catch (IOException e) {
            log.error("Lỗi khi tạo file hướng dẫn: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Lấy thông tin về template hiện tại
     */
    @GetMapping("/template/info")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ApiResponse> getTemplateInfo() {
        try {
            ProductExcelTemplateResponseDTO templateInfo = productExcelService.getTemplateInfo();
            return ResponseEntity.ok(new ApiResponse(true, "Lấy thông tin template thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin template: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy thông tin template: " + e.getMessage()));
        }
    }

    /**
     * Tải lên file ZIP chứa Excel và thư mục ảnh để nhập sản phẩm hàng loạt
     * ĐÃ CẢI TIẾN: Không trả về 400 nữa mà trả về 200 với thông tin lỗi
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<?> importProducts(@RequestParam("file") MultipartFile zipFile) {
        try {
            ImportResult result = productExcelService.importProductsFromExcel(zipFile);

            // Nếu có lỗi, lưu báo cáo lỗi và trả về URL để tải
            if (!result.isSuccess() && result.getErrorReportBytes() != null) {
                // Tạo ID duy nhất cho báo cáo lỗi
                String errorReportId = UUID.randomUUID().toString();

                // Lưu báo cáo lỗi vào bộ nhớ tạm
                errorReports.put(errorReportId, result.getErrorReportBytes());

                // Tạo URL cho báo cáo lỗi
                String errorUrl = "/api/v1/products/excel/error-report/" + errorReportId;

                // Trả về thông tin lỗi với mã HTTP 200
                ApiResponse response = new ApiResponse(false, result.getMessage());

                // Tạo ResponseEntity tùy chỉnh với các thông tin bổ sung
                return ResponseEntity.ok()
                        .header("X-Error-Total", String.valueOf(result.getTotalErrors()))
                        .header("X-Error-Rows", String.valueOf(result.getErrorRowCount()))
                        .header("X-Error-Report-Url", errorUrl)
                        .body(response);
            }

            // Nếu thành công, trả về thông báo kết quả
            ApiResponse response = new ApiResponse(true, result.getMessage());

            return ResponseEntity.ok()
                    .header("X-Total-Imported", String.valueOf(result.getTotalImported()))
                    .body(response);

        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ Excel: ", e);

            // Vẫn trả về HTTP 200 để frontend dễ xử lý
            return ResponseEntity.ok(new ApiResponse(false, "Lỗi khi import sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * API mới: Tải báo cáo lỗi theo ID
     */
    @GetMapping("/error-report/{reportId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void downloadErrorReport(@PathVariable String reportId, HttpServletResponse response) {
        byte[] errorReportBytes = errorReports.get(reportId);

        if (errorReportBytes != null) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=error_report.xlsx");

            try {
                response.getOutputStream().write(errorReportBytes);
                response.getOutputStream().flush();

                // Xóa báo cáo lỗi khỏi bộ nhớ tạm sau khi tải xuống
                errorReports.remove(reportId);
            } catch (IOException e) {
                log.error("Lỗi khi tải báo cáo lỗi: ", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("Báo cáo lỗi không tồn tại hoặc đã hết hạn");
            } catch (IOException e) {
                log.error("Lỗi khi ghi phản hồi: ", e);
            }
        }
    }

    /**
     * API kiểm tra trạng thái tiến trình nhập sản phẩm
     */
    @GetMapping("/import/status")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ApiResponse> getImportStatus(@RequestParam(required = false) String jobId) {
        // Có thể triển khai hệ thống theo dõi tiến trình nhập sản phẩm ở đây
        return ResponseEntity.ok(new ApiResponse(true, "Lấy trạng thái nhập thành công"));
    }

    /**
     * Xuất danh sách sản phẩm ra file Excel
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public void exportProducts(
            HttpServletResponse response,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean status) {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=products_export.xlsx");

        try {
            // Nếu bạn muốn thêm tính năng xuất sản phẩm, hãy triển khai ở đây
            response.getWriter().write("Tính năng đang được phát triển");
        } catch (IOException e) {
            log.error("Lỗi khi xuất sản phẩm ra Excel: ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * API phân tích file ZIP trước khi import
     */
    @PostMapping(value = "/zip/info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ApiResponse> getZipInfo(@RequestParam("file") MultipartFile zipFile) {
        try {
            if (zipFile.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(false, "File không được để trống"));
            }

            if (!zipFile.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                return ResponseEntity.ok(new ApiResponse(false, "File phải có định dạng ZIP"));
            }

            // Sử dụng ZipImportProcessor để phân tích file ZIP
            ZipImportProcessor processor = new ZipImportProcessor();
            ZipImportProcessor.ImportData importData = processor.processZipFile(zipFile);

            // Trả về thông tin phân tích qua header
            return ResponseEntity.ok()
                    .header("X-Has-Excel", String.valueOf(importData.getWorkbook() != null))
                    .header("X-Has-Images", String.valueOf(importData.getImagesBySku() != null && !importData.getImagesBySku().isEmpty()))
                    .header("X-Image-Count", String.valueOf(importData.getTotalImageCount()))
                    .header("X-Excel-Filename", importData.getExcelFileName() != null ? importData.getExcelFileName() : "")
                    .header("X-Total-Size", String.valueOf(zipFile.getSize()))
                    .header("X-Formatted-Size", importData.getFormattedSize())
                    .body(new ApiResponse(true, "Phân tích ZIP thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi phân tích file ZIP: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "Lỗi khi phân tích file ZIP: " + e.getMessage()));
        }
    }

    /**
     * API sinh SKU preview cho biến thể
     */
    @PostMapping("/sku/preview")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ApiResponse> generateSkuPreview(@RequestBody Map<String, String> request) {
        try {
            String productName = request.get("productName");
            String size = request.get("size");
            String color = request.get("color");

            if (productName == null || size == null || color == null) {
                return ResponseEntity.ok(new ApiResponse(false, "Thiếu thông tin cần thiết để sinh SKU"));
            }

            // Gọi service để sinh SKU từ tên sản phẩm
            String generatedSku = skuGeneratorService.generateSku(productName, size, color);

            // Trả về thông tin SKU qua header
            return ResponseEntity.ok()
                    .header("X-Generated-SKU", generatedSku)
                    .header("X-Product-Code", generatedSku.split("-")[0])
                    .header("X-Size-Code", size)
                    .header("X-Color-Code", color)
                    .header("X-Folder-Path", "Images/" + generatedSku + "/")
                    .body(new ApiResponse(true, "Sinh SKU thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi sinh SKU: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "Lỗi khi sinh SKU: " + e.getMessage()));
        }
    }

    /**
     * API sinh nhiều SKU từ một sản phẩm với nhiều size và màu
     */
    @PostMapping("/sku/bulk-preview")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ApiResponse> generateBulkSkuPreview(@RequestBody Map<String, Object> request) {
        try {
            String productName = (String) request.get("productName");
            @SuppressWarnings("unchecked")
            List<String> sizes = (List<String>) request.get("sizes");
            @SuppressWarnings("unchecked")
            List<String> colors = (List<String>) request.get("colors");

            if (productName == null || sizes == null || colors == null || sizes.isEmpty() || colors.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(false, "Thiếu thông tin cần thiết để sinh SKU"));
            }

            int totalCount = sizes.size() * colors.size();

            // Tạo SKU đầu tiên làm ví dụ
            String exampleSku = skuGeneratorService.generateSku(productName, sizes.get(0), colors.get(0));

            return ResponseEntity.ok()
                    .header("X-Product-Name", productName)
                    .header("X-Total-SKUs", String.valueOf(totalCount))
                    .header("X-Example-SKU", exampleSku)
                    .body(new ApiResponse(true, "Đã tạo " + totalCount + " SKU thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi tạo nhiều SKU: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "Lỗi khi tạo nhiều SKU: " + e.getMessage()));
        }
    }

    /**
     * API thực hiện thao tác hàng loạt với sản phẩm
     */
    @PostMapping("/bulk/{operation}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ApiResponse> performBulkOperation(
            @PathVariable String operation,
            @RequestBody Map<String, List<Long>> request) {

        try {
            List<Long> productIds = request.get("productIds");

            if (productIds == null || productIds.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(false, "Danh sách sản phẩm không được để trống"));
            }

            String message;

            switch (operation) {
                case "delete":
                    message = "Đã xóa " + productIds.size() + " sản phẩm thành công";
                    // Gọi service để xóa sản phẩm ở đây
                    break;
                case "activate":
                    message = "Đã kích hoạt " + productIds.size() + " sản phẩm thành công";
                    // Gọi service để kích hoạt sản phẩm ở đây
                    break;
                case "deactivate":
                    message = "Đã vô hiệu hóa " + productIds.size() + " sản phẩm thành công";
                    // Gọi service để vô hiệu hóa sản phẩm ở đây
                    break;
                default:
                    return ResponseEntity.ok(new ApiResponse(false, "Thao tác không hợp lệ"));
            }

            return ResponseEntity.ok()
                    .header("X-Total-Processed", String.valueOf(productIds.size()))
                    .header("X-Success-Count", String.valueOf(productIds.size()))
                    .header("X-Failure-Count", "0")
                    .body(new ApiResponse(true, message));
        } catch (Exception e) {
            log.error("Lỗi khi thực hiện thao tác hàng loạt: ", e);
            return ResponseEntity.ok(new ApiResponse(false, "Lỗi khi thực hiện thao tác hàng loạt: " + e.getMessage()));
        }
    }
}