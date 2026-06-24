package com.sylvester.springauthkeycloak.service;



import com.sylvester.springauthkeycloak.dto.*;
import com.sylvester.springauthkeycloak.entity.User;
import com.sylvester.springauthkeycloak.exception.AlreadyExistException;
import com.sylvester.springauthkeycloak.exception.NotFoundException;
import com.sylvester.springauthkeycloak.repository.TokenRepository;
import com.sylvester.springauthkeycloak.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final Keycloak keycloak;

    private final UserRepository userRepository;

    private final RestClient restClient;

    private final TokenRepository tokenRepository;


    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;




    public AuthServiceImpl(Keycloak keycloak,
                           UserRepository userRepository,
                           RestClient restClient,
                           TokenRepository tokenRepository) {
        this.keycloak = keycloak;
        this.userRepository = userRepository;
        this.restClient = restClient;
        this.tokenRepository = tokenRepository;

    }


    @Override
    @Transactional
    public void createUser(CreateUserRequest request) {

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setUsername(request.username());
        user.setEmailVerified(false);
        user.setCredentials(Collections.singletonList(credential));
        user.setEnabled(true);

        UsersResource userResource = keycloak.realm(realm).users();

        Response response = userResource.create(user);

        if (response.getStatus() == 409){
            throw new AlreadyExistException("Email  already exists");
        }

        log.info("User created successfully");
        String userId = CreatedResponseUtil.getCreatedId(response);
        keycloak.realm(realm).users().get(userId).sendVerifyEmail();
        log.info("Email verification sent successfully");

        var newUser = User.builder()
                .id(userId)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(newUser);
        log.info("User saved to app database");
        assignRole(userId, "USER");

    }

    @Override
    public void resendVerificationEmail(String email) {
       List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email,true);

       if (users.isEmpty()) {
           throw new NotFoundException("User not found");
       }
        UserRepresentation user = users.getFirst();

        if (Boolean.TRUE.equals(user.isEmailVerified())) {
            throw new AlreadyExistException("Email already verified");
        }
       keycloak.realm(realm).users().get(user.getId()).sendVerifyEmail();
        log.info("Email verification sent successfully");
    }

    @Override
    public UserResponse getUser(String userId){
        try {
            UserRepresentation user = keycloak.realm(realm).users().get(userId)
                    .toRepresentation();
            return new UserResponse(
                    user.getId(), user.getUsername(), user.getEmail(),
                    user.getFirstName(), user.getLastName()
            );
        } catch (NotFoundException e) {
            throw new NotFoundException("User not found");
        }
    }

    @Transactional
    @Override
    public void updateUser(String userId, UpdateUserRequest request) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        boolean emailChanged = !Objects.equals(user.getEmail(), request.email());
        if (request.firstName() != null && !Objects.equals(user.getFirstName(),request.firstName())) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !Objects.equals(user.getLastName(),request.lastName())) {
            user.setLastName(request.lastName());
        }

        if (emailChanged) {
            user.setEmail(request.email());
            user.setEmailVerified(false);
        }
        userResource.update(user);
        User appUser = userRepository.findUserById(userId).orElseThrow(
                () -> new NotFoundException("User not found")
        );
        if (request.firstName() != null) {
            appUser.setFirstName(request.firstName());
        }
        if (request.lastName()  != null) {
            appUser.setLastName(request.lastName());
        }

        if (emailChanged) {
            appUser.setEmail(request.email());
        }

        userRepository.save(appUser);

        if (emailChanged) {
            userResource.sendVerifyEmail();
            userResource.logout();
        }
        log.info("User updated successfully");
    }

    @Override
    @Transactional
    public void deleteUser(String id) {
        keycloak.realm(realm).users().delete(id);

        var user = userRepository.findUserById(id).orElseThrow(
                () -> new NotFoundException("User not found")
        );
        userRepository.delete(user);
        log.info("User deleted successfully");

    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", request.email());
        formData.add("password", request.password());

        String url = "http://keycloak:8080/realms/"+realm+"/protocol/openid-connect/token";

      TokenResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(TokenResponse.class);

      tokenRepository.storeTokens(request.email(), response.accessToken(), response.refreshToken(),
              response.expiresIn() * 1000, response.refreshExpiresIn() * 1000);

      return response;


    }

    @Override
    public TokenResponse refresh(String refreshToken, String username){

        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();

        String url = "http://keycloak:8080/realms/"+realm+"/protocol/openid-connect/token";

        form.add("grant_type","refresh_token");

        form.add("client_id",clientId);

        form.add("client_secret",clientSecret);

        form.add("refresh_token",refreshToken);

        TokenResponse response = restClient.post()

                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        tokenRepository.removeAllTokens(username, response.expiresIn() * 1000, response.refreshExpiresIn() * 1000);

        tokenRepository.storeTokens(username, response.accessToken(), response.refreshToken(),
                response.expiresIn() * 1000, response.refreshExpiresIn() * 1000);
        return response;

    }


    @Override
    public void forgotPassword(String email) {
        List<UserRepresentation> users = keycloak.realm(realm).users()
                        .searchByEmail(email, true);
        if (users.isEmpty()) {
            throw new NotFoundException("User not found");
        }
        UserRepresentation user = users.getFirst();
        keycloak.realm(realm).users().get(user.getId()).executeActionsEmail(
                List.of("UPDATE_PASSWORD")
        );

        log.info("password reset email sent successfully");

    }

    @Override
    public void logout(String email,String refreshToken){

        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();

        String logoutUrl = "http://keycloak:8080/realms/" +realm + "/protocol/openid-connect/logout";

        form.add("client_id",clientId);

        form.add("client_secret",clientSecret);

        form.add("refresh_token",refreshToken);

        restClient.post()
                .uri(logoutUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
        String accessToken = tokenRepository.getAccessToken(email);
        String refresh = tokenRepository.getRefreshToken(email);

        Long accessExpiration = tokenRepository.remainingLifetime(accessToken);
        Long refreshExpiration = tokenRepository.getRefreshTokenTtl(refresh);

        tokenRepository.removeAllTokens(email, accessExpiration, refreshExpiration);



    }


    public void assignRole(String userId, String roleName) {

        RealmResource realmResource = keycloak.realm(realm);

        RoleRepresentation role = realmResource.roles()
                .get(roleName)
                .toRepresentation();

        realmResource.users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }


}

