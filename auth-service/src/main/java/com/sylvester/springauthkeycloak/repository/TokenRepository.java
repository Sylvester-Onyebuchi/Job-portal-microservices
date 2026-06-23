package com.sylvester.springauthkeycloak.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class TokenRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtDecoder jwtDecoder;

    private static final String ACCESS_TOKEN_KEY_PREFIX= "user:access:";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "user:refresh:";

    private static final String ACCESS_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String REFRESH_BLACKLIST_PREFIX = "blacklist:refresh:";


    public void storeTokens(
            String username,
            String accessToken,
            String refreshToken,
            Long accessExpiration,
            Long refreshExpiration
    ){
        String accessKey = ACCESS_TOKEN_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(accessKey, accessToken);
        redisTemplate.expire(accessKey, accessExpiration, TimeUnit.MILLISECONDS);
        String refreshKey = REFRESH_TOKEN_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(refreshKey, refreshToken);
        redisTemplate.expire(refreshKey, refreshExpiration, TimeUnit.MILLISECONDS);
    }

    public String getAccessToken(String username){
        String accessKey = ACCESS_TOKEN_KEY_PREFIX + username;
        return getToken(accessKey);
    }

    public String getRefreshToken(String username){
        String accessKey = REFRESH_TOKEN_KEY_PREFIX + username;
        return getToken(accessKey);
    }

    public void removeAllTokens(String username, Long accessExpiration, Long refreshExpiration){
        String accessToken = getAccessToken(username);
        String refreshToken = getRefreshToken(username);
        String accessKey = ACCESS_TOKEN_KEY_PREFIX + username;
        String refreshKey = REFRESH_TOKEN_KEY_PREFIX + username;
        redisTemplate.delete(accessKey);
        redisTemplate.delete(refreshKey);

        if (accessToken != null){
            blacklistTokenAccessToken(accessToken, accessExpiration);
        }
        if (refreshToken != null){
            blacklistTokenRefreshToken(refreshToken, refreshExpiration);
        }
    }

    public boolean isAccessTokenBlacklisted(String token){
        String accessKey = ACCESS_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessKey));

    }

    public boolean isRefreshTokenBlacklisted(String token){
        String refreshKey = REFRESH_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey));
    }

    public void removeAccessToken(String username, Long accessExpiration){
        String accessToken = getAccessToken(username);
        String accessKey = ACCESS_TOKEN_KEY_PREFIX + username;
        redisTemplate.delete(accessKey);
        if (accessToken != null){
            blacklistTokenAccessToken(accessToken, accessExpiration);
        }

    }

    private void blacklistTokenAccessToken(String accessToken, long expiration){
        String key = ACCESS_BLACKLIST_PREFIX + accessToken;
        redisTemplate.opsForValue().set(key, "blacklisted");
        redisTemplate.expire(key, expiration, TimeUnit.MILLISECONDS);
    }

    private void blacklistTokenRefreshToken(String refreshToken, long expiration){
        String key = REFRESH_BLACKLIST_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, "blacklisted");
        redisTemplate.expire(key, expiration, TimeUnit.MILLISECONDS);
    }

    public Long remainingLifetime(String token) {

        Jwt jwt = jwtDecoder.decode(token);

        Instant expiresAt = jwt.getExpiresAt();

        if (expiresAt == null) {
            return 0L;
        }

        Long seconds = Duration.between(
                Instant.now(),
                expiresAt
        ).toSeconds();
        return Math.max(seconds, 0L);
    }

    public Long getRefreshTokenTtl(String username) {
        String key = REFRESH_TOKEN_KEY_PREFIX + username;

        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

        return ttl != null && ttl > 0 ? ttl : 0L;
    }




    private String getToken(String accessKey){
        Object token = redisTemplate.opsForValue().get(accessKey);
        return token != null ? token.toString() : null;
    }


}
