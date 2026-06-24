package com.sylvester.jobservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JobServiceApplicationTests {

    @Test
    void applicationCanBeInstantiated() {
        assertDoesNotThrow(JobServiceApplication::new);
    }

}
