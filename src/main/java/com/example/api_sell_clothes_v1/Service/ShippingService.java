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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {
    private final ShippingRepository shippingMethodRepository;
    private final OrderRepository orderRepository;
    private final ShippingMapper shippingMapper;

    // Shipping fee constants
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    /**
     * Get all available shipping methods
     */
    @Transactional(readOnly = true)
    public List<ShippingMethodDTO> getAllShippingMethods() {
        List<ShippingMethod> methods = shippingMethodRepository.findAll();
        return shippingMapper.toDto(methods);
    }

    /**
     * Get shipping method by ID
     */
    @Transactional(readOnly = true)
    public ShippingMethodDTO getShippingMethodById(Long id) {
        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));
        return shippingMapper.toDto(method);
    }

    /**
     * Create new shipping method (admin only)
     */
    @Transactional
    public ShippingMethodDTO createShippingMethod(ShippingMethodCreateDTO createDTO) {
        // Verify method name is unique
        if (shippingMethodRepository.findByName(createDTO.getName()).isPresent()) {
            throw new IllegalArgumentException("Shipping method with name " + createDTO.getName() + " already exists");
        }

        ShippingMethod method = shippingMapper.toEntity(createDTO);
        ShippingMethod savedMethod = shippingMethodRepository.save(method);
        log.info("Created shipping method with ID: {}", savedMethod.getId());
        return shippingMapper.toDto(savedMethod);
    }

    /**
     * Update existing shipping method (admin only)
     */
    @Transactional
    public ShippingMethodDTO updateShippingMethod(Long id, ShippingMethodUpdateDTO updateDTO) {
        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping method not found with ID: " + id));

        // Check for unique name if changed
        if (updateDTO.getName() != null && !method.getName().equals(updateDTO.getName())) {
            if (shippingMethodRepository.findByName(updateDTO.getName()).isPresent()) {
                throw new IllegalArgumentException("Shipping method with name " + updateDTO.getName() + " already exists");
            }
        }

        // Update entity from DTO
        shippingMapper.updateEntityFromDTO(updateDTO, method);

        ShippingMethod updatedMethod = shippingMethodRepository.save(method);
        log.info("Updated shipping method with ID: {}", id);
        return shippingMapper.toDto(updatedMethod);
    }

    /**
     * Delete shipping method (admin only)
     */
    @Transactional
    public ApiResponse deleteShippingMethod(Long id) {
        if (!shippingMethodRepository.existsById(id)) {
            throw new ResourceNotFoundException("Shipping method not found with ID: " + id);
        }

        shippingMethodRepository.deleteById(id);
        log.info("Deleted shipping method with ID: {}", id);
        return new ApiResponse(true, "Shipping method deleted successfully");
    }

    /**
     * Calculate shipping fee based on order total and shipping method
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateShippingFee(BigDecimal orderTotal, Long shippingMethodId, Double totalWeight) {
        // Free shipping for orders above the threshold
        if (orderTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        // Get shipping method
        ShippingMethod method = shippingMethodRepository.findById(shippingMethodId)
                .orElse(getDefaultShippingMethod());

        // Calculate fee based on base fee and weight
        BigDecimal baseFee = method.getBaseFee();

        // Add extra fee based on weight if provided
        if (totalWeight != null && totalWeight > 0) {
            BigDecimal extraFee = method.getExtraFeePerKg().multiply(BigDecimal.valueOf(totalWeight));
            return baseFee.add(extraFee);
        }

        return baseFee;
    }

    /**
     * Estimate shipping for cart items
     */
    @Transactional(readOnly = true)
    public ShippingEstimateDTO estimateShipping(BigDecimal orderTotal, Long shippingMethodId, Double totalWeight) {
        ShippingMethod method = shippingMethodRepository.findById(shippingMethodId)
                .orElse(getDefaultShippingMethod());

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

    /**
     * Apply shipping method to order
     */
    @Transactional
    public Order applyShippingToOrder(Long orderId, Long shippingMethodId, Double totalWeight) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        BigDecimal shippingFee = calculateShippingFee(order.getTotalAmount(), shippingMethodId, totalWeight);

        // Update order with shipping fee
        order.setShippingFee(shippingFee);

        // Update total amount to include shipping
        BigDecimal newTotal = order.getTotalAmount().add(shippingFee);
        order.setTotalAmount(newTotal);

        // Update order
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    /**
     * Helper method to get default shipping method
     */
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