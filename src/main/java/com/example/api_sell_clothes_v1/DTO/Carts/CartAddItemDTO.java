package com.example.api_sell_clothes_v1.DTO.Carts;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartAddItemDTO {
    @NotNull(message = "Variant ID không được để trống")
    private Long variantId;

    @NotNull(message = "Quantity không được để trống")
    @Min(value = 1, message = "Quantity phải lớn hơn 0")
    private Integer quantity;
}