package com.sylvester.springauthkeycloak.controller;


import com.sylvester.springauthkeycloak.dto.*;
import com.sylvester.springauthkeycloak.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/public/create")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        authService.createUser(request);
        return new  ResponseEntity<>(HttpStatus.CREATED);
    }


    @PostMapping("/public/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "If an account with this email exists, a password reset link has been sent."
        ));
    }

    @PostMapping("/public/resend")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody ResendEmailRequest request) {
        authService.resendVerificationEmail(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/public/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(response);

    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody TokenRequest request, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        TokenResponse response = authService.refresh(request.refreshToken(), email);
        return ResponseEntity.ok(response);

    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody TokenRequest request, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        authService.logout(email,request.refreshToken());
        return ResponseEntity.noContent().build();

    }


}
