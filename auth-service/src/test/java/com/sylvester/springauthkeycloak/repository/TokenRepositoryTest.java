package com.sylvester.springauthkeycloak.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRepositoryTest {

    private static final String EMAIL = "sylvester@example.com";
    private static final String ACCESS_KEY = "user:access:" + EMAIL;
    private static final String REFRESH_KEY = "user:refresh:" + EMAIL;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JwtDecoder jwtDecoder;

    private TokenRepository tokenRepository;

    @BeforeEach
    void setUp() {
        tokenRepository = new TokenRepository(redisTemplate, jwtDecoder);
    }

    @Test
    void storeTokensWritesAccessAndRefreshTokensWithMillisecondTtls() {
        mockValueOperations();

        tokenRepository.storeTokens(EMAIL, ACCESS_TOKEN, REFRESH_TOKEN, 300_000L, 1_800_000L);

        verify(valueOperations).set(ACCESS_KEY, ACCESS_TOKEN);
        verify(redisTemplate).expire(ACCESS_KEY, 300_000L, TimeUnit.MILLISECONDS);
        verify(valueOperations).set(REFRESH_KEY, REFRESH_TOKEN);
        verify(redisTemplate).expire(REFRESH_KEY, 1_800_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void getAccessTokenReturnsStoredAccessToken() {
        mockValueOperations();
        when(valueOperations.get(ACCESS_KEY)).thenReturn(ACCESS_TOKEN);

        assertEquals(ACCESS_TOKEN, tokenRepository.getAccessToken(EMAIL));
    }

    @Test
    void getRefreshTokenReturnsStoredRefreshToken() {
        mockValueOperations();
        when(valueOperations.get(REFRESH_KEY)).thenReturn(REFRESH_TOKEN);

        assertEquals(REFRESH_TOKEN, tokenRepository.getRefreshToken(EMAIL));
    }

    @Test
    void removeAllTokensDeletesTokensAndBlacklistsBothWhenTheyExist() {
        mockValueOperations();
        when(valueOperations.get(ACCESS_KEY)).thenReturn(ACCESS_TOKEN);
        when(valueOperations.get(REFRESH_KEY)).thenReturn(REFRESH_TOKEN);

        tokenRepository.removeAllTokens(EMAIL, 120L, 600L);

        verify(redisTemplate).delete(ACCESS_KEY);
        verify(redisTemplate).delete(REFRESH_KEY);
        verify(valueOperations).set("blacklist:access:" + ACCESS_TOKEN, "blacklisted");
        verify(redisTemplate).expire("blacklist:access:" + ACCESS_TOKEN, 120L, TimeUnit.MILLISECONDS);
        verify(valueOperations).set("blacklist:refresh:" + REFRESH_TOKEN, "blacklisted");
        verify(redisTemplate).expire("blacklist:refresh:" + REFRESH_TOKEN, 600L, TimeUnit.MILLISECONDS);
    }

    @Test
    void removeAccessTokenDeletesAndBlacklistsExistingAccessToken() {
        mockValueOperations();
        when(valueOperations.get(ACCESS_KEY)).thenReturn(ACCESS_TOKEN);

        tokenRepository.removeAccessToken(EMAIL, 120L);

        verify(redisTemplate).delete(ACCESS_KEY);
        verify(valueOperations).set("blacklist:access:" + ACCESS_TOKEN, "blacklisted");
        verify(redisTemplate).expire("blacklist:access:" + ACCESS_TOKEN, 120L, TimeUnit.MILLISECONDS);
    }

    @Test
    void blacklistChecksReturnTrueOnlyWhenRedisHasTheBlacklistKey() {
        when(redisTemplate.hasKey("blacklist:access:" + ACCESS_TOKEN)).thenReturn(true);
        when(redisTemplate.hasKey("blacklist:refresh:" + REFRESH_TOKEN)).thenReturn(false);

        assertTrue(tokenRepository.isAccessTokenBlacklisted(ACCESS_TOKEN));
        assertEquals(false, tokenRepository.isRefreshTokenBlacklisted(REFRESH_TOKEN));
    }

    @Test
    void remainingLifetimeReturnsSecondsUntilJwtExpiry() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt(Instant.now().plusSeconds(60)));

        Long lifetime = tokenRepository.remainingLifetime(ACCESS_TOKEN);

        assertTrue(lifetime > 0L);
        assertTrue(lifetime <= 60L);
    }

    @Test
    void remainingLifetimeReturnsZeroWhenJwtHasNoExpiry() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt(null));

        assertEquals(0L, tokenRepository.remainingLifetime(ACCESS_TOKEN));
    }

    @Test
    void remainingLifetimeReturnsZeroWhenJwtIsExpired() {
        when(jwtDecoder.decode(ACCESS_TOKEN)).thenReturn(jwt(Instant.now().minusSeconds(10)));

        assertEquals(0L, tokenRepository.remainingLifetime(ACCESS_TOKEN));
    }

    @Test
    void getRefreshTokenTtlReturnsRedisTtlInSecondsWhenPositive() {
        when(redisTemplate.getExpire(REFRESH_KEY, TimeUnit.SECONDS)).thenReturn(600L);

        assertEquals(600L, tokenRepository.getRefreshTokenTtl(EMAIL));
    }

    @Test
    void getRefreshTokenTtlReturnsZeroWhenRedisTtlIsMissingOrExpired() {
        when(redisTemplate.getExpire(REFRESH_KEY, TimeUnit.SECONDS)).thenReturn(-1L);

        assertEquals(0L, tokenRepository.getRefreshTokenTtl(EMAIL));
    }

    private Jwt jwt(Instant expiresAt) {
        Jwt.Builder builder = Jwt.withTokenValue(ACCESS_TOKEN)
                .header("alg", "none")
                .claim("sub", "user-123");
        if (expiresAt != null) {
            builder.expiresAt(expiresAt);
        }
        return builder.build();
    }

    private void mockValueOperations() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
}
