package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Roles.RoleCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Mapper.RoleMapper;
import com.example.api_sell_clothes_v1.Repository.PermissionRepository;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public Page<RoleResponseDTO> getAllRoles(Pageable pageable, String search) {
        Page<Roles> rolesPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search by name or description
            rolesPage = roleRepository.findBySearchCriteria(search.trim(), pageable);
        } else {
            // No search filter
            rolesPage = roleRepository.findAll(pageable);
        }

        return rolesPage.map(roleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RoleResponseDTO getRoleById(Long id) {
        Roles role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));
        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleResponseDTO createRole(RoleCreateDTO createDTO) {
        // Validate role name uniqueness
        String roleName = ensureRolePrefixAndUpperCase(createDTO.getName());
        if (roleRepository.existsByName(roleName)) {
            throw new IllegalArgumentException("Role name already exists: " + roleName);
        }

        Roles role = Roles.builder()
                .name(roleName)
                .description(createDTO.getDescription())
                .permissions(new HashSet<>())
                .build();

        Roles savedRole = roleRepository.save(role);
        return roleMapper.toDto(savedRole);
    }

    @Transactional
    public RoleResponseDTO updateRole(Long id, RoleUpdateDTO updateDTO) {
        Roles role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + id));

        // Kiểm tra và cập nhật `name` nếu có trong `updateDTO`
        if (updateDTO.getName() != null) {
            String updatedRoleName = ensureRolePrefixAndUpperCase(updateDTO.getName());

            // Check name uniqueness only if name is being changed
            if (!role.getName().equals(updatedRoleName) &&
                    roleRepository.existsByName(updatedRoleName)) {
                throw new IllegalArgumentException("Role name already exists: " + updatedRoleName);
            }
            role.setName(updatedRoleName);
        }

        // Kiểm tra và cập nhật `description` nếu có trong `updateDTO`
        if (updateDTO.getDescription() != null) {
            role.setDescription(updateDTO.getDescription());
        }

        Roles updatedRole = roleRepository.save(role);
        return roleMapper.toDto(updatedRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new EntityNotFoundException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }


    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    @Transactional(readOnly = true)
    public RoleResponseDTO getRoleByName(String name) {
        Roles role = roleRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Role not found with name: " + name));
        return roleMapper.toDto(role);
    }

    /**
     * Đảm bảo tiền tố ROLE_ được thêm vào trước tên vai trò nếu chưa có.
     */
    private String ensureRolePrefixAndUpperCase(String roleName) {
        final String ROLE_PREFIX = "ROLE_";
        String normalizedRoleName = roleName.toUpperCase(); // Chuyển toàn bộ tên sang chữ hoa
        if (!normalizedRoleName.startsWith(ROLE_PREFIX)) {
            return ROLE_PREFIX + normalizedRoleName;
        }
        return normalizedRoleName;
    }

    /// /////////////////////////// /////////////////////////// /////////////////////////// /////////////////////////// ///////////////////////////

    /**
     * Add a permission to role
     */
    @Transactional
    public RoleResponseDTO addPermissionToRole(Long roleId, Long permissionId) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        Permissions permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found"));

        role.getPermissions().add(permission);
        Roles updatedRole = roleRepository.save(role);
        log.info("Added permission {} to role {}", permission.getName(), roleId);

        return roleMapper.toDto(updatedRole);
    }

    /**
     * Remove a permission from role
     */
    @Transactional
    public RoleResponseDTO removePermissionFromRole(Long roleId, Long permissionId) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        Permissions permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new EntityNotFoundException("Permission not found"));

        if (!role.getPermissions().contains(permission)) {
            throw new IllegalArgumentException("Role does not have this permission");
        }

        role.getPermissions().remove(permission);
        Roles updatedRole = roleRepository.save(role);
        log.info("Removed permission {} from role {}", permission.getName(), roleId);

        return roleMapper.toDto(updatedRole);
    }

    /**
     * Update all permissions for a role
     */
    @Transactional
    public RoleResponseDTO updateRolePermissions(Long roleId, Set<Long> permissionIds) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        Set<Permissions> newPermissions = new HashSet<>();
        for (Long permissionId : permissionIds) {
            Permissions permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId));
            newPermissions.add(permission);
        }

        role.setPermissions(newPermissions);
        Roles updatedRole = roleRepository.save(role);
        log.info("Updated permissions for role {}", roleId);

        return roleMapper.toDto(updatedRole);
    }

    /**
     * Remove multiple permissions from role
     */
    @Transactional
    public RoleResponseDTO removeMultiplePermissionsFromRole(Long roleId, Set<Long> permissionIds) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));

        Set<Permissions> permissionsToRemove = new HashSet<>();
        for (Long permissionId : permissionIds) {
            Permissions permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new EntityNotFoundException("Permission not found with id: " + permissionId));
            permissionsToRemove.add(permission);
        }

        role.getPermissions().removeAll(permissionsToRemove);
        Roles updatedRole = roleRepository.save(role);
        log.info("Removed {} permissions from role {}", permissionIds.size(), roleId);

        return roleMapper.toDto(updatedRole);
    }

    /**
     * Get all permissions for a role
     */
    @Transactional(readOnly = true)
    public Set<Permissions> getRolePermissions(Long roleId) {
        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        return role.getPermissions();
    }
}