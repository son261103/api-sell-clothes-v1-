package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentMethodDTO;
import com.example.api_sell_clothes_v1.Entity.PaymentMethod;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.PaymentMethodMapper;
import com.example.api_sell_clothes_v1.Repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodService {
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    /**
     * Lấy tất cả các phương thức thanh toán
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodDTO> getAllPaymentMethods() {
        List<PaymentMethod> methods = paymentMethodRepository.findAll();
        return paymentMethodMapper.toDto(methods);
    }

    /**
     * Lấy tất cả các phương thức thanh toán đang hoạt động
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodDTO> getActivePaymentMethods() {
        List<PaymentMethod> methods = paymentMethodRepository.findByStatus(true);
        return paymentMethodMapper.toDto(methods);
    }

    /**
     * Lấy phương thức thanh toán theo ID
     */
    @Transactional(readOnly = true)
    public PaymentMethodDTO getPaymentMethodById(Long id) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));
        return paymentMethodMapper.toDto(method);
    }

    /**
     * Lấy các phương thức thanh toán phân trang
     */
    @Transactional(readOnly = true)
    public Page<PaymentMethodDTO> getPaymentMethodsPaginated(Pageable pageable) {
        Page<PaymentMethod> methods = paymentMethodRepository.findAll(pageable);
        return methods.map(paymentMethodMapper::toDto);
    }

    /**
     * Tạo phương thức thanh toán mới
     */
    @Transactional
    public PaymentMethodDTO createPaymentMethod(PaymentMethodDTO paymentMethodDTO) {
        // Kiểm tra mã code đã tồn tại chưa
        if (paymentMethodRepository.existsByCode(paymentMethodDTO.getCode())) {
            throw new IllegalArgumentException("Mã phương thức thanh toán đã tồn tại");
        }

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .name(paymentMethodDTO.getName())
                .code(paymentMethodDTO.getCode())
                .description(paymentMethodDTO.getDescription())
                .status(paymentMethodDTO.getStatus())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        return paymentMethodMapper.toDto(savedMethod);
    }

    /**
     * Cập nhật phương thức thanh toán
     */
    @Transactional
    public PaymentMethodDTO updatePaymentMethod(Long id, PaymentMethodDTO paymentMethodDTO) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));

        // Cập nhật thông tin
        paymentMethod.setName(paymentMethodDTO.getName());
        paymentMethod.setDescription(paymentMethodDTO.getDescription());
        paymentMethod.setStatus(paymentMethodDTO.getStatus());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod updatedMethod = paymentMethodRepository.save(paymentMethod);
        return paymentMethodMapper.toDto(updatedMethod);
    }

    /**
     * Xóa phương thức thanh toán
     */
    @Transactional
    public ApiResponse deletePaymentMethod(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));

        // Kiểm tra xem phương thức thanh toán đã được sử dụng chưa
        // (trong thực tế nên kiểm tra có liên kết với các đơn hàng không)

        paymentMethodRepository.delete(paymentMethod);
        return new ApiResponse(true, "Phương thức thanh toán đã được xóa thành công");
    }

    /**
     * Bật/tắt trạng thái phương thức thanh toán
     */
    @Transactional
    public PaymentMethodDTO togglePaymentMethodStatus(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));

        paymentMethod.setStatus(!paymentMethod.getStatus());
        paymentMethod.setUpdatedAt(LocalDateTime.now());

        PaymentMethod updatedMethod = paymentMethodRepository.save(paymentMethod);
        return paymentMethodMapper.toDto(updatedMethod);
    }

    /**
     * Kiểm tra phương thức thanh toán có đang hoạt động không
     */
    @Transactional(readOnly = true)
    public boolean isPaymentMethodActive(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));
        return paymentMethod.getStatus();
    }

    /**
     * Lấy danh sách ngân hàng hỗ trợ cho VNPay
     */
    @Transactional(readOnly = true)
    public List<String> getSupportedBanks(Long methodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", methodId));

        if (!"VNPAY".equalsIgnoreCase(paymentMethod.getCode())) {
            throw new IllegalArgumentException("Phương thức thanh toán này không hỗ trợ danh sách ngân hàng");
        }

        // Danh sách các ngân hàng hỗ trợ VNPay
        return Arrays.asList(
                "NCB", "VNPAYQR", "VIETCOMBANK", "VIETINBANK", "BIDV",
                "AGRIBANK", "SACOMBANK", "TECHCOMBANK", "MB", "ACB",
                "OCB", "IVB", "VPBANK", "TPB", "SHB", "VIB"
        );
    }
}