package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentMethodDTO;
import com.example.api_sell_clothes_v1.Service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PAYMENT_METHODS)
@RequiredArgsConstructor
public class PaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    /**
     * Lấy danh sách tất cả các phương thức thanh toán khả dụng (cho người dùng và admin)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PAYMENT_METHOD')")
    public ResponseEntity<List<PaymentMethodDTO>> getAllPaymentMethods() {
        try {
            List<PaymentMethodDTO> paymentMethods = paymentMethodService.getAllPaymentMethods();
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            log.error("Error fetching payment methods: {}", e.getMessage());
            throw new IllegalStateException("Lỗi khi lấy danh sách phương thức thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách phương thức thanh toán phân trang (cho admin)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_METHOD')")
    public ResponseEntity<Page<PaymentMethodDTO>> getPaymentMethodsPaginated(
            @PageableDefault(page = 0, size = 10, sort = "name") Pageable pageable) {
        try {
            Page<PaymentMethodDTO> paymentMethods = paymentMethodService.getPaymentMethodsPaginated(pageable);
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            log.error("Error fetching paginated payment methods: {}", e.getMessage());
            throw new IllegalStateException("Lỗi khi lấy danh sách phân trang phương thức thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết phương thức thanh toán theo ID (cho admin)
     */
    @GetMapping("/admin/{methodId}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_METHOD')")
    public ResponseEntity<PaymentMethodDTO> getPaymentMethodById(@PathVariable Long methodId) {
        try {
            PaymentMethodDTO paymentMethod = paymentMethodService.getPaymentMethodById(methodId);
            return ResponseEntity.ok(paymentMethod);
        } catch (Exception e) {
            log.error("Error fetching payment method by ID {}: {}", methodId, e.getMessage());
            throw new IllegalStateException("Lỗi khi lấy thông tin phương thức thanh toán: " + e.getMessage());
        }
    }

    /**
     * Tạo mới một phương thức thanh toán (cho admin)
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_METHOD')")
    public ResponseEntity<PaymentMethodDTO> createPaymentMethod(
            @Valid @RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            PaymentMethodDTO createdMethod = paymentMethodService.createPaymentMethod(paymentMethodDTO);
            return new ResponseEntity<>(createdMethod, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating payment method: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi tạo phương thức thanh toán: " + e.getMessage());
        }
    }

    /**
     * Cập nhật thông tin một phương thức thanh toán (cho admin)
     */
    @PutMapping("/admin/{methodId}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_METHOD')")
    public ResponseEntity<PaymentMethodDTO> updatePaymentMethod(
            @PathVariable Long methodId,
            @Valid @RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            PaymentMethodDTO updatedMethod = paymentMethodService.updatePaymentMethod(methodId, paymentMethodDTO);
            return ResponseEntity.ok(updatedMethod);
        } catch (Exception e) {
            log.error("Error updating payment method ID {}: {}", methodId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật phương thức thanh toán: " + e.getMessage());
        }
    }

    /**
     * Xóa một phương thức thanh toán (cho admin)
     */
    @DeleteMapping("/admin/{methodId}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_METHOD')")
    public ResponseEntity<ApiResponse> deletePaymentMethod(@PathVariable Long methodId) {
        try {
            ApiResponse response = paymentMethodService.deletePaymentMethod(methodId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting payment method ID {}: {}", methodId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi xóa phương thức thanh toán: " + e.getMessage());
        }
    }
}