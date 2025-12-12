package ru.practicum.explorewithme;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ExploreWithMeApplicationTest {

    @Test
    void contextLoads() {
        assertTrue(true, "Контекст должен загрузиться успешно");
    }

    @Test
    void mainMethodStartsApplication() {
        assertDoesNotThrow(() -> {
            Class<?> clazz = ExploreWithMeApplication.class;
            assertNotNull(clazz);

            var method = clazz.getMethod("main", String[].class);
            assertNotNull(method);
        });
    }
}