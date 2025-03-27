package com.example.api_sell_clothes_v1.Security;

import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("OAuth2 login attempt with provider: {}", userRequest.getClientRegistration().getRegistrationId());

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        // Extract provider (google, facebook, etc)
        String provider = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        // Extract attributes based on provider
        OAuth2UserInfo oAuth2UserInfo;
        if (provider.equalsIgnoreCase("google")) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("Login with " + provider + " is not supported yet.");
        }

        // Validate required info
        if (oAuth2UserInfo.getEmail() == null || oAuth2UserInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Check if user exists by email
        Optional<Users> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        Users user;

        // If user exists, update user info
        if (userOptional.isPresent()) {
            log.info("Existing user found for OAuth2 login: {}", oAuth2UserInfo.getEmail());
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            // If user doesn't exist, create a new user
            log.info("Creating new user from OAuth2 login: {}", oAuth2UserInfo.getEmail());
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        // Create authorities for the OAuth2User
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> {
                    Set<SimpleGrantedAuthority> auths = new HashSet<>();
                    auths.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
                    role.getPermissions().forEach(permission ->
                            auths.add(new SimpleGrantedAuthority(permission.getCodeName())));
                    return auths.stream();
                })
                .collect(Collectors.toSet());

        // Add user attributes to OAuth2 attributes
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("userId", user.getUserId());

        return new DefaultOAuth2User(
                authorities,
                attributes,
                "name" // Attribut-Key für den Anzeigenamen
        );
    }

    private Users registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        // Create a new user from OAuth2 info
        Users user = new Users();
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setUsername(generateUniqueUsername(oAuth2UserInfo));
        user.setFullName(oAuth2UserInfo.getName());
        user.setAvatar(oAuth2UserInfo.getImageUrl());
        user.setStatus(UserStatus.ACTIVE); // OAuth2 users are pre-verified by the provider

        // Set roles (default to ROLE_CUSTOMER)
        Set<Roles> roles = new HashSet<>();
        Roles customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        roles.add(customerRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    private Users updateExistingUser(Users existingUser, OAuth2UserInfo oAuth2UserInfo) {
        // Update user info if necessary
        if (oAuth2UserInfo.getName() != null && !oAuth2UserInfo.getName().equals(existingUser.getFullName())) {
            existingUser.setFullName(oAuth2UserInfo.getName());
        }

        if (oAuth2UserInfo.getImageUrl() != null && !oAuth2UserInfo.getImageUrl().equals(existingUser.getAvatar())) {
            existingUser.setAvatar(oAuth2UserInfo.getImageUrl());
        }

        return userRepository.save(existingUser);
    }

    private String generateUniqueUsername(OAuth2UserInfo userInfo) {
        // Generate a username from email/name with random suffix to ensure uniqueness
        String baseUsername = userInfo.getEmail().split("@")[0];
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return baseUsername + "_" + uniqueSuffix;
    }
}