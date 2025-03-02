package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingEstimateDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.ShippingMethod;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.ShippingMapper;
import com.example.api_sell_clothes_v1.Repository.OrderRepository;
import com.example.api_sell_clothes_v1.Repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {
    private final ShippingRepository shippingMethodRepository;
    private final OrderRepository orderRepository;
    private final ShippingMapper shippingMapper;

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    @Transactional(readOnly = true)
    public List<ShippingMethodDTO> getAllShippingMethods() {
        List<ShippingMethod> methods = shippingMethodRepository.findAll();
        return shippingMapper.toDto(methods);
    }

    @Transactional(readOnly = true)
    public ShippingMethodDTO getShippingMethodById(Long id) {
        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        return shippingMapper.toDto(method);
    }

    @Transactional
    public ShippingMethodDTO createShippingMethod(ShippingMethodCreateDTO createDTO) {
        if (shippingMethodRepository.findByName(createDTO.getName()).isPresent()) {
            throw new IllegalArgumentException("Shipping method with name " + createDTO.getName() + " already exists");
        }

        ShippingMethod method = shippingMapper.toEntity(createDTO);
        ShippingMethod savedMethod = shippingMethodRepository.save(method);
        log.info("Created shipping method with ID: {}", savedMethod.getId());
        return shippingMapper.toDto(savedMethod);
    }

    @Transactional
    public ShippingMethodDTO updateShippingMethod(Long id, ShippingMethodUpdateDTO updateDTO) {
        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));

        if (updateDTO.getName() != null && !method.getName().equals(updateDTO.getName())) {
            if (shippingMethodRepository.findByName(updateDTO.getName()).isPresent()) {
                throw new IllegalArgumentException("Shipping method with name " + updateDTO.getName() + " already exists");
            }
        }

        shippingMapper.updateEntityFromDTO(updateDTO, method);

        ShippingMethod updatedMethod = shippingMethodRepository.save(method);
        log.info("Updated shipping method with ID: {}", id);
        return shippingMapper.toDto(updatedMethod);
    }

    @Transactional
    public ApiResponse deleteShippingMethod(Long id) {
        if (!shippingMethodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shipping method not found with ID: " + id);
        }

        shippingMethodRepository.deleteById(id);
        log.info("Deleted shipping method with ID: {}", id);
        return new ApiResponse(true, "Shipping method deleted successfully");
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateShippingFee(BigDecimal orderTotal, Long shippingMethodId, Double totalWeight) {

        ShippingMethod method = shippingMethodRepository.findById(shippingMethodId)
                .orElseGet(this::getDefaultShippingMethod);

        BigDecimal baseFee = method.getBaseFee() != null ? method.getBaseFee() : DEFAULT_SHIPPING_FEE;
        log.debug("Base fee for shipping_method_id {}: {}", shippingMethodId, baseFee);

        if (totalWeight != null && totalWeight > 0 && method.getExtraFeePerKg() != null) {
            BigDecimal extraFee = method.getExtraFeePerKg().multiply(BigDecimal.valueOf(totalWeight));
            BigDecimal totalFee = baseFee.add(extraFee);
            log.debug("Extra fee for weight {}: {}, total shipping fee: {}", totalWeight, extraFee, totalFee);
            return totalFee;
        }

        log.debug("Returning base fee for shipping_method_id {}: {}", shippingMethodId, baseFee);
        return baseFee;
    }

    @Transactional(readOnly = true)
    public ShippingEstimateDTO estimateShipping(BigDecimal orderTotal, Long shippingMethodId, Double totalWeight) {
        ShippingMethod method = shippingMethodRepository.findById(shippingMethodId)
                .orElseGet(this::getDefaultShippingMethod);

        BigDecimal shippingFee = calculateShippingFee(orderTotal, shippingMethodId, totalWeight);

        return ShippingEstimateDTO.builder()
                .methodName(method.getName())
                .methodId(method.getId())
                .shippingFee(shippingFee)
                .estimatedDeliveryTime(method.getEstimatedDeliveryTime())
                .freeShippingEligible(orderTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0)
                .freeShippingThreshold(FREE_SHIPPING_THRESHOLD)
                .build();
    }

    @Transactional
    public Order applyShippingToOrder(Long orderId, Long shippingMethodId, Double totalWeight) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        BigDecimal shippingFee = calculateShippingFee(order.getTotalAmount(), shippingMethodId, totalWeight);

        order.setShippingFee(shippingFee);
        BigDecimal newTotal = order.getTotalAmount().add(shippingFee);
        order.setTotalAmount(newTotal);
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    private ShippingMethod getDefaultShippingMethod() {
        return shippingMethodRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ShippingMethod defaultMethod = new ShippingMethod();
                    defaultMethod.setName("Standard Delivery");
                    defaultMethod.setBaseFee(DEFAULT_SHIPPING_FEE);
                    defaultMethod.setExtraFeePerKg(BigDecimal.valueOf(5000));
                    defaultMethod.setEstimatedDeliveryTime("3-5 business days");
                    return defaultMethod;
                });
    }
}