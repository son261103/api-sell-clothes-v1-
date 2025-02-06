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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping(ApiPatternConstants.API_USERS)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // Basic CRUD operations without file upload remain unchanged
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Create new user with optional avatar
     * Accepts multipart form data with JSON string for user data
     */
    @PostMapping(value = "/create", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('CREATE_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> createUser(
            @RequestParam("user") String userCreateDTOString,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile) {
        try {
            UserCreateDTO createDTO = objectMapper.readValue(userCreateDTOString, UserCreateDTO.class);
            UserResponseDTO createdUser = userService.createUser(createDTO, avatarFile);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi xử lý dữ liệu người dùng: " + e.getMessage());
        }
    }

    /**
     * Update user with optional avatar
     * Accepts multipart form data with JSON string for user data
     */
    @PutMapping(value = "/edit/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('EDIT_CUSTOMER')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @RequestParam("user") String userUpdateDTOString,
            @RequestParam(value = "avatar", required = false) MultipartFile avatarFile) {
        try {
            UserUpdateDTO updateDTO = objectMapper.readValue(userUpdateDTOString, UserUpdateDTO.class);
            UserResponseDTO updatedUser = userService.updateUser(id, updateDTO, avatarFile);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            throw new IllegalArgumentException("Lỗi khi xử lý dữ liệu cập nhật: " + e.getMessage());
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