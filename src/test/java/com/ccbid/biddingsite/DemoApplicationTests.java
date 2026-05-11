package com.ccbid.biddingsite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:context-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.h2.console.enabled=false"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
