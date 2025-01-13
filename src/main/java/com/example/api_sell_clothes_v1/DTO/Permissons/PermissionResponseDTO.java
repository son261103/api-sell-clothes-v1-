package com.example.api_sell_clothes_v1.DTO.Permissons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponseDTO {
    private Long permissionId;
    private String name;
    private String codeName;
    private String description;
    private String groupName;
    private LocalDateTime createdAt;
}
