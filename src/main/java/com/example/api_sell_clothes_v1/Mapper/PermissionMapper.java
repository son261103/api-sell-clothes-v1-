package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Permissions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionMapper implements EntityMapper<Permissions, PermissionResponseDTO> {
    @Override
    public Permissions toEntity(PermissionResponseDTO dto) {
        if (dto == null) {
            return null;
        }

        return Permissions.builder()
                .permissionId(dto.getPermissionId())
                .name(dto.getName())
                .codeName(dto.getCodeName())
                .description(dto.getDescription())
                .groupName(dto.getGroupName())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    @Override
    public PermissionResponseDTO toDto(Permissions entity) {
        if (entity == null) {
            return null;
        }

        return PermissionResponseDTO.builder()
                .permissionId(entity.getPermissionId())
                .name(entity.getName())
                .codeName(entity.getCodeName())
                .description(entity.getDescription())
                .groupName(entity.getGroupName())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public List<Permissions> toEntity(List<PermissionResponseDTO> Dto) {
        if (Dto == null) {
            return null;
        }

        return Dto.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionResponseDTO> toDto(List<Permissions> entity) {
        if (entity == null) {
            return null;
        }

        return entity.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    //    Thêm phương thức cho UpdateDTO
    public void updateEntityFromDTO(PermissionUpdateDTO updateDTO, Permissions permissions) {
        if (updateDTO == null || permissions == null) {
            return;
        }
        if (updateDTO.getName() != null) {
            permissions.setName(updateDTO.getName());
        }
        if (updateDTO.getCodeName() != null) {
            permissions.setCodeName(updateDTO.getCodeName());
        }
        if (updateDTO.getDescription() != null) {
            permissions.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getGroupName() != null) {
            permissions.setGroupName(updateDTO.getGroupName());
        }
    }


}
