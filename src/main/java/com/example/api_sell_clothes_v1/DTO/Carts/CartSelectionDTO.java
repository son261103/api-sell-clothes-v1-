package com.example.api_sell_clothes_v1.DTO.Carts;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSelectionDTO {
    @NotNull(message = "isSelected không được để trống")
    private Boolean isSelected;
}
