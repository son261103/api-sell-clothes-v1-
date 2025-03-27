package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.TokenResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Entity.RefreshTokens;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Repository.RefreshTokenRepository;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Security.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthGlobalService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthorityService authorityService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * Authenticate with Google by verifying the ID token and either creating a new user or
     * logging in an existing user
     *
     * @param idTokenString The Google ID token to verify
     * @return TokenResponseDTO containing JWT tokens and user info
     */
    public TokenResponseDTO authenticateWithGoogle(String idTokenString) {
        log.info("Authenticating with Google ID token");

        try {
            // Verify the Google ID token
            GoogleIdToken idToken = verifyGoogleIdToken(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            // Extract user info from the token
            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");
            String sub = payload.getSubject(); // Google's unique user ID

            if (!emailVerified) {
                throw new RuntimeException("Email not verified by Google");
            }

            log.info("Google authentication successful for email: {}", email);

            // Find or create the user in our system
            Users user = userRepository.findByEmail(email)
                    .orElseGet(() -> createGoogleUser(email, name, pictureUrl, sub));

            // Update user information if needed
            updateGoogleUserIfNeeded(user, name, pictureUrl);

            // Create custom user details for JWT generation
            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            // Clean up old refresh tokens
            refreshTokenService.deleteAllUserTokens(user);

            // Generate tokens
            String accessToken = jwtService.generateToken(userDetails);
            RefreshTokens refreshToken = refreshTokenService.createRefreshToken(user);

            // Extract roles and permissions for the response
            Set<String> roles = user.getRoles().stream()
                    .map(Roles::getName)
                    .collect(Collectors.toSet());

            Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(Permissions::getCodeName)
                    .collect(Collectors.toSet());

            log.info("Tokens generated successfully for Google user: {}", email);

            // Return token response
            return TokenResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

        } catch (Exception e) {
            log.error("Error authenticating with Google: {}", e.getMessage(), e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * Verify the Google ID token using Google's API
     *
     * @param idTokenString The token to verify
     * @return The verified GoogleIdToken or null if invalid
     */
    private GoogleIdToken verifyGoogleIdToken(String idTokenString) throws GeneralSecurityException, IOException {
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        NetHttpTransport transport = new NetHttpTransport();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        return verifier.verify(idTokenString);
    }

    /**
     * Create a new user based on Google account information
     */
    private Users createGoogleUser(String email, String name, String pictureUrl, String googleId) {
        log.info("Creating new user from Google account for email: {}", email);

        // Generate a random secure password for the user
        String randomPassword = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(randomPassword);

        // Extract a username from the email (or use the email as username)
        String username = email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Create the new user
        Users newUser = Users.builder()
                .email(email)
                .username(username)
                .fullName(name)
                .passwordHash(encodedPassword)
                .avatar(pictureUrl)
                .status(UserStatus.ACTIVE) // Google users are already verified
                .roles(new HashSet<>())
                .build();

        // Add ROLE_CUSTOMER by default
        Roles userRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        newUser.getRoles().add(userRole);

        return userRepository.save(newUser);
    }

    /**
     * Update an existing user's information based on latest Google profile data
     */
    private void updateGoogleUserIfNeeded(Users user, String name, String pictureUrl) {
        boolean needsUpdate = false;

        // Update name if needed
        if (name != null && !name.equals(user.getFullName())) {
            user.setFullName(name);
            needsUpdate = true;
        }

        // Update profile picture if needed
        if (pictureUrl != null && !pictureUrl.equals(user.getAvatar())) {
            user.setAvatar(pictureUrl);
            needsUpdate = true;
        }

        // Ensure user is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            needsUpdate = true;
        }

        // Save updates if needed
        if (needsUpdate) {
            userRepository.save(user);
            log.info("Updated user information from Google profile for: {}", user.getEmail());
        }
    }

    /**
     * Link a Google account to an existing user account
     */
    @Transactional
    public ApiResponse linkGoogleAccount(Long userId, String idTokenString) {
        log.info("Linking Google account to user ID: {}", userId);

        try {
            // Verify the Google ID token
            GoogleIdToken idToken = verifyGoogleIdToken(idTokenString);
            if (idToken == null) {
                throw new RuntimeException("Invalid Google ID token");
            }

            // Extract user info from the token
            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();

            if (!emailVerified) {
                throw new RuntimeException("Email not verified by Google");
            }

            // Check if another user already has this Google account linked
            if (userRepository.existsByEmail(email) && !userRepository.findByEmail(email).get().getUserId().equals(userId)) {
                throw new RuntimeException("This Google account is already linked to another user");
            }

            // Get the user to link
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update user with Google email if different
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                userRepository.save(user);
            }

            log.info("Google account successfully linked to user ID: {}", userId);
            return new ApiResponse(true, "Google account successfully linked");

        } catch (Exception e) {
            log.error("Error linking Google account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to link Google account: " + e.getMessage());
        }
    }

    /**
     * Unlink a Google account from a user
     */
    @Transactional
    public ApiResponse unlinkGoogleAccount(Long userId) {
        log.info("Unlinking Google account from user ID: {}", userId);

        try {
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if user has a password set (required for unlinking)
            if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
                throw new RuntimeException("Cannot unlink Google account because no password is set. Please set a password first");
            }

            // Clear Google account connection (implementation depends on how you store this)
            // For this example, we're assuming you're using the email as the connection
            // You might want to store a separate googleId field in your Users entity

            log.info("Google account successfully unlinked from user ID: {}", userId);
            return new ApiResponse(true, "Google account successfully unlinked");

        } catch (Exception e) {
            log.error("Error unlinking Google account: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to unlink Google account: " + e.getMessage());
        }
    }
}