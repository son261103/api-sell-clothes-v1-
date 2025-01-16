package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Types.PermissionType;
import com.example.api_sell_clothes_v1.Enums.Types.RoleType;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Repository.PermissionRepository;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitializationService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;

    @PostConstruct
    public void init() {
        try {
            createDefaultPermissions();
            createDefaultRoles();
            assignPermissionsToRoles();
            createDefaultAdmin();
        } catch (Exception e) {
            log.error("Error during initialization: ", e);
        }
    }

    @Transactional
    protected void createDefaultPermissions() {
        try {
            // Create permissions from PermissionType enum if they don't exist
            for (PermissionType type : PermissionType.values()) {
                Permissions permission = permissionRepository.findByCodeName(type.getCodeName())
                        .orElseGet(() -> {
                            // Create new permission if not exists
                            Permissions newPermission = new Permissions();
                            newPermission.setCodeName(type.getCodeName());
                            newPermission.setName(type.getName());
                            newPermission.setDescription(type.getDescription());
                            newPermission.setGroupName(type.getGroupName());
                            newPermission.setCreatedAt(LocalDateTime.now());
                            log.info("Created new permission: {}", type.getCodeName());
                            return permissionRepository.save(newPermission);
                        });
            }

            // Get all permissions after creation
            List<Permissions> existingPermissions = permissionRepository.findAll();

            // Get admin role
            Optional<Roles> adminRoleOpt = roleRepository.findByName(RoleType.ROLE_ADMIN.getCode());
            Roles adminRole = adminRoleOpt.get();

            // Initialize permissions set if null
            if (adminRole.getPermissions() == null) {
                adminRole.setPermissions(new HashSet<>());
            }

            // Assign permissions to admin role if not already assigned
            for (Permissions permission : existingPermissions) {
                if (!adminRole.getPermissions().contains(permission)) {
                    adminRole.getPermissions().add(permission);
                    log.info("Assigned permission {} to admin role", permission.getCodeName());
                } else {
                    log.debug("Permission {} is already assigned to admin role", permission.getCodeName());
                }
            }

            // Save admin role with updated permissions
            roleRepository.save(adminRole);
            log.info("Successfully updated admin role permissions");

        } catch (Exception e) {
            log.error("Error while creating and assigning permissions: ", e);
            throw e;
        }
    }


    @Transactional
    protected void createDefaultRoles() {
        try {
            if (!roleRepository.existsByName(RoleType.ROLE_ADMIN.getCode())) {
                Roles adminRole = new Roles();
                adminRole.setName(RoleType.ROLE_ADMIN.getCode());
                adminRole.setDescription(RoleType.ROLE_ADMIN.getDescription());
                adminRole.setCreatedAt(LocalDateTime.now());
                adminRole.setUpdatedAt(LocalDateTime.now());
                roleRepository.save(adminRole);
                log.info("Created admin role successfully");
            }

            if (!roleRepository.existsByName(RoleType.ROLE_CUSTOMER.getCode())) {
                Roles customerRole = new Roles();
                customerRole.setName(RoleType.ROLE_CUSTOMER.getCode());
                customerRole.setDescription(RoleType.ROLE_CUSTOMER.getDescription());
                customerRole.setCreatedAt(LocalDateTime.now());
                customerRole.setUpdatedAt(LocalDateTime.now());
                roleRepository.save(customerRole);
                log.info("Created customer role successfully");
            }
        } catch (Exception e) {
            log.error("Error creating default roles: ", e);
            throw e;
        }
    }

    @Transactional
    protected void assignPermissionsToRoles() {
        try {
            // Get admin role
            Optional<Roles> adminRoleOpt = roleRepository.findByName(RoleType.ROLE_ADMIN.getCode());
            if (adminRoleOpt.isPresent()) {
                Roles adminRole = adminRoleOpt.get();
                // Get all permissions
                List<Permissions> allPermissions = permissionRepository.findAll();
                adminRole.setPermissions(new HashSet<>(allPermissions));
                roleRepository.save(adminRole);
                log.info("Assigned all permissions to admin role");
            }

            // Get customer role - assign specific permissions if needed
            Optional<Roles> customerRoleOpt = roleRepository.findByName(RoleType.ROLE_CUSTOMER.getCode());
            if (customerRoleOpt.isPresent()) {
                // Add customer-specific permissions here if needed
                log.info("Customer role permissions set");
            }
        } catch (Exception e) {
            log.error("Error assigning permissions to roles: ", e);
            throw e;
        }
    }

    @Transactional
    protected void createDefaultAdmin() {
        try {
            if (!userRepository.existsByUsername("admin")) {
                // Get admin role
                Roles adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN.getCode())
                        .orElseThrow(() -> new RuntimeException("Admin role not found"));

                // Create admin user
                Users admin = Users.builder()
                        .username("admin")
                        .email("admin@system.com")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .fullName("System Administrator")
                        .status(UserStatus.ACTIVE)
                        .roles(Set.of(adminRole))
                        .createdAt(LocalDateTime.now())
                        .build();

                userRepository.save(admin);
                log.info("Created admin user successfully");
            }
        } catch (Exception e) {
            log.error("Error creating default admin: ", e);
            throw e;
        }
    }
}