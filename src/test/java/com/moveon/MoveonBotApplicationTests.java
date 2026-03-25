package com.moveon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic context test to verify application starts correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class MoveonBotApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads successfully
    }
}
