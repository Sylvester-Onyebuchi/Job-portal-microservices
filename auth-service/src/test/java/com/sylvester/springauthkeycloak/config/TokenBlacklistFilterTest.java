package com.sylvester.springauthkeycloak.config;

import com.sylvester.springauthkeycloak.repository.TokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistFilterTest {

    private static final String EMAIL = "sylvester@example.com";
    private static final String TOKEN = "access-token";

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private FilterChain filterChain;

    private TokenBlacklistFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TokenBlacklistFilter(tokenRepository, jwtDecoder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void continuesFilterChainWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtDecoder, never()).decode(TOKEN);
    }

    @Test
    void continuesFilterChainWhenBearerTokenMatchesStoredAccessToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwt());
        when(tokenRepository.getAccessToken(EMAIL)).thenReturn(TOKEN);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void returnsUnauthorizedWhenStoredAccessTokenDoesNotMatchBearerToken() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtDecoder.decode(TOKEN)).thenReturn(jwt());
        when(tokenRepository.getAccessToken(EMAIL)).thenReturn("different-token");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"message\":\"Token has been revoked\"}", response.getContentAsString());
    }

    private Jwt jwt() {
        return Jwt.withTokenValue(TOKEN)
                .header("alg", "none")
                .claim("email", EMAIL)
                .build();
    }
}
