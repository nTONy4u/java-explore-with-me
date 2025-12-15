package ru.practicum.stats.client;

import org.junit.jupiter.api.Test;
import ru.practicum.stats.dto.EndpointHit;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StatsClientIntegrationTest {

    @Test
    void statsClientCanBeCreated() {
        StatsClient client = new StatsClient("http://localhost:9090");
        assertNotNull(client);

        EndpointHit hit = EndpointHit.builder()
                .app("test")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        assertNotNull(hit);
        assertEquals("test", hit.getApp());
    }
}