package com.sylvester.springauthkeycloak.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(
        @NotBlank(message = "RefreshToken is required")
        String refreshToken
) {
}
