package com.sylvester.springauthkeycloak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.springauthkeycloak.dto.CreateUserRequest;
import com.sylvester.springauthkeycloak.dto.ForgotPasswordRequest;
import com.sylvester.springauthkeycloak.dto.LoginRequest;
import com.sylvester.springauthkeycloak.dto.ResendEmailRequest;
import com.sylvester.springauthkeycloak.dto.TokenRequest;
import com.sylvester.springauthkeycloak.dto.TokenResponse;
import com.sylvester.springauthkeycloak.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String EMAIL = "sylvester@example.com";

    @Mock
    private AuthService authService;

    private AuthController authController;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createUserReturnsCreatedAndDelegatesToService() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "sylvester",
                EMAIL,
                "Sylvester",
                "Onah",
                "password123"
        );

        mockMvc.perform(post("/api/v1/auth/public/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).createUser(request);
    }

    @Test
    void forgotPasswordReturnsGenericMessageAndDelegatesToService() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest(EMAIL);

        mockMvc.perform(post("/api/v1/auth/public/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account with this email exists, a password reset link has been sent."
                ));

        verify(authService).forgotPassword(EMAIL);
    }

    @Test
    void resendVerificationEmailReturnsOkAndDelegatesToService() throws Exception {
        ResendEmailRequest request = new ResendEmailRequest(EMAIL);

        mockMvc.perform(post("/api/v1/auth/public/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).resendVerificationEmail(EMAIL);
    }

    @Test
    void loginReturnsTokenResponse() throws Exception {
        LoginRequest request = new LoginRequest(EMAIL, "password123");
        TokenResponse tokenResponse = tokenResponse("access-token", "refresh-token");

        when(authService.login(request)).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
                .andExpect(jsonPath("$.expires_in").value(300))
                .andExpect(jsonPath("$.refresh_expires_in").value(1800))
                .andExpect(jsonPath("$.token_type").value("Bearer"));

        verify(authService).login(request);
    }

    @Test
    void refreshTokenUsesAuthenticatedEmailAndReturnsTokenResponse() {
        TokenRequest request = new TokenRequest("old-refresh-token");
        TokenResponse tokenResponse = tokenResponse("new-access-token", "new-refresh-token");

        when(authService.refresh("old-refresh-token", EMAIL)).thenReturn(tokenResponse);

        ResponseEntity<TokenResponse> response = authController.refreshToken(request, jwt(EMAIL));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tokenResponse, response.getBody());
        verify(authService).refresh("old-refresh-token", EMAIL);
    }

    @Test
    void logoutUsesAuthenticatedEmailAndReturnsNoContent() {
        TokenRequest request = new TokenRequest("refresh-token");

        ResponseEntity<?> response = authController.logout(request, jwt(EMAIL));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).logout(EMAIL, "refresh-token");
    }

    private Jwt jwt(String email) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-123")
                .claim("email", email)
                .build();
    }

    private TokenResponse tokenResponse(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, 300L, 1800L, "Bearer");
    }
}
