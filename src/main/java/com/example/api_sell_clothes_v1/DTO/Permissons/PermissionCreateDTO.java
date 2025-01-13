package com.example.api_sell_clothes_v1.DTO.Permissons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionCreateDTO {
    private String name;
    private String codeName;
    private String description;
    private String groupName;
}
