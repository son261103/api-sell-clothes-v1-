package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Products.ProductExcelTemplateResponseDTO;
import com.example.api_sell_clothes_v1.Service.ProductExcelService;
import com.example.api_sell_clothes_v1.Service.ProductExcelService.ImportResult;
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

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCTS + "/excel")
@RequiredArgsConstructor
public class ProductExcelController {

    private final ProductExcelService productExcelService;

    /**
     * Tải về template Excel
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
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<?> importProducts(@RequestParam("file") MultipartFile zipFile) {
        try {
            ImportResult result = productExcelService.importProductsFromExcel(zipFile);

            // Nếu có lỗi, trả về file Excel chứa báo cáo lỗi
            if (!result.isSuccess() && result.getErrorReportBytes() != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "error_report.xlsx");
                headers.add("X-Error-Message", result.getMessage());

                return new ResponseEntity<>(result.getErrorReportBytes(), headers, HttpStatus.BAD_REQUEST);
            }

            // Nếu thành công, trả về thông báo kết quả
            return ResponseEntity.ok(new ApiResponse(result.isSuccess(), result.getMessage()));

        } catch (Exception e) {
            log.error("Lỗi khi import sản phẩm từ Excel: ", e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Lỗi khi import sản phẩm: " + e.getMessage()));
        }
    }

    /**
     * API kiểm tra trạng thái tiến trình nhập sản phẩm
     */
    @GetMapping("/import/status")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ApiResponse> getImportStatus() {
        // Có thể triển khai hệ thống theo dõi tiến trình nhập sản phẩm ở đây
        return ResponseEntity.ok(new ApiResponse(true, "Không có tiến trình nhập sản phẩm nào đang chạy"));
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
}