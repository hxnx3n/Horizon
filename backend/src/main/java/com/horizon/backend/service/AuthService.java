package com.horizon.backend.service;

import com.horizon.backend.dto.auth.LoginRequest;
import com.horizon.backend.dto.auth.LoginResponse;
import com.horizon.backend.dto.auth.RegisterRequest;
import com.horizon.backend.dto.user.UserDto;

public interface AuthService {

    LoginResponse register(RegisterRequest request, String userAgent, String ipAddress);

    LoginResponse login(LoginRequest request, String userAgent, String ipAddress);

    LoginResponse refreshToken(String refreshToken, String userAgent, String ipAddress);

    void logout(String refreshToken, String accessToken);

    void logoutAll(String accessToken);

    boolean validateToken(String token);

    UserDto getCurrentUser();
}
