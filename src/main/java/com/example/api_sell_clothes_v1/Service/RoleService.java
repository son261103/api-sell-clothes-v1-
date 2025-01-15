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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> getAllRoles() {
        List<Roles> roles = roleRepository.findAll();
        return roleMapper.toDto(roles);
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
}