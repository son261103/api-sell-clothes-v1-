package com.example.api_sell_clothes_v1.Security;

import com.example.api_sell_clothes_v1.Entity.RefreshTokens;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.authorized-redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        log.info("OAuth2 authentication success, generating tokens");

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.warn("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<String> redirectUri = Optional.ofNullable(request.getParameter("redirect_uri"));

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            log.error("Unauthorized redirect URI detected: {}", redirectUri.get());
            throw new RuntimeException("Unauthorized redirect URI");
        }

        String targetUrl = redirectUri.orElse(this.redirectUri);

        // Get the principal as OAuth2User
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = ((Number) oAuth2User.getAttribute("userId")).longValue();

        // Get user from database
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Create CustomUserDetails
        CustomUserDetails userDetails = new CustomUserDetails(
                user,
                authentication.getAuthorities()
        );

        // Generate JWT token
        String token = jwtService.generateToken(userDetails);

        // Create refresh token
        RefreshTokens refreshToken = refreshTokenService.createRefreshToken(user);

        // Add the token to cookies
        Cookie refreshTokenCookie = createRefreshTokenCookie(refreshToken.getRefreshToken());
        response.addCookie(refreshTokenCookie);

        // Add the token to the redirect URL
        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("token", token)
                .build().toUriString();
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);
        URI authorizedRedirectUri = URI.create(redirectUri);

        // Only validate host and port combination
        return authorizedRedirectUri.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                && authorizedRedirectUri.getPort() == clientRedirectUri.getPort();
    }

    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
    }

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