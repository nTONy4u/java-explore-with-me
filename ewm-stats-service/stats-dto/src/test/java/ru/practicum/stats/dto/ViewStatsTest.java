package ru.practicum.stats.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewStatsTest {

    @Test
    void testBuilderAndGetters() {
        ViewStats stats = ViewStats.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(42L)
                .build();

        assertEquals("ewm-main-service", stats.getApp());
        assertEquals("/events/1", stats.getUri());
        assertEquals(42L, stats.getHits());
    }

    @Test
    void testEqualsAndHashCode() {
        ViewStats stats1 = ViewStats.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(42L)
                .build();

        ViewStats stats2 = ViewStats.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(42L)
                .build();

        ViewStats stats3 = ViewStats.builder()
                .app("other-service")
                .uri("/events/1")
                .hits(42L)
                .build();

        assertEquals(stats1, stats2);
        assertNotEquals(stats1, stats3);
        assertEquals(stats1.hashCode(), stats2.hashCode());
    }

    @Test
    void testToString() {
        ViewStats stats = ViewStats.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(42L)
                .build();

        String str = stats.toString();
        assertTrue(str.contains("ewm-main-service"));
        assertTrue(str.contains("/events/1"));
        assertTrue(str.contains("42"));
    }
}