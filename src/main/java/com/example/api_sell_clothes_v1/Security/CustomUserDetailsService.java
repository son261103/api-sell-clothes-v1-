package com.example.api_sell_clothes_v1.Security;

import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Users user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with login ID: " + loginId));

        // Kiểm tra status của user
        switch (user.getStatus()) {
            case UserStatus.PENDING:
                throw new RuntimeException("Account has not been activated");
            case UserStatus.BANNED:
                throw new RuntimeException("account has been banned");
            case UserStatus.LOCKED:
                throw new RuntimeException("account has been locked");
            case ACTIVE:
                break;
            default:
                throw new RuntimeException("Invalid account status");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add role-based authorities
        for (Roles role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));

            // Add permission-based authorities
            role.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getCodeName()))
            );
        }

        return new CustomUserDetails(user, authorities);
    }
}