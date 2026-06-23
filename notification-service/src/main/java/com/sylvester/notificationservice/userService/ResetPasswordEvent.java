package com.sylvester.notificationservice.userService;

public record ResetPasswordEvent(
        String email,
        String firstname,
        String token
) {
}
