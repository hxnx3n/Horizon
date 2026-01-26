package com.horizon.backend.service;

import com.horizon.backend.dto.auth.LoginRequest;
import com.horizon.backend.dto.auth.LoginResponse;
import com.horizon.backend.dto.auth.RefreshTokenRequest;
import com.horizon.backend.dto.auth.RegisterRequest;
import com.horizon.backend.dto.user.UserDto;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(String token);

    boolean validateToken(String token);

    UserDto getCurrentUser();
}
