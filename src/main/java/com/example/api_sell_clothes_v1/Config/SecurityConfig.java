package com.example.api_sell_clothes_v1.Config;

import com.example.api_sell_clothes_v1.Constants.*;
import com.example.api_sell_clothes_v1.Security.CustomUserDetailsService;
import com.example.api_sell_clothes_v1.Security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Configure endpoint permissions for Product management
     */
    private void configureProductEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PRODUCT_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for Order management
     */
    private void configureOrderEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.ORDER_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for User management
     */
    private void configureUserEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.USER_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for Category management
     */
    private void configureCategoryEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.CATEGORY_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for Review management
     */
    private void configureReviewEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.REVIEW_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure basic security settings
     */
    private void configureBasicSecurity(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(SecurityUrlConstants.PUBLIC_URLS).permitAll()
                .requestMatchers(SecurityUrlConstants.ADMIN_URLS).hasRole(RoleConstants.ROLE_ADMIN)
                .requestMatchers(SecurityUrlConstants.USER_URLS)
                .hasAnyRole(RoleConstants.ROLE_CUSTOMER, RoleConstants.ROLE_ADMIN);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization
                .authorizeHttpRequests(auth -> {
                    // Configure basic security settings
                    configureBasicSecurity(auth);

                    // Configure detailed endpoint permissions
                    configureProductEndpoints(auth);
                    configureOrderEndpoints(auth);
                    configureUserEndpoints(auth);
                    configureCategoryEndpoints(auth);
                    configureReviewEndpoints(auth);

                    // Any other request needs authentication
                    auth.anyRequest().authenticated();
                })

                // Configure session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure authentication
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Set allowed origins
        configuration.setAllowedOrigins(Arrays.asList(CorsConstants.ALLOWED_ORIGINS));

        // Set allowed methods
        configuration.setAllowedMethods(Arrays.asList(CorsConstants.ALLOWED_METHODS));

        // Set allowed headers
        configuration.setAllowedHeaders(Arrays.asList(CorsConstants.ALLOWED_HEADERS));

        // Allow credentials
        configuration.setAllowCredentials(true);

        // Set exposed headers
        configuration.setExposedHeaders(List.of(JwtConstants.HEADER_STRING));

        // Set max age
        configuration.setMaxAge(CorsConstants.MAX_AGE);

        // Create URL based CORS configuration source
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}