package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(ApiPatternConstants.API_ROLES)
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_ROLE')")
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        List<RoleResponseDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_ROLE')")
    public ResponseEntity<RoleResponseDTO> getRoleById(@PathVariable Long id) {
        RoleResponseDTO role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_ROLE')")
    public ResponseEntity<RoleResponseDTO> createRole(@RequestBody RoleCreateDTO createDTO) {
        RoleResponseDTO createdRole = roleService.createRole(createDTO);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('EDIT_ROLE')")
    public ResponseEntity<RoleResponseDTO> updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateDTO updateDTO) {
        RoleResponseDTO updatedRole = roleService.updateRole(id, updateDTO);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_ROLE')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-exists")
    @PreAuthorize("hasAuthority('VIEW_ROLE')")
    public ResponseEntity<Boolean> checkRoleExists(@RequestParam String name) {
        boolean exists = roleService.existsByName(name);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAuthority('VIEW_ROLE')")
    public ResponseEntity<RoleResponseDTO> getRoleByName(@PathVariable String name) {
        RoleResponseDTO role = roleService.getRoleByName(name);
        return ResponseEntity.ok(role);
    }

    /// /////////////////////////// /////////////////////////// /////////////////////////// /////////////////////////// ///////////////////////////

    /**
     * Add a permission to role
     */
    @PostMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('EDIT_ROLE')")
    public ResponseEntity<RoleResponseDTO> addPermissionToRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        try {
            RoleResponseDTO updatedRole = roleService.addPermissionToRole(roleId, permissionId);
            return ResponseEntity.ok(updatedRole);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error adding permission to role: " + e.getMessage());
        }
    }

    /**
     * Remove a permission from role
     */
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('EDIT_ROLE')")
    public ResponseEntity<RoleResponseDTO> removePermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        try {
            RoleResponseDTO updatedRole = roleService.removePermissionFromRole(roleId, permissionId);
            return ResponseEntity.ok(updatedRole);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error removing permission from role: " + e.getMessage());
        }
    }

    /**
     * Update all permissions for a role
     */
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('EDIT_ROLE')")
    public ResponseEntity<RoleResponseDTO> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        try {
            RoleResponseDTO updatedRole = roleService.updateRolePermissions(roleId, permissionIds);
            return ResponseEntity.ok(updatedRole);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error updating role permissions: " + e.getMessage());
        }
    }

    /**
     * Remove multiple permissions from role
     */
    @DeleteMapping("/{roleId}/permissions/bulk")
    @PreAuthorize("hasAuthority('EDIT_ROLE')")
    public ResponseEntity<RoleResponseDTO> removeMultiplePermissionsFromRole(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        try {
            RoleResponseDTO updatedRole = roleService.removeMultiplePermissionsFromRole(roleId, permissionIds);
            return ResponseEntity.ok(updatedRole);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error removing permissions from role: " + e.getMessage());
        }
    }

    /**
     * Get all permissions for a role
     */
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('VIEW_ROLE')")
    public ResponseEntity<Set<Permissions>> getRolePermissions(@PathVariable Long roleId) {
        try {
            Set<Permissions> permissions = roleService.getRolePermissions(roleId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error getting role permissions: " + e.getMessage());
        }
    }

}