package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Users.UserCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import com.example.api_sell_clothes_v1.Mapper.UserMapper;
import com.example.api_sell_clothes_v1.Repository.RefreshTokenRepository;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAvatarService userAvatarService;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String DEFAULT_ROLE = "ROLE_CUSTOMER";

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return userMapper.toDto(user);
    }

    /**
     * Get all users with pagination and search
     */
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(Pageable pageable, String search, UserStatus status) {
        Page<Users> usersPage;

        if (search != null && !search.trim().isEmpty()) {
            if (status != null) {
                // Search with status filter
                usersPage = userRepository.findByStatusAndSearchCriteria(status, search.trim(), pageable);
            } else {
                // Search without status filter
                usersPage = userRepository.findBySearchCriteria(search.trim(), pageable);
            }
        } else {
            if (status != null) {
                // Only status filter
                usersPage = userRepository.findByStatus(status, pageable);
            } else {
                // No filters
                usersPage = userRepository.findAll(pageable);
            }
        }

        return usersPage.map(userMapper::toDto);
    }

    /**
     * Create new user with avatar and default role
     */
    @Transactional
    public UserResponseDTO createUser(UserCreateDTO createDTO, MultipartFile avatarFile) {
        // Validate unique constraints
        validateUniqueFields(createDTO.getUsername(), createDTO.getEmail());

        // Create user entity
        Users user = userMapper.toEntity(createDTO);
        user.setPasswordHash(passwordEncoder.encode(createDTO.getPassword()));
        user.setStatus(UserStatus.ACTIVE);  // Set PENDING by default for email verification

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);

        // Upload avatar if provided
        String avatarUrl = userAvatarService.uploadAvatar(avatarFile);
        user.setAvatar(avatarUrl);

        // Set default role
        Roles customerRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new EntityNotFoundException("Role ROLE_CUSTOMER not found"));
        user.setRoles(new HashSet<>(Set.of(customerRole)));

        Users savedUser = userRepository.save(user);
        log.info("Created new user with ID: {} and role: {}", savedUser.getUserId(), DEFAULT_ROLE);

        return userMapper.toDto(savedUser);
    }

    /**
     * Update user information and avatar
     */
    @Transactional
    public UserResponseDTO updateUser(Long userId, UserUpdateDTO updateDTO, MultipartFile avatarFile) {
        Users existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        // Validate status for update
//        validateUserStatus(existingUser);

        // Validate unique fields if changed
        validateUniqueFieldsForUpdate(updateDTO, existingUser);

        // Update avatar if provided
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String newAvatarUrl = userAvatarService.updateAvatar(avatarFile, existingUser.getAvatar());
                existingUser.setAvatar(newAvatarUrl);
            } catch (FileHandlingException e) {
                log.warn("Avatar update failed but continuing with user update: {}", e.getMessage());
            }
        }

        // Update user information
        updateUserFields(existingUser, updateDTO);

        Users updatedUser = userRepository.save(existingUser);
        log.info("Updated user with ID: {}", updatedUser.getUserId());

        return userMapper.toDto(updatedUser);
    }

    /**
     * Delete user and their avatar
     */
    @Transactional
    public ApiResponse deleteUser(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        try {
            // Xóa refresh tokens trước
            refreshTokenRepository.deleteByUserUserId(userId);

            // Xóa avatar nếu có
            try {
                userAvatarService.deleteAvatar(user.getAvatar());
            } catch (Exception e) {
                log.warn("Failed to delete avatar but continuing with user deletion: {}", e.getMessage());
            }

            // Cuối cùng mới xóa user
            userRepository.delete(user);
            log.info("Deleted user with ID: {}", userId);

            return new ApiResponse(true, "Xóa người dùng thành công");

        } catch (Exception e) {
            log.error("Failed to delete user: {}", e.getMessage());
            throw new RuntimeException("Không thể xóa người dùng: " + e.getMessage());
        }
    }


    //    Update user status
    @Transactional
    public ApiResponse updateUserStatus(Long userId, UserStatus newStatus) {
        try {
            // Kiểm tra user tồn tại
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));

            // Kiểm tra logic chuyển đổi trạng thái
            if (!isValidStatusTransition(user.getStatus(), newStatus)) {
                return new ApiResponse(false,
                        String.format("Unable to switch from state %s (%s) to %s (%s)",
                                user.getStatus().name(), user.getStatus().getDescription(),
                                newStatus.name(), newStatus.getDescription()));
            }

            // Cập nhật trạng thái
            user.setStatus(newStatus);
            userRepository.save(user);

            log.info("Updated status for user ID {} from {} to {}",
                    userId, user.getStatus(), newStatus);

            return new ApiResponse(true,
                    String.format("Update status to %s (%s) successfully",
                            newStatus.name(), newStatus.getDescription()));

        } catch (IllegalArgumentException e) {
            return new ApiResponse(false, "Invalid status: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error updating user status: {}", e.getMessage());
            return new ApiResponse(false, "Error updating status: " + e.getMessage());
        }
    }


    private boolean isValidStatusTransition(UserStatus currentStatus, UserStatus newStatus) {
        // Nếu trạng thái giống nhau
        if (currentStatus == newStatus) {
            return false;
        }

        // Định nghĩa luật chuyển đổi trạng thái
        switch (currentStatus) {
            case PENDING:
                // PENDING chỉ có thể -> ACTIVE hoặc BANNED
                return newStatus == UserStatus.ACTIVE || newStatus == UserStatus.BANNER;

            case ACTIVE:
                // ACTIVE -> LOCKED hoặc BANNED
                return newStatus == UserStatus.LOCKED || newStatus == UserStatus.BANNER;

            case LOCKED:
                // LOCKED -> ACTIVE hoặc BANNED
                return newStatus == UserStatus.ACTIVE || newStatus == UserStatus.BANNER;

            case BANNER:
                // BANNED không thể chuyển sang trạng thái khác
                return false;

            default:
                return false;
        }
    }

    /**
     * Update last login time
     */
    @Transactional
    public void updateLastLogin(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);

        userRepository.save(user);
        log.info("Updated last login time for user ID: {} at {}", userId, now);
    }

    /**
     * Find by username
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsername(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User with username not found: " + username));
        return userMapper.toDto(user);
    }

    /**
     * Find by email
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email not found: " + email));
        return userMapper.toDto(user);
    }

    /**
     * Check if username exists
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Private helper methods
    private void validateUniqueFields(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

//    private void validateUserStatus(Users user) {
//        if (user.getStatus() == UserStatus.LOCKED || user.getStatus() == UserStatus.BANNER) {
//            throw new IllegalArgumentException("Account has been locked or banned");
//        }
//    }

    private void validateUniqueFieldsForUpdate(UserUpdateDTO updateDTO, Users existingUser) {
        if (updateDTO.getUsername() != null &&
                !updateDTO.getUsername().trim().isEmpty() &&
                !updateDTO.getUsername().equals(existingUser.getUsername()) &&
                userRepository.existsByUsername(updateDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (updateDTO.getEmail() != null &&
                !updateDTO.getEmail().trim().isEmpty() &&
                !updateDTO.getEmail().equals(existingUser.getEmail()) &&
                userRepository.existsByEmail(updateDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private void updateUserFields(Users user, UserUpdateDTO updateDTO) {
        if (updateDTO.getUsername() != null && !updateDTO.getUsername().trim().isEmpty()) {
            user.setUsername(updateDTO.getUsername());
        }
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().trim().isEmpty()) {
            user.setEmail(updateDTO.getEmail());
        }
        if (updateDTO.getFullName() != null && !updateDTO.getFullName().trim().isEmpty()) {
            user.setFullName(updateDTO.getFullName());
        }
        if (updateDTO.getPhone() != null && !updateDTO.getPhone().trim().isEmpty()) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(updateDTO.getPassword()));
        }
        if (updateDTO.getStatus() != null) {
            user.setStatus(updateDTO.getStatus());
        }
    }
}