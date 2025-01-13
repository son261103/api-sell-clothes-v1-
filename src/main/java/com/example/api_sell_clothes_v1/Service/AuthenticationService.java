package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Auth.LoginDTO;
import com.example.api_sell_clothes_v1.DTO.Auth.RegisterDTO;
import com.example.api_sell_clothes_v1.DTO.Auth.RegisterResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Auth.TokenResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuthorityService authorityService;
    private final PasswordEncoder passwordEncoder;

    // Phương thức xác thực người dùng
    public TokenResponseDTO authenticate(LoginDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    )
            );

            Users user = userRepository.findByLoginId(request.getLoginId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            Set<String> roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());

            Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getCodeName())
                    .collect(Collectors.toSet());

            return TokenResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password");
        }
    }

    // Phương thức xác thực với OTP
    public TokenResponseDTO authenticateWithOTP(LoginDTO request, String otp) {
        try {
            String cachedOtp = otpService.getOtpFromCache(request.getLoginId());
            if (cachedOtp == null) {
                throw new BadCredentialsException("OTP not found for the given username/email");
            }

            if (!cachedOtp.equals(otp)) {
                throw new BadCredentialsException("Invalid OTP");
            }

            Users user = userRepository.findByLoginId(request.getLoginId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    )
            );

            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            Set<String> roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());

            Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getCodeName())
                    .collect(Collectors.toSet());

            otpService.removeOtpFromCache(request.getLoginId());

            return TokenResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username/password or OTP");
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }


    // Phương thức đăng ký người dùng

    // Phương thức đăng ký
    @Transactional
    public RegisterResponseDTO register(RegisterDTO registerDTO) {
        // Kiểm tra xem email hoặc tên đăng nhập đã tồn tại chưa
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException("Email đã tồn tại.");
        }

        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại.");
        }

        // Kiểm tra mật khẩu và xác nhận mật khẩu
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }

        // Mã hóa mật khẩu trước khi lưu vào cơ sở dữ liệu
        String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());

        // Tạo người dùng mới
        Users newUser = Users.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .passwordHash(encodedPassword)
                .fullName(registerDTO.getFullName())
                .phone(registerDTO.getPhone())
                .build();

        // Lưu người dùng vào cơ sở dữ liệu
        userRepository.save(newUser);

        // Gửi mã OTP cho người dùng qua email
        otpService.sendOtpToEmail(newUser.getEmail());

        // Tạo đối tượng DTO để trả về
        return RegisterResponseDTO.builder()
                .userId(newUser.getUserId())
                .username(newUser.getUsername())
                .email(newUser.getEmail())
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.")
                .requiresEmailVerification(true)
                .build();
    }


    // Phương thức xác thực OTP cho đăng ký
    public boolean verifyOtpForRegister(String email, String otp) {
        // Kiểm tra OTP
        String otpFromCache = otpService.getOtpFromCache(email);

        // Nếu OTP không tồn tại hoặc không khớp
        if (otpFromCache == null || !otpFromCache.equals(otp)) {
            return false; // OTP không hợp lệ
        }

        // Nếu OTP hợp lệ, tìm người dùng từ email
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Cập nhật trạng thái người dùng thành ACTIVE sau khi xác thực thành công
        user.setStatus(UserStatus.ACTIVE);

        // Lưu lại trạng thái người dùng vào cơ sở dữ liệu
        userRepository.save(user);

        // Xóa OTP khỏi cache sau khi xác thực thành công
        otpService.removeOtpFromCache(email);

        return true; // OTP hợp lệ và đã xác thực thành công
    }
}
