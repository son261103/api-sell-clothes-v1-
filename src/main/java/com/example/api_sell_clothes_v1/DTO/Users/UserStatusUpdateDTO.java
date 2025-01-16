package com.example.api_sell_clothes_v1.DTO.Users;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateDTO {
    @Min(value = 1, message = "Mã trạng thái phải từ 1-4")
    @Max(value = 4, message = "Mã trạng thái phải từ 1-4")
    private String status;
}