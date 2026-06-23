package com.sylvester.springauthkeycloak.dto;

public record UpdateUserRequest(

        String email,

        String firstName,

        String lastName,

        String username
) {
}
