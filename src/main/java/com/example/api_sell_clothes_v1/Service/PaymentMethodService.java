package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentMethodDTO;
import com.example.api_sell_clothes_v1.Entity.PaymentMethod;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.PaymentMethodMapper;
import com.example.api_sell_clothes_v1.Repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    @Transactional(readOnly = true)
    public List<PaymentMethodDTO> getAllPaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findAll();
        return paymentMethodMapper.toDto(paymentMethods);
    }

    @Transactional(readOnly = true)
    public Page<PaymentMethodDTO> getPaymentMethodsPaginated(Pageable pageable) {
        Page<PaymentMethod> paymentMethods = paymentMethodRepository.findAll(pageable);
        return paymentMethods.map(paymentMethodMapper::toDto);
    }

    @Transactional(readOnly = true)
    public PaymentMethodDTO getPaymentMethodById(Long methodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", methodId));
        return paymentMethodMapper.toDto(paymentMethod);
    }

    @Transactional(readOnly = true)
    public PaymentMethodDTO getPaymentMethodByCode(String code) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "code", code));
        return paymentMethodMapper.toDto(paymentMethod);
    }

    @Transactional
    public PaymentMethodDTO createPaymentMethod(PaymentMethodDTO dto) {
        PaymentMethod paymentMethod = paymentMethodMapper.toEntity(dto);
        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        return paymentMethodMapper.toDto(savedPaymentMethod);
    }

    @Transactional
    public PaymentMethodDTO updatePaymentMethod(Long methodId, PaymentMethodDTO dto) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", methodId));
        paymentMethod.setName(dto.getName());
        paymentMethod.setCode(dto.getCode());
        paymentMethod.setDescription(dto.getDescription());
        paymentMethod.setStatus(dto.getStatus());
        PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        return paymentMethodMapper.toDto(updatedPaymentMethod);
    }

    @Transactional
    public ApiResponse deletePaymentMethod(Long methodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", methodId));
        paymentMethodRepository.delete(paymentMethod);
        return new ApiResponse(true, "Payment method deleted successfully");
    }
}