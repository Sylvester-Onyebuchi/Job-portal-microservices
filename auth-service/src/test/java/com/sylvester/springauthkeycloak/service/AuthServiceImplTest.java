package com.sylvester.springauthkeycloak.service;

import com.sylvester.springauthkeycloak.dto.CreateUserRequest;
import com.sylvester.springauthkeycloak.dto.UpdateUserRequest;
import com.sylvester.springauthkeycloak.dto.UserResponse;
import com.sylvester.springauthkeycloak.entity.User;
import com.sylvester.springauthkeycloak.exception.AlreadyExistException;
import com.sylvester.springauthkeycloak.exception.NotFoundException;
import com.sylvester.springauthkeycloak.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String REALM = "master";

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestClient restClient;


    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(keycloak, userRepository, restClient);
        ReflectionTestUtils.setField(authService, "realm", REALM);
        ReflectionTestUtils.setField(authService, "clientId", "client-id");
        ReflectionTestUtils.setField(authService, "clientSecret", "client-secret");
    }

    @Test
    void createUserCreatesKeycloakUserSendsVerificationEmailAndPersistsUser() {
        CreateUserRequest request = new CreateUserRequest(
                "sylvester",
                "sylvester@example.com",
                "Sylvester",
                "Onah",
                "password123"
        );
        Response response = Response.created(URI.create("http://localhost/admin/realms/master/users/user-123")).build();

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(usersResource.get("user-123")).thenReturn(userResource);

        authService.createUser(request);

        ArgumentCaptor<UserRepresentation> keycloakUserCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(keycloakUserCaptor.capture());
        UserRepresentation keycloakUser = keycloakUserCaptor.getValue();
        assertEquals("sylvester", keycloakUser.getUsername());
        assertEquals("sylvester@example.com", keycloakUser.getEmail());
        assertEquals("Sylvester", keycloakUser.getFirstName());
        assertEquals("Onah", keycloakUser.getLastName());
        assertEquals(Boolean.FALSE, keycloakUser.isEmailVerified());
        assertEquals(Boolean.TRUE, keycloakUser.isEnabled());
        assertEquals("password123", keycloakUser.getCredentials().getFirst().getValue());

        verify(userResource).sendVerifyEmail();

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();
        assertEquals("user-123", savedUser.getId());
        assertEquals("sylvester@example.com", savedUser.getEmail());
        assertEquals("Sylvester", savedUser.getFirstName());
        assertEquals("Onah", savedUser.getLastName());
    }

    @Test
    void createUserThrowsWhenKeycloakReportsDuplicateUser() {
        CreateUserRequest request = new CreateUserRequest(
                "sylvester",
                "sylvester@example.com",
                "Sylvester",
                "Onah",
                "password123"
        );
        Response response = Response.status(Response.Status.CONFLICT).build();

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

        assertThrows(AlreadyExistException.class, () -> authService.createUser(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationEmailSendsEmailWhenUserExistsAndIsNotVerified() {
        UserRepresentation user = new UserRepresentation();
        user.setId("user-123");
        user.setEmailVerified(false);

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("sylvester@example.com", true)).thenReturn(List.of(user));
        when(usersResource.get("user-123")).thenReturn(userResource);

        authService.resendVerificationEmail("sylvester@example.com");

        verify(userResource).sendVerifyEmail();
    }

    @Test
    void resendVerificationEmailThrowsWhenUserDoesNotExist() {
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("missing@example.com", true)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> authService.resendVerificationEmail("missing@example.com"));
    }

    @Test
    void resendVerificationEmailThrowsWhenEmailIsAlreadyVerified() {
        UserRepresentation user = new UserRepresentation();
        user.setEmailVerified(true);

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("sylvester@example.com", true)).thenReturn(List.of(user));

        assertThrows(AlreadyExistException.class, () -> authService.resendVerificationEmail("sylvester@example.com"));
    }

    @Test
    void getUserReturnsUserResponseFromKeycloak() {
        UserRepresentation user = new UserRepresentation();
        user.setId("user-123");
        user.setUsername("sylvester");
        user.setEmail("sylvester@example.com");
        user.setFirstName("Sylvester");
        user.setLastName("Onah");

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-123")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(user);

        UserResponse response = authService.getUser("user-123");

        assertEquals("user-123", response.id());
        assertEquals("sylvester", response.username());
        assertEquals("sylvester@example.com", response.email());
        assertEquals("Sylvester", response.firstname());
        assertEquals("Onah", response.lastname());
    }

    @Test
    void updateUserUpdatesKeycloakAndLocalUserAndSendsVerificationWhenEmailChanges() {
        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setId("user-123");
        keycloakUser.setEmail("old@example.com");
        keycloakUser.setFirstName("Old");
        keycloakUser.setLastName("Name");

        User appUser = User.builder()
                .id("user-123")
                .email("old@example.com")
                .firstName("Old")
                .lastName("Name")
                .build();

        UpdateUserRequest request = new UpdateUserRequest("new@example.com", "New", "Person", null);

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("user-123")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(keycloakUser);
        when(userRepository.findUserById("user-123")).thenReturn(Optional.of(appUser));

        authService.updateUser("user-123", request);

        ArgumentCaptor<UserRepresentation> keycloakUserCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(userResource).update(keycloakUserCaptor.capture());
        assertEquals("new@example.com", keycloakUserCaptor.getValue().getEmail());
        assertEquals(Boolean.FALSE, keycloakUserCaptor.getValue().isEmailVerified());

        verify(userResource).sendVerifyEmail();
        verify(userResource).logout();
        verify(userRepository).save(appUser);
        assertEquals("new@example.com", appUser.getEmail());
        assertEquals("New", appUser.getFirstName());
        assertEquals("Person", appUser.getLastName());
    }

    @Test
    void deleteUserDeletesKeycloakAndLocalUser() {
        User appUser = User.builder().id("user-123").build();

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(userRepository.findUserById("user-123")).thenReturn(Optional.of(appUser));

        authService.deleteUser("user-123");

        verify(usersResource).delete("user-123");
        verify(userRepository).delete(appUser);
    }

    @Test
    void forgotPasswordSendsUpdatePasswordEmail() {
        UserRepresentation user = new UserRepresentation();
        user.setId("user-123");

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.searchByEmail("sylvester@example.com", true)).thenReturn(List.of(user));
        when(usersResource.get("user-123")).thenReturn(userResource);

        authService.forgotPassword("sylvester@example.com");

        verify(userResource).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

    @Test
    void loginPostsPasswordGrantFormAndReturnsTokenResponse() {
        var tokenResponse = new com.sylvester.springauthkeycloak.dto.TokenResponse("access-token", "refresh-token");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://localhost:8079/realms/master/protocol/openid-connect/token"))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.sylvester.springauthkeycloak.dto.TokenResponse.class)).thenReturn(tokenResponse);

        var response = authService.login(new com.sylvester.springauthkeycloak.dto.LoginRequest(
                "sylvester@example.com",
                "password123"
        ));

        assertEquals(tokenResponse, response);

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("password", form.getFirst("grant_type"));
        assertEquals("client-id", form.getFirst("client_id"));
        assertEquals("client-secret", form.getFirst("client_secret"));
        assertEquals("sylvester@example.com", form.getFirst("username"));
        assertEquals("password123", form.getFirst("password"));
    }

    @Test
    void refreshPostsRefreshTokenGrantFormAndReturnsTokenResponse() {
        var tokenResponse = new com.sylvester.springauthkeycloak.dto.TokenResponse("access-token", "refresh-token");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://localhost:8079/realms/master/protocol/openid-connect/token"))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(com.sylvester.springauthkeycloak.dto.TokenResponse.class)).thenReturn(tokenResponse);

        var response = authService.refresh("refresh-token");

        assertEquals(tokenResponse, response);

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("refresh_token", form.getFirst("grant_type"));
        assertEquals("client-id", form.getFirst("client_id"));
        assertEquals("client-secret", form.getFirst("client_secret"));
        assertEquals("refresh-token", form.getFirst("refresh_token"));
    }

    @Test
    void logoutPostsLogoutForm() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://localhost:8079/realms/master/protocol/openid-connect/logout"))
                .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        authService.logout("refresh-token");

        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        MultiValueMap<String, String> form = formCaptor.getValue();
        assertEquals("client-id", form.getFirst("client_id"));
        assertEquals("client-secret", form.getFirst("client_secret"));
        assertEquals("refresh-token", form.getFirst("refresh_token"));
    }
}
