package com.sylvester.springauthkeycloak;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuthServiceApplicationTests {

    @Test
    void applicationCanBeInstantiated() {
        assertDoesNotThrow(AuthServiceApplication::new);
    }

}
