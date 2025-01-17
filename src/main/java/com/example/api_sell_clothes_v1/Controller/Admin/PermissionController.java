package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Permissons.PermissionUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.Roles.RoleResponseDTO;
import com.example.api_sell_clothes_v1.Service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(ApiPatternConstants.API_PERMISSIONS)
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionService permissionService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_PERMISSION')")
    public ResponseEntity<List<PermissionResponseDTO>> getAllPermissions() {
        List<PermissionResponseDTO> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_PERMISSION')")
    public ResponseEntity<PermissionResponseDTO> getPermissionById(@PathVariable Long id) {
        PermissionResponseDTO permission = permissionService.getPermissionById(id);
        return ResponseEntity.ok(permission);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_PERMISSION')")
    public ResponseEntity<PermissionResponseDTO> createPermission(@RequestBody PermissionCreateDTO createDTO) {
        PermissionResponseDTO createdPermission = permissionService.createPermission(createDTO);
        return new ResponseEntity<>(createdPermission, HttpStatus.CREATED);
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('EDIT_PERMISSION')")
    public ResponseEntity<PermissionResponseDTO> updatePermission(
            @PathVariable Long id,
            @RequestBody PermissionUpdateDTO updateDTO) {
        PermissionResponseDTO updatedPermission = permissionService.updatePermission(id, updateDTO);
        return ResponseEntity.ok(updatedPermission);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_PERMISSION')")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('CREATE_PERMISSION') or hasAnyAuthority('CREATE_ROLE')")
    public ResponseEntity<RoleResponseDTO> addPermissionToRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        RoleResponseDTO updatedRole = permissionService.addPermissionToRole(roleId, permissionId);
        return ResponseEntity.ok(updatedRole);
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('EDIT_PERMISSION') or hasAnyAuthority('EDIT_ROLE')")
    public ResponseEntity<RoleResponseDTO> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody Set<Long> permissionIds) {
        RoleResponseDTO updatedRole = permissionService.updateRolePermissions(roleId, permissionIds);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('DELETE_PERMISSION') or hasAuthority('DELETE_ROLE') ")
    public ResponseEntity<RoleResponseDTO> removePermissionFromRole(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        RoleResponseDTO updatedRole = permissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.ok(updatedRole);
    }

    @GetMapping("/group/{groupName}")
    @PreAuthorize("hasAuthority('VIEW_PERMISSION')")
    public ResponseEntity<List<PermissionResponseDTO>> getPermissionsByGroup(@PathVariable String groupName) {
        List<PermissionResponseDTO> permissions = permissionService.getPermissionsByGroupName(groupName);
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/check-exists")
    @PreAuthorize("hasAuthority('VIEW_PERMISSION')")
    public ResponseEntity<Boolean> checkPermissionExists(@RequestParam String name) {
        boolean exists = permissionService.existsByName(name);
        return ResponseEntity.ok(exists);
    }
}