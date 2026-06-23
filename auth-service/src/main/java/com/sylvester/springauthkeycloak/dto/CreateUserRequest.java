package com.sylvester.springauthkeycloak.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "username is required")
        String username,

        @Email(message = "Email must be in valid format")
        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "firstname is required")
        String firstName,

        @NotBlank(message = "lastname is required")
        String lastName,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 15, message = "Password must be between 6 and 15 characters long")
        String password
) {
}
