package com.horizon.backend.controller;

import com.horizon.backend.common.ApiResponse;
import com.horizon.backend.dto.auth.LoginRequest;
import com.horizon.backend.dto.auth.LoginResponse;
import com.horizon.backend.dto.auth.RegisterRequest;
import com.horizon.backend.dto.user.UserDto;
import com.horizon.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final long refreshExpiration;

    public AuthController(
            AuthService authService,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        this.authService = authService;
        this.refreshExpiration = refreshExpiration;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);

        LoginResponse loginResponse = authService.register(request, userAgent, ipAddress);
        addRefreshTokenCookie(response, loginResponse.getRefreshToken());

        LoginResponse safeResponse = LoginResponse.of(
                loginResponse.getAccessToken(),
                null,
                loginResponse.getExpiresIn(),
                loginResponse.getUser()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(safeResponse, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);

        LoginResponse loginResponse = authService.login(request, userAgent, ipAddress);
        addRefreshTokenCookie(response, loginResponse.getRefreshToken());

        LoginResponse safeResponse = LoginResponse.of(
                loginResponse.getAccessToken(),
                null,
                loginResponse.getExpiresIn(),
                loginResponse.getUser()
        );
        return ResponseEntity.ok(ApiResponse.success(safeResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token not found"));
        }

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);

        LoginResponse loginResponse = authService.refreshToken(refreshToken, userAgent, ipAddress);
        addRefreshTokenCookie(response, loginResponse.getRefreshToken());

        LoginResponse safeResponse = LoginResponse.of(
                loginResponse.getAccessToken(),
                null,
                loginResponse.getExpiresIn(),
                loginResponse.getUser()
        );
        return ResponseEntity.ok(ApiResponse.success(safeResponse, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            HttpServletResponse response) {
        authService.logout(refreshToken, accessToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            HttpServletResponse response) {
        authService.logoutAll(accessToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String token) {
        boolean isValid = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success(isValid, "Token validation complete"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        UserDto user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(user, "User info retrieved successfully"));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(refreshExpiration / 1000)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
