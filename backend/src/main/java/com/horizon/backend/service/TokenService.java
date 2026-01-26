package com.horizon.backend.service;

import com.horizon.backend.entity.User;

public interface TokenService {

    String createRefreshToken(User user, String userAgent, String ipAddress);

    RefreshTokenInfo validateRefreshToken(String token);

    void deleteRefreshToken(String token);

    void deleteAllUserRefreshTokens(Long userId);

    void blacklistAccessToken(String token);

    boolean isAccessTokenBlacklisted(String token);

    record RefreshTokenInfo(Long userId, String email, String userAgent, String ipAddress) {}
}
