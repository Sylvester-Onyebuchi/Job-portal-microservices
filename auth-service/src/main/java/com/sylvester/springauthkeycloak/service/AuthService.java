package com.sylvester.springauthkeycloak.service;

import com.sylvester.springauthkeycloak.dto.*;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    void createUser(CreateUserRequest request);

    void resendVerificationEmail(String email);

    UserResponse getUser(String userId);

    void updateUser(String userId, UpdateUserRequest request);

    void deleteUser(String id);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void forgotPassword(String email);

    void logout(String refreshToken);
}
