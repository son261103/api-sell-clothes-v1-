package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Mapper.PermissionMapper;
import com.example.api_sell_clothes_v1.Mapper.RoleMapper;
import com.example.api_sell_clothes_v1.Repository.PermissionRepository;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final RoleService roleService;
    private final RoleMapper roleMapper;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> getAllPermissions() {
        List<Permissions> permissions = permissionRepository.findAll();
        return permissionMapper.toDto(permissions);
    }

    @Transactional(readOnly = true)
    public PermissionResponseDTO getPermissionById(Long id) {
        Permissions permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + id));
        return permissionMapper.toDto(permission);
    }


    @Transactional
    public PermissionResponseDTO createPermission(PermissionCreateDTO createDTO) {
        Permissions permission = Permissions.builder()
                .name(createDTO.getName())
                .codeName(createDTO.getCodeName())
                .description(createDTO.getDescription())
                .groupName(createDTO.getGroupName())
                .build();

        Permissions savedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(savedPermission);
    }

    @Transactional
    public PermissionResponseDTO updatePermission(Long id, PermissionUpdateDTO updateDTO) {
        Permissions permission = permissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + id));

        permissionMapper.updateEntityFromDTO(updateDTO, permission);
        Permissions updatedPermission = permissionRepository.save(permission);
        return permissionMapper.toDto(updatedPermission);
    }

    @Transactional
    public void deletePermission(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new EntityNotFoundException("Permission not found with id: " + id);
        }
        permissionRepository.deleteById(id);
    }

    @Transactional
    public RoleResponseDTO addPermissionToRole(Long roleId, Long permissionId) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

        Permissions permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId));

        role.getPermissions().add(permission);
        Roles updatedRole = roleRepository.save(role);
        return roleMapper.toDto(updatedRole);
    }

    @Transactional
    public RoleResponseDTO updateRolePermissions(Long roleId, Set<Long> permissionIds) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

        Set<Permissions> permissions = new HashSet<>();
        for (Long permissionId : permissionIds) {
            Permissions permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId));
            permissions.add(permission);
        }

        role.setPermissions(permissions);
        Roles updatedRole = roleRepository.save(role);
        return roleMapper.toDto(updatedRole);
    }

    @Transactional
    public RoleResponseDTO removePermissionFromRole(Long roleId, Long permissionId) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));

        Permissions permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId));

        role.getPermissions().remove(permission);
        Roles updatedRole = roleRepository.save(role);
        return roleMapper.toDto(updatedRole);
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return permissionRepository.existsByName(name);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> getPermissionsByGroupName(String groupName) {
        List<Permissions> permissions = permissionRepository.findByGroupName(groupName);
        return permissionMapper.toDto(permissions);
    }
}