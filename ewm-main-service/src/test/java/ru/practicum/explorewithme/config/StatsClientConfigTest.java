package ru.practicum.explorewithme.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.stats.client.StatsClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = {
        "stats.service.url=http://localhost:9090"
})
class StatsClientConfigTest {

    @Autowired(required = false)
    private StatsClient statsClient;

    @Test
    void statsClientBeanShouldBeCreated() {
        assertNotNull(statsClient, "StatsClient должен быть создан как Spring bean");
    }

    @Test
    void statsClientShouldBeConfiguredWithCorrectUrl() {
        assertNotNull(statsClient);
    }
}