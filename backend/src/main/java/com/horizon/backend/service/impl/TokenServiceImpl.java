package com.horizon.backend.service.impl;

import com.horizon.backend.entity.User;
import com.horizon.backend.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public TokenServiceImpl(
            RedisTemplate<String, String> redisTemplate,
            @Value("${jwt.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration) {
        this.redisTemplate = redisTemplate;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @Override
    public String createRefreshToken(User user, String userAgent, String ipAddress) {
        String token = UUID.randomUUID().toString();
        String key = REFRESH_TOKEN_PREFIX + token;

        String value = String.join("|",
                user.getId().toString(),
                user.getEmail(),
                userAgent != null ? userAgent : "",
                ipAddress != null ? ipAddress : ""
        );

        redisTemplate.opsForValue().set(key, value, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        String userTokensKey = USER_TOKENS_PREFIX + user.getId();
        redisTemplate.opsForSet().add(userTokensKey, token);
        redisTemplate.expire(userTokensKey, refreshTokenExpiration, TimeUnit.MILLISECONDS);

        log.info("Created refresh token for user: {}", user.getEmail());

        return token;
    }

    @Override
    public RefreshTokenInfo validateRefreshToken(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        String[] parts = value.split("\\|", -1);
        if (parts.length < 4) {
            return null;
        }

        return new RefreshTokenInfo(
                Long.parseLong(parts[0]),
                parts[1],
                parts[2],
                parts[3]
        );
    }

    @Override
    public void deleteRefreshToken(String token) {
        String key = REFRESH_TOKEN_PREFIX + token;
        String value = redisTemplate.opsForValue().get(key);

        if (value != null) {
            String[] parts = value.split("\\|", -1);
            if (parts.length >= 1) {
                String userTokensKey = USER_TOKENS_PREFIX + parts[0];
                redisTemplate.opsForSet().remove(userTokensKey, token);
            }
        }

        redisTemplate.delete(key);
        log.info("Deleted refresh token");
    }

    @Override
    public void deleteAllUserRefreshTokens(Long userId) {
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

        if (tokens != null && !tokens.isEmpty()) {
            for (String token : tokens) {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + token);
            }
        }

        redisTemplate.delete(userTokensKey);
        log.info("Deleted all refresh tokens for user: {}", userId);
    }

    @Override
    public void blacklistAccessToken(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", accessTokenExpiration, TimeUnit.MILLISECONDS);
        log.info("Blacklisted access token");
    }

    @Override
    public boolean isAccessTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
