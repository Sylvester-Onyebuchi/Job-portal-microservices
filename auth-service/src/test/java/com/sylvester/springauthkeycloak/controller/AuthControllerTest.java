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
                "sylvester@example.com",
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
    void forgotPasswordReturnsGenericSuccessMessage() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("sylvester@example.com");

        mockMvc.perform(post("/api/v1/auth/public/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "If an account with this email exists, a password reset link has been sent."
                ));

        verify(authService).forgotPassword("sylvester@example.com");
    }

    @Test
    void resendVerificationEmailReturnsOkAndDelegatesToService() throws Exception {
        ResendEmailRequest request = new ResendEmailRequest("sylvester@example.com");

        mockMvc.perform(post("/api/v1/auth/public/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(authService).resendVerificationEmail("sylvester@example.com");
    }

    @Test
    void loginReturnsTokenResponse() throws Exception {
        LoginRequest request = new LoginRequest("sylvester@example.com", "password123");
        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token");

        when(authService.login(request)).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.refresh_token").value("refresh-token"));

        verify(authService).login(request);
    }

    @Test
    void refreshTokenReturnsTokenResponse() {
        TokenRequest request = new TokenRequest("refresh-token");
        TokenResponse tokenResponse = new TokenResponse("new-access-token", "new-refresh-token");

        when(authService.refresh("refresh-token")).thenReturn(tokenResponse);

        ResponseEntity<TokenResponse> response = authController.refreshToken(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tokenResponse, response.getBody());
        verify(authService).refresh("refresh-token");
    }

    @Test
    void logoutReturnsNoContentAndDelegatesToService() {
        TokenRequest request = new TokenRequest("refresh-token");

        ResponseEntity<?> response = authController.logout(request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).logout("refresh-token");
    }
}
