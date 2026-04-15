package com.sellglass;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Integration test — requires running PostgreSQL and Redis. Run manually with docker-compose up.")
class SellGlassApplicationTests {

    @Test
    void contextLoads() {
    }
}
