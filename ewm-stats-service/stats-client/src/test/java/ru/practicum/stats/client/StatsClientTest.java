package ru.practicum.stats.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsClientTest {

    @Test
    void testUrlBuildingWithoutDoubleEncoding() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        String url = UriComponentsBuilder
                .fromHttpUrl("http://localhost:9090/stats")
                .queryParam("start", start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .queryParam("end", end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .queryParam("unique", false)
                .build(false)
                .toUriString();

        assertTrue(url.contains("start=2024-01-01 00:00:00"));
        assertTrue(url.contains("end=2024-12-31 23:59:59"));

        System.out.println("Generated URL: " + url);
    }

    @Test
    void testConstructorAndGetters() {
        StatsClient client = new StatsClient("http://test:8080");
        assertNotNull(client);
    }

    @Test
    void testCreateEndpointHit() {
        EndpointHit hit = EndpointHit.builder()
                .app("test")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        assertNotNull(hit);
        assertEquals("test", hit.getApp());
        assertEquals("/test", hit.getUri());
    }

    @Test
    void testCreateViewStats() {
        ViewStats stats = ViewStats.builder()
                .app("test")
                .uri("/test")
                .hits(10L)
                .build();

        assertNotNull(stats);
        assertEquals("test", stats.getApp());
        assertEquals(10L, stats.getHits());
    }
}