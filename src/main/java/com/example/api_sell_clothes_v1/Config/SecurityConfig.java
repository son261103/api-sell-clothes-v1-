package com.example.api_sell_clothes_v1.Config;

import com.example.api_sell_clothes_v1.Constants.*;
import com.example.api_sell_clothes_v1.Enums.Types.RoleType;
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
     * Configure endpoint permissions for Permission management
     */
    private void configurePermissionEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PERMISSION_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for Role management
     */
    private void configureRoleEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.ROLE_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }


    /**
     * Configure endpoint permissions for Product management
     */
    private void configureProductEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PRODUCT_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for Product Images management
     */
    private void configureProductImageEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PRODUCT_IMAGE_ENDPOINTS.forEach((endpoint, permission) ->
                auth.requestMatchers(endpoint).hasAuthority(permission)
        );
    }

    /**
     * Configure endpoint permissions for Product variant management
     */

    private void configureProductVariantEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PRODUCT_VARIANT_ENDPOINTS.forEach((endpoint, permission) ->
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

    private void configureCartItemEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.CART_ITEM_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) {
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }

    private void configureCartEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.CART_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) {
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }


    /**
     * Configure endpoint permissions for Order management
     */
    private void configureOrderEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.ORDER_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) { // Thêm kiểm tra null
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }

    /**
     * Configure endpoint permissions for Order Items management
     */
    private void configureOrderItemEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.ORDER_ITEM_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) { // Thêm kiểm tra null
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }

    /**
     * Configure endpoint permissions for User Address management
     */
    private void configureUserAddressEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.USER_ADDRESS_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) { // Thêm kiểm tra null
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }


    /**
     * Configure endpoint permissions for Payment Method management
     */
    private void configurePaymentMethodEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PAYMENT_METHOD_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) {
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }

    /**
     * Configure endpoint permissions for Payment management
     */
    private void configurePaymentEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PAYMENT_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) {
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }

    /**
     * Configure endpoint permissions for Payment History management
     */
    private void configurePaymentHistoryEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.PAYMENT_HISTORY_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) {
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }

    /**
     * Configure endpoint permissions for Shipping management
     */
    private void configureShippingEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        EndpointPermissionConstants.SHIPPING_ENDPOINTS.forEach((endpoint, permission) -> {
            if (permission != null) {
                auth.requestMatchers(endpoint).hasAuthority(permission);
            }
        });
    }


    /**
     * Configure basic security settings
     */
    private void configureBasicSecurity(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(SecurityUrlConstants.PUBLIC_URLS).permitAll()
                .requestMatchers(SecurityUrlConstants.ADMIN_URLS).hasAuthority(RoleType.ROLE_ADMIN.getCode())
                .requestMatchers(SecurityUrlConstants.USER_URLS)
                .hasAnyAuthority(RoleType.ROLE_CUSTOMER.getCode(), RoleType.ROLE_ADMIN.getCode());
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
                    configurePermissionEndpoints(auth);
                    configureRoleEndpoints(auth);
                    configureProductEndpoints(auth);
                    configureProductVariantEndpoints(auth);
                    configureProductImageEndpoints(auth);
                    configureOrderEndpoints(auth);
                    configureUserEndpoints(auth);
                    configureCategoryEndpoints(auth);
                    configureReviewEndpoints(auth);
                    configureCartItemEndpoints(auth);
                    configureCartEndpoints(auth);
                    configureOrderItemEndpoints(auth);     // Thêm mới
                    configureUserAddressEndpoints(auth);   // Thêm mới
                    // Configure Payment Method endpoints
                    configurePaymentMethodEndpoints(auth);
                    // Configure Payment endpoints
                    configurePaymentEndpoints(auth);
                    // Configure Shipping endpoints
                    configureShippingEndpoints(auth);

                    // Configure Payment History endpoints
                    configurePaymentHistoryEndpoints(auth);
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

        // Cho phép tất cả các origin từ danh sách cấu hình
        configuration.setAllowedOriginPatterns(List.of(CorsConstants.ALLOWED_ORIGINS));

        // Cho phép tất cả các phương thức HTTP được cấu hình
        configuration.setAllowedMethods(List.of(CorsConstants.ALLOWED_METHODS));

        // Cho phép tất cả các header được cấu hình
        configuration.setAllowedHeaders(List.of(CorsConstants.ALLOWED_HEADERS));

        // Cho phép gửi credentials (cookies, Authorization headers,...)
        configuration.setAllowCredentials(true);

        // Thiết lập các header mà client có thể đọc từ response
        configuration.setExposedHeaders(List.of(JwtConstants.HEADER_STRING));

        // Thời gian cache CORS (giảm request preflight)
        configuration.setMaxAge(CorsConstants.MAX_AGE);

        // Đăng ký cấu hình CORS với tất cả các URL
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}