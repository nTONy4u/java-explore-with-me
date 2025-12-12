package ru.practicum.stats.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.model.EndpointHitEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StatsMapperTest {

    @Test
    void toEntity_shouldMapCorrectly() {
        EndpointHit dto = EndpointHit.builder()
                .app("test-app")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();

        EndpointHitEntity entity = StatsMapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("test-app", entity.getApp());
        assertEquals("/test", entity.getUri());
        assertEquals("127.0.0.1", entity.getIp());
        assertEquals(LocalDateTime.of(2024, 1, 1, 12, 0, 0), entity.getTimestamp());
    }

    @Test
    void toEntity_whenNull_shouldReturnNull() {
        EndpointHitEntity entity = StatsMapper.toEntity(null);
        assertNull(entity);
    }
}