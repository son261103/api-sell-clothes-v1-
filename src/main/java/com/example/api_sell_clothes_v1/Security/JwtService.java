package com.example.api_sell_clothes_v1.Security;

import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Getter
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
            Users user = customUserDetails.getUser();

            // Add roles
            Set<String> roles = user.getRoles().stream()
                    .map(Roles::getName)
                    .collect(Collectors.toSet());
            claims.put("roles", roles);

            // Add permissions
            Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getCodeName())
                    .collect(Collectors.toSet());
            claims.put("permissions", permissions);

            // Add user info
            claims.put("userId", user.getUserId());
            claims.put("email", user.getEmail());
            claims.put("fullName", user.getFullName());
            claims.put("status", user.getStatus());
        }

        return buildToken(claims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException e) {
            logger.error("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new JwtException("Empty or null JWT token");
            }

            // Clean the token
            token = token.trim().replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]", "");

            return Jwts
                    .parser()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed JWT token", e);
        } catch (ExpiredJwtException e) {
            logger.error("JWT token has expired: {}", e.getMessage());
            throw new JwtException("JWT token has expired", e);
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported JWT token", e);
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new JwtException("JWT token compact of handler are invalid", e);
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new JwtException("Invalid JWT token", e);
        }
    }

    private Key getSignInKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid secret key encoding: {}", e.getMessage());
            throw new JwtException("Invalid secret key encoding", e);
        }
    }

    // Methods to extract additional claims with error handling
    public Set<String> extractRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            List<String> roles = claims.get("roles", List.class);
            return roles != null ? new HashSet<>(roles) : new HashSet<>();
        } catch (Exception e) {
            logger.error("Error extracting roles from token: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    public Set<String> extractPermissions(String token) {
        try {
            Claims claims = extractAllClaims(token);
            List<String> permissions = claims.get("permissions", List.class);
            return permissions != null ? new HashSet<>(permissions) : new HashSet<>();
        } catch (Exception e) {
            logger.error("Error extracting permissions from token: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Integer userId = claims.get("userId", Integer.class);
            return userId != null ? userId.longValue() : null;
        } catch (Exception e) {
            logger.error("Error extracting userId from token: {}", e.getMessage());
            return null;
        }
    }

    public String extractEmail(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    public String extractFullName(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("fullName", String.class);
        } catch (Exception e) {
            logger.error("Error extracting fullName from token: {}", e.getMessage());
            return null;
        }
    }
}