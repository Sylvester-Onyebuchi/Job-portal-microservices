package com.sylvester.notificationservice.userService;

public record RegisterEvent(
        String id,
        String firstname,
        String lastname,
        String email,
        String code
) {
}
