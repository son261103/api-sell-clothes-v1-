package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.TokenResponseDTO;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Service.AuthGlobalService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthController {

    private final AuthGlobalService authGlobalService;

    /**
     * Endpoint to authenticate with Google using ID token
     */
    @PostMapping("/login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> requestBody,
                                         HttpServletResponse response,
                                         @RequestParam(defaultValue = "false") boolean rememberMe) {
        log.info("Google login attempt");

        try {
            String idToken = requestBody.get("idToken");
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Google ID token is required"));
            }

            TokenResponseDTO tokenResponse = authGlobalService.authenticateWithGoogle(idToken);

            // Set refresh token cookie if remember me is enabled
            if (rememberMe) {
                Cookie refreshTokenCookie = createRefreshTokenCookie(tokenResponse.getRefreshToken());
                response.addCookie(refreshTokenCookie);
                // Remove refresh token from response body when using cookie
                tokenResponse.setRefreshToken(null);
                log.info("Remember me enabled - Set refresh token cookie for Google login");
            }

            log.info("Google login successful");
            return ResponseEntity.ok(tokenResponse);

        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Google authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Link Google account to existing user account
     */
    @PostMapping("/link")
    public ResponseEntity<?> linkGoogleAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> requestBody) {

        log.info("Request to link Google account to user");

        try {
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            String idToken = requestBody.get("idToken");
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Google ID token is required"));
            }

            ApiResponse response = authGlobalService.linkGoogleAccount(
                    userDetails.getUser().getUserId(),
                    idToken
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to link Google account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Unlink Google account from user account
     */
    @PostMapping("/unlink")
    public ResponseEntity<?> unlinkGoogleAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Request to unlink Google account from user");

        try {
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            ApiResponse response = authGlobalService.unlinkGoogleAccount(
                    userDetails.getUser().getUserId()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to unlink Google account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Handle OAuth2 errors
     */
    @GetMapping("/oauth2/error")
    public ResponseEntity<?> handleOAuth2Error() {
        log.error("OAuth2 authentication error");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "OAuth2 authentication failed"));
    }

    // Helper method for cookies
    private Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Enable on HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }
}