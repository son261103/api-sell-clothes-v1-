package com.example.api_sell_clothes_v1.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Extract and validate Authorization header
            final String authHeader = request.getHeader("Authorization");
            if (!isValidAuthHeader(authHeader)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Clean and extract JWT token
            final String jwt = extractAndCleanToken(authHeader);
            if (jwt == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Process authentication
            processAuthentication(jwt, request);

        } catch (Exception e) {
            logger.error("Error processing JWT token: {}", e.getMessage());
            handleAuthenticationError(response, e);
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/") ||
                path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/refresh-token");
    }

    private boolean isValidAuthHeader(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7;
    }

    private String extractAndCleanToken(String authHeader) {
        try {
            String token = authHeader.substring(7).trim();
            // Clean the token by removing any invisible characters
            return token.replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]", "");
        } catch (Exception e) {
            logger.error("Error extracting token from header: {}", e.getMessage());
            return null;
        }
    }

    private void processAuthentication(String jwt, HttpServletRequest request) {
        try {
            // Extract username from token
            String username = jwtService.extractUsername(jwt);
            if (username == null || username.trim().isEmpty()) {
                logger.error("No username found in token");
                return;
            }

            // Check if authentication is already set
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }

            // Load user details and validate token
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(jwt, userDetails)) {
                logger.error("Invalid token for user: {}", username);
                return;
            }

            // Set authentication
            setAuthentication(jwt, userDetails, request);

        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error during authentication: {}", e.getMessage());
        }
    }

    private void setAuthentication(String jwt, UserDetails userDetails, HttpServletRequest request) {
        try {
            // Extract roles and permissions
            Set<String> roles = jwtService.extractRoles(jwt);
            Set<String> permissions = jwtService.extractPermissions(jwt);

            // Build authorities
            Set<GrantedAuthority> authorities = buildAuthorities(roles, permissions);

            // Create authentication token
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities
            );

            // Set details and context
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

        } catch (Exception e) {
            logger.error("Error setting authentication: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private Set<GrantedAuthority> buildAuthorities(Set<String> roles, Set<String> permissions) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role-based authorities
        if (roles != null) {
            roles.stream()
                    .filter(role -> role != null && !role.trim().isEmpty())
                    .forEach(role -> authorities.add(
                            new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().trim())
                    ));
        }

        // Add permission-based authorities
        if (permissions != null) {
            permissions.stream()
                    .filter(permission -> permission != null && !permission.trim().isEmpty())
                    .forEach(permission -> authorities.add(
                            new SimpleGrantedAuthority(permission.trim())
                    ));
        }

        return authorities;
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        logger.error("Authentication error: {}", e.getMessage());

        if (e instanceof AuthenticationException) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Authentication failed: " + e.getMessage());
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal server error during authentication");
        }
    }
}