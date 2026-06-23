package com.sylvester.springauthkeycloak.controller;

import com.sylvester.springauthkeycloak.dto.UpdateUserRequest;
import com.sylvester.springauthkeycloak.dto.UserResponse;
import com.sylvester.springauthkeycloak.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController(authService);
    }

    @Test
    void getUserUsesJwtSubjectAndReturnsUserResponse() {
        Jwt jwt = jwt("user-123");
        UserResponse userResponse = new UserResponse(
                "user-123",
                "sylvester",
                "sylvester@example.com",
                "Sylvester",
                "Onah"
        );

        when(authService.getUser("user-123")).thenReturn(userResponse);

        ResponseEntity<UserResponse> response = userController.getUser(jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(userResponse, response.getBody());
        verify(authService).getUser("user-123");
    }

    @Test
    void updateUserUsesJwtSubjectAndReturnsOk() {
        Jwt jwt = jwt("user-123");
        UpdateUserRequest request = new UpdateUserRequest(
                "new@example.com",
                "New",
                "Person",
                null
        );

        ResponseEntity<?> response = userController.updateUser(request, jwt);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).updateUser("user-123", request);
    }

    @Test
    void deleteUserUsesJwtSubjectAndReturnsNoContent() {
        Jwt jwt = jwt("user-123");

        ResponseEntity<?> response = userController.deleteUser(jwt);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).deleteUser("user-123");
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("sub", subject)
                .build();
    }
}
