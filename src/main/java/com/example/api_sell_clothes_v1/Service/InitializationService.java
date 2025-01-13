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
            for (PermissionType permissionType : PermissionType.values()) {
                if (!permissionRepository.existsByCodeName(permissionType.getCode())) {
                    Permissions permission = Permissions.builder()
                            .name(permissionType.getName())
                            .codeName(permissionType.getCode())
                            .description(permissionType.getDescription())
                            .groupName(permissionType.getGroupName())
                            .createdAt(LocalDateTime.now())
                            .build();
                    permissionRepository.save(permission);
                    log.info("Created permission {} successfully", permissionType.getCode());
                }
            }
        } catch (Exception e) {
            log.error("Error creating default permissions: ", e);
            throw e;
        }
    }

    @Transactional
    protected void createDefaultRoles() {
        try {
            if (!roleRepository.existsByName(RoleType.ROLE_ADMIN.name())) {
                Roles adminRole = new Roles();
                adminRole.setName(RoleType.ROLE_ADMIN.name());
                adminRole.setDescription("Administrator role");
                adminRole.setCreatedAt(LocalDateTime.now());
                adminRole.setUpdatedAt(LocalDateTime.now());
                roleRepository.save(adminRole);
                log.info("Created admin role successfully");
            }

            if (!roleRepository.existsByName(RoleType.ROLE_CUSTOMER.name())) {
                Roles customerRole = new Roles();
                customerRole.setName(RoleType.ROLE_CUSTOMER.name());
                customerRole.setDescription("Customer role");
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
            Optional<Roles> adminRoleOpt = roleRepository.findByName(RoleType.ROLE_ADMIN.name());
            if (adminRoleOpt.isPresent()) {
                Roles adminRole = adminRoleOpt.get();
                // Get all permissions
                List<Permissions> allPermissions = permissionRepository.findAll();
                adminRole.setPermissions(new HashSet<>(allPermissions));
                roleRepository.save(adminRole);
                log.info("Assigned all permissions to admin role");
            }

            // Get customer role - assign specific permissions if needed
            Optional<Roles> customerRoleOpt = roleRepository.findByName(RoleType.ROLE_CUSTOMER.name());
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
                Roles adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN.name())
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