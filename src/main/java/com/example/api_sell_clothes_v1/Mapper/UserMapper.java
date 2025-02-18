package com.example.api_sell_clothes_v1.Mapper;


import com.example.api_sell_clothes_v1.DTO.Roles.RoleResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper implements EntityMapper<Users, UserResponseDTO> {

    private final RoleMapper roleMapper;

    @Override
    public Users toEntity(UserResponseDTO dto) {
        if (dto == null) {
            return null;
        }

        return Users.builder()
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .avatar(dto.getAvatar())
                .status(dto.getStatus())
                .address(dto.getAddress())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .build();
    }

    @Override
    public UserResponseDTO toDto(Users entity) {
        if (entity == null) {
            return null;
        }

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setUserId(entity.getUserId());
        userResponseDTO.setUsername(entity.getUsername());
        userResponseDTO.setEmail(entity.getEmail());
        userResponseDTO.setFullName(entity.getFullName());
        userResponseDTO.setPhone(entity.getPhone());
        userResponseDTO.setAvatar(entity.getAvatar());
        userResponseDTO.setStatus(entity.getStatus());
        userResponseDTO.setAddress(entity.getAddress());
        userResponseDTO.setDateOfBirth(entity.getDateOfBirth());
        userResponseDTO.setGender(entity.getGender());
        userResponseDTO.setLastLoginAt(entity.getLastLoginAt() != null ?
                entity.getLastLoginAt().toString() : null);

        // Convert roles to RoleResponseDTO
        if (entity.getRoles() != null) {
            Set<RoleResponseDTO> roleResponseDTOs = entity.getRoles().stream()
                    .map(roleMapper::toDto)
                    .collect(Collectors.toSet());
            userResponseDTO.setRoles(roleResponseDTOs);
        }

        return userResponseDTO;
    }

    @Override
    public List<Users> toEntity(List<UserResponseDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponseDTO> toDto(List<Users> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Additional methods for other DTOs
    public Users toEntity(UserCreateDTO createDTO) {
        if (createDTO == null) {
            return null;
        }

        return Users.builder()
                .username(createDTO.getUsername())
                .email(createDTO.getEmail())
                .passwordHash(createDTO.getPassword())
                .fullName(createDTO.getFullName())
                .phone(createDTO.getPhone())
                .avatar(createDTO.getAvatar())
                .status(createDTO.getStatus())
                .build();
    }

    public void updateEntityFromDTO(UserUpdateDTO updateDTO, Users user) {
        if (updateDTO == null || user == null) {
            return;
        }

        if (updateDTO.getUsername() != null) {
            user.setUsername(updateDTO.getUsername());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getFullName() != null) {
            user.setFullName(updateDTO.getFullName());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getAvatar() != null) {
            user.setAvatar(updateDTO.getAvatar());
        }
        if (updateDTO.getStatus() != null) {
            user.setStatus(updateDTO.getStatus());
        }
    }
}