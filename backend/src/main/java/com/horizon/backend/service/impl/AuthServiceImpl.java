package com.horizon.backend.service.impl;

import com.horizon.backend.dto.auth.LoginRequest;
import com.horizon.backend.dto.auth.LoginResponse;
import com.horizon.backend.dto.auth.RegisterRequest;
import com.horizon.backend.dto.user.UserDto;
import com.horizon.backend.entity.User;
import com.horizon.backend.exception.BadRequestException;
import com.horizon.backend.exception.DuplicateResourceException;
import com.horizon.backend.exception.ResourceNotFoundException;
import com.horizon.backend.exception.UnauthorizedException;
import com.horizon.backend.repository.UserRepository;
import com.horizon.backend.security.JwtTokenProvider;
import com.horizon.backend.service.AuthService;
import com.horizon.backend.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final long jwtExpiration;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService,
            TokenService tokenService,
            @Value("${jwt.expiration}") long jwtExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.jwtExpiration = jwtExpiration;
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = tokenService.createRefreshToken(savedUser, userAgent, ipAddress);

        return buildLoginResponse(savedUser, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String userAgent, String ipAddress) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = tokenService.createRefreshToken(user, userAgent, ipAddress);

        log.info("User logged in successfully: {}", user.getEmail());

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken, String userAgent, String ipAddress) {
        TokenService.RefreshTokenInfo tokenInfo = tokenService.validateRefreshToken(refreshToken);

        if (tokenInfo == null) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(tokenInfo.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", tokenInfo.userId()));

        tokenService.deleteRefreshToken(refreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtTokenProvider.generateToken(userDetails);
        String newRefreshToken = tokenService.createRefreshToken(user, userAgent, ipAddress);

        log.info("Token refreshed for user: {}", user.getEmail());

        return buildLoginResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        if (refreshToken != null) {
            tokenService.deleteRefreshToken(refreshToken);
        }

        if (accessToken != null) {
            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
            tokenService.blacklistAccessToken(accessToken);
        }

        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void logoutAll(String accessToken) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        tokenService.deleteAllUserRefreshTokens(user.getId());

        if (accessToken != null) {
            if (accessToken.startsWith("Bearer ")) {
                accessToken = accessToken.substring(7);
            }
            tokenService.blacklistAccessToken(accessToken);
        }

        SecurityContextHolder.clearContext();
        log.info("User logged out from all devices: {}", email);
    }

    @Override
    public boolean validateToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (tokenService.isAccessTokenBlacklisted(token)) {
            return false;
        }

        return jwtTokenProvider.validateToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() 
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Not authenticated");
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profileImageUrl(user.getProfileImageUrl())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

        return LoginResponse.of(accessToken, refreshToken, jwtExpiration, userInfo);
    }
}
