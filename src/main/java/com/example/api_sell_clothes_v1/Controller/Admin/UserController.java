package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Users.UserCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserStatusUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserUpdateDTO;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping(ApiPatternConstants.API_USERS)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // Basic CRUD operations
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @PageableDefault(page = 0, size = 10, sort = "userId") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status) {
        Page<UserResponseDTO> users = userService.getAllUsers(pageable, search, status);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Create new user
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        try {
            UserResponseDTO createdUser = userService.createUser(createDTO);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi tạo người dùng: " + e.getMessage());
        }
    }

    /**
     * Update user information
     */
    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAuthority('EDIT_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO updateDTO) {
        try {
            UserResponseDTO updatedUser = userService.updateUser(id, updateDTO);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi cập nhật người dùng: " + e.getMessage());
        }
    }

    /**
     * Upload new avatar
     */
    @PostMapping(value = "/avatar/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("avatar") MultipartFile avatarFile) {
        try {
            UserResponseDTO response = userService.uploadAvatar(userId, avatarFile);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi tải lên ảnh đại diện: " + e.getMessage());
        }
    }

    /**
     * Update existing avatar
     */
    @PutMapping(value = "/avatar/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> updateAvatar(
            @PathVariable Long userId,
            @RequestParam("avatar") MultipartFile avatarFile) {
        try {
            UserResponseDTO response = userService.updateAvatar(userId, avatarFile);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi cập nhật ảnh đại diện: " + e.getMessage());
        }
    }

    /**
     * Delete avatar
     */
    @DeleteMapping("/avatar/{userId}")
    @PreAuthorize("hasAuthority('EDIT_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> deleteAvatar(@PathVariable Long userId) {
        try {
            UserResponseDTO response = userService.deleteAvatar(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi xóa ảnh đại diện: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_CUSTOMER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EDIT_CUSTOMER')")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateDTO statusUpdateDTO) {
        ApiResponse response = userService.updateUserStatus(id, UserStatus.valueOf(statusUpdateDTO.getStatus().toUpperCase()));
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    // Các endpoints khác giữ nguyên
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        UserResponseDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/last-login")
    public ResponseEntity<ApiResponse> updateLastLogin(@PathVariable Long userId) {
        try {
            userService.updateLastLogin(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Update successful login time"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error updating login time: " + e.getMessage()));
        }
    }

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse> checkUsername(@RequestParam String username) {
        boolean exists = userService.existsByUsername(username);
        String message = exists ? "Username already exists" : "Username can be used";
        return ResponseEntity.ok(new ApiResponse(!exists, message));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse> checkEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        String message = exists ? "Email already exists" : "Email available";
        return ResponseEntity.ok(new ApiResponse(!exists, message));
    }
}