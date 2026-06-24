package com.sylvester.companyservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CompanyServiceApplicationTests {

    @Test
    void applicationCanBeInstantiated() {
        assertDoesNotThrow(CompanyServiceApplication::new);
    }

}
