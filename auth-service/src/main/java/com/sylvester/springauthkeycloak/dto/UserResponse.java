package com.sylvester.springauthkeycloak.dto;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstname,
        String lastname
) {
}
