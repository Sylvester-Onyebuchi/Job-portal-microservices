package com.sylvester.springauthkeycloak.controller;


import com.sylvester.springauthkeycloak.dto.UpdateUserRequest;
import com.sylvester.springauthkeycloak.dto.UserResponse;
import com.sylvester.springauthkeycloak.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    @GetMapping("/user")
    public ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal Jwt jwt){
        String userId = jwt.getSubject();
        UserResponse  response = authService.getUser(userId);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/user/update")
    public ResponseEntity<?> updateUser(@RequestBody UpdateUserRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        authService.updateUser(userId, request);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/user/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getClaimAsString("sub");
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
