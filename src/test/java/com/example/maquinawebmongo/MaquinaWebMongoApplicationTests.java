package com.example.maquinawebmongo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MaquinaWebMongoApplication.class)
@ActiveProfiles("test")
class MaquinaWebMongoApplicationTests {

    @Test
    void contextLoads() {
    }
}
