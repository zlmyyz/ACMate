package com.itnoduck.acmate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class AcMateApplicationTests {

    @Test
    void contextLoads() {
    }

}
