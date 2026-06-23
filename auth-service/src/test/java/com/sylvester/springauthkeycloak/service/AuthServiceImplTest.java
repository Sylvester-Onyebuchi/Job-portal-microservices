package com.sylvester.springauthkeycloak.service;

import com.sylvester.springauthkeycloak.dto.CreateUserRequest;
import com.sylvester.springauthkeycloak.dto.LoginRequest;
import com.sylvester.springauthkeycloak.dto.TokenResponse;
import com.sylvester.springauthkeycloak.dto.UpdateUserRequest;
import com.sylvester.springauthkeycloak.dto.UserResponse;
import com.sylvester.springauthkeycloak.entity.User;
import com.sylvester.springauthkeycloak.exception.AlreadyExistException;
import com.sylvester.springauthkeycloak.exception.NotFoundException;
import com.sylvester.springauthkeycloak.repository.TokenRepository;
import com.sylvester.springauthkeycloak.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String EMAIL = "sylvester@example.com";
    private static final String USER_ID = "user-123";

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(keycloak, userRepository, restClient, tokenRepository);
        ReflectionTestUtils.setField(authService, "realm", REALM);
        ReflectionTestUtils.setField(authService, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(authService, "clientSecret", CLIENT_SECRET);
    }

    @Test
    void createUserCreatesKeycloakUserPersistsLocalUserAndAssignsUserRole() {
        CreateUserRequest request = createUserRequest();
        Response created = Response.created(URI.create("http://localhost/admin/realms/master/users/" + USER_ID)).build();
        RoleRepresentation userRole = new RoleRepresentation();
        userRole.setName("USER");

        mockRealmUsers();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(created);
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get("USER")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(userRole);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

        authService.createUser(request);

        ArgumentCaptor<UserRepresentation> keycloakUserCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(keycloakUserCaptor.capture());
        UserRepresentation keycloakUser = keycloakUserCaptor.getValue();
        assertEquals("sylvester", keycloakUser.getUsername());
        assertEquals(EMAIL, keycloakUser.getEmail());
        assertEquals("Sylvester", keycloakUser.getFirstName());
        assertEquals("Onah", keycloakUser.getLastName());
        assertEquals(Boolean.FALSE, keycloakUser.isEmailVerified());
        assertEquals(Boolean.TRUE, keycloakUser.isEnabled());
        assertEquals("password123", keycloakUser.getCredentials().getFirst().getValue());

        verify(userResource).sendVerifyEmail();

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();
        assertEquals(USER_ID, savedUser.getId());
        assertEquals("sylvester", savedUser.getUsername());
        assertEquals(EMAIL, savedUser.getEmail());
        assertEquals("Sylvester", savedUser.getFirstName());
        assertEquals("Onah", savedUser.getLastName());

        ArgumentCaptor<List<RoleRepresentation>> rolesCaptor = ArgumentCaptor.captor();
        verify(roleScopeResource).add(rolesCaptor.capture());
        assertEquals(List.of(userRole), rolesCaptor.getValue());
    }

    @Test
    void createUserThrowsAlreadyExistsWhenKeycloakReturnsConflict() {
        Response conflict = Response.status(Response.Status.CONFLICT).build();

        mockRealmUsers();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(conflict);

        assertThrows(AlreadyExistException.class, () -> authService.createUser(createUserRequest()));

        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerificationEmailSendsEmailForUnverifiedUser() {
        UserRepresentation user = keycloakUser(false);

        mockRealmUsers();
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of(user));
        when(usersResource.get(USER_ID)).thenReturn(userResource);

        authService.resendVerificationEmail(EMAIL);

        verify(userResource).sendVerifyEmail();
    }

    @Test
    void resendVerificationEmailThrowsWhenUserDoesNotExist() {
        mockRealmUsers();
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> authService.resendVerificationEmail(EMAIL));
    }

    @Test
    void resendVerificationEmailThrowsWhenEmailIsAlreadyVerified() {
        mockRealmUsers();
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of(keycloakUser(true)));

        assertThrows(AlreadyExistException.class, () -> authService.resendVerificationEmail(EMAIL));
    }

    @Test
    void getUserReturnsUserResponseFromKeycloak() {
        UserRepresentation user = keycloakUser(false);
        user.setUsername("sylvester");
        user.setFirstName("Sylvester");
        user.setLastName("Onah");

        mockRealmUsers();
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(user);

        UserResponse response = authService.getUser(USER_ID);

        assertEquals(USER_ID, response.id());
        assertEquals("sylvester", response.username());
        assertEquals(EMAIL, response.email());
        assertEquals("Sylvester", response.firstname());
        assertEquals("Onah", response.lastname());
    }

    @Test
    void updateUserUpdatesKeycloakAndLocalUserWhenEmailChanges() {
        UserRepresentation keycloakUser = keycloakUser(true);
        keycloakUser.setEmail("old@example.com");
        keycloakUser.setFirstName("Old");
        keycloakUser.setLastName("Name");

        User appUser = User.builder()
                .id(USER_ID)
                .email("old@example.com")
                .firstName("Old")
                .lastName("Name")
                .build();
        UpdateUserRequest request = new UpdateUserRequest(EMAIL, "Sylvester", "Onah", null);

        mockRealmUsers();
        when(usersResource.get(USER_ID)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(keycloakUser);
        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(appUser));

        authService.updateUser(USER_ID, request);

        ArgumentCaptor<UserRepresentation> keycloakUserCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(userResource).update(keycloakUserCaptor.capture());
        assertEquals(EMAIL, keycloakUserCaptor.getValue().getEmail());
        assertEquals("Sylvester", keycloakUserCaptor.getValue().getFirstName());
        assertEquals("Onah", keycloakUserCaptor.getValue().getLastName());
        assertEquals(Boolean.FALSE, keycloakUserCaptor.getValue().isEmailVerified());

        verify(userResource).sendVerifyEmail();
        verify(userResource).logout();
        verify(userRepository).save(appUser);
        assertEquals(EMAIL, appUser.getEmail());
        assertEquals("Sylvester", appUser.getFirstName());
        assertEquals("Onah", appUser.getLastName());
    }

    @Test
    void deleteUserDeletesKeycloakUserAndLocalUser() {
        User appUser = User.builder().id(USER_ID).build();

        mockRealmUsers();
        when(userRepository.findUserById(USER_ID)).thenReturn(Optional.of(appUser));

        authService.deleteUser(USER_ID);

        verify(usersResource).delete(USER_ID);
        verify(userRepository).delete(appUser);
    }

    @Test
    void forgotPasswordSendsUpdatePasswordActionEmail() {
        mockRealmUsers();
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of(keycloakUser(false)));
        when(usersResource.get(USER_ID)).thenReturn(userResource);

        authService.forgotPassword(EMAIL);

        verify(userResource).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

    @Test
    void forgotPasswordThrowsWhenUserDoesNotExist() {
        mockRealmUsers();
        when(usersResource.searchByEmail(EMAIL, true)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> authService.forgotPassword(EMAIL));
    }

    @Test
    void loginPostsPasswordGrantAndStoresReturnedTokens() {
        TokenResponse tokenResponse = tokenResponse("access-token", "refresh-token");
        mockTokenPost(tokenUrl(), tokenResponse);

        TokenResponse response = authService.login(new LoginRequest(EMAIL, "password123"));

        assertEquals(tokenResponse, response);
        MultiValueMap<String, String> form = capturedForm();
        assertEquals("password", form.getFirst("grant_type"));
        assertEquals(CLIENT_ID, form.getFirst("client_id"));
        assertEquals(CLIENT_SECRET, form.getFirst("client_secret"));
        assertEquals(EMAIL, form.getFirst("username"));
        assertEquals("password123", form.getFirst("password"));
        verify(tokenRepository).storeTokens(EMAIL, "access-token", "refresh-token", 300_000L, 1_800_000L);
    }

    @Test
    void refreshPostsRefreshGrantRotatesTokensAndReturnsTokenResponse() {
        TokenResponse tokenResponse = tokenResponse("new-access-token", "new-refresh-token");
        mockTokenPost(tokenUrl(), tokenResponse);

        TokenResponse response = authService.refresh("old-refresh-token", EMAIL);

        assertEquals(tokenResponse, response);
        MultiValueMap<String, String> form = capturedForm();
        assertEquals("refresh_token", form.getFirst("grant_type"));
        assertEquals(CLIENT_ID, form.getFirst("client_id"));
        assertEquals(CLIENT_SECRET, form.getFirst("client_secret"));
        assertEquals("old-refresh-token", form.getFirst("refresh_token"));
        verify(tokenRepository).removeAllTokens(EMAIL, 300_000L, 1_800_000L);
        verify(tokenRepository).storeTokens(EMAIL, "new-access-token", "new-refresh-token", 300_000L, 1_800_000L);
    }

    @Test
    void logoutPostsLogoutGrantAndRemovesStoredTokens() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(logoutUrl())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());
        when(tokenRepository.getAccessToken(EMAIL)).thenReturn("access-token");
        when(tokenRepository.getRefreshToken(EMAIL)).thenReturn("refresh-token");
        when(tokenRepository.remainingLifetime("access-token")).thenReturn(120L);
        when(tokenRepository.remainingLifetime("refresh-token")).thenReturn(600L);

        authService.logout(EMAIL, "refresh-token");

        MultiValueMap<String, String> form = capturedForm();
        assertEquals(CLIENT_ID, form.getFirst("client_id"));
        assertEquals(CLIENT_SECRET, form.getFirst("client_secret"));
        assertEquals("refresh-token", form.getFirst("refresh_token"));
        verify(tokenRepository).removeAllTokens(EMAIL, 120L, 600L);
    }

    private void mockRealmUsers() {
        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    private void mockTokenPost(String url, TokenResponse response) {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(url)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(MultiValueMap.class))).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(TokenResponse.class)).thenReturn(response);
    }

    @SuppressWarnings("unchecked")
    private MultiValueMap<String, String> capturedForm() {
        ArgumentCaptor<MultiValueMap<String, String>> formCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(requestBodyUriSpec).body(formCaptor.capture());
        return formCaptor.getValue();
    }

    private CreateUserRequest createUserRequest() {
        return new CreateUserRequest("sylvester", EMAIL, "Sylvester", "Onah", "password123");
    }

    private UserRepresentation keycloakUser(boolean emailVerified) {
        UserRepresentation user = new UserRepresentation();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setEmailVerified(emailVerified);
        return user;
    }

    private TokenResponse tokenResponse(String accessToken, String refreshToken) {
        return new TokenResponse(accessToken, refreshToken, 300L, 1800L, "Bearer");
    }

    private String tokenUrl() {
        return "http://localhost:8079/realms/" + REALM + "/protocol/openid-connect/token";
    }

    private String logoutUrl() {
        return "http://localhost:8079/realms/" + REALM + "/protocol/openid-connect/logout";
    }
}
