package com.sylvester.applicationservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApplicationServiceApplicationTests {

    @Test
    void applicationCanBeInstantiated() {
        assertDoesNotThrow(ApplicationServiceApplication::new);
    }

}
