package ru.practicum.stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointHitJacksonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testSerialization() throws Exception {
        EndpointHit hit = EndpointHit.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();

        String json = objectMapper.writeValueAsString(hit);

        assertTrue(json.contains("\"app\":\"ewm-main-service\""));
        assertTrue(json.contains("\"uri\":\"/events/1\""));
        assertTrue(json.contains("\"ip\":\"192.168.1.1\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01 12:00:00\""));
    }

    @Test
    void testDeserialization() throws Exception {
        String json = """
                {
                    "app": "ewm-main-service",
                    "uri": "/events/1",
                    "ip": "192.168.1.1",
                    "timestamp": "2024-01-01 12:00:00"
                }
                """;

        EndpointHit hit = objectMapper.readValue(json, EndpointHit.class);

        assertEquals("ewm-main-service", hit.getApp());
        assertEquals("/events/1", hit.getUri());
        assertEquals("192.168.1.1", hit.getIp());
        assertEquals(LocalDateTime.of(2024, 1, 1, 12, 0, 0), hit.getTimestamp());
    }

    @Test
    void testDateFormat() throws Exception {
        String[] validDates = {
                "\"2024-01-01 12:00:00\"",
                "\"2024-12-31 23:59:59\"",
                "\"2022-09-06 11:00:23\""
        };

        for (String dateJson : validDates) {
            String json = """
                    {
                        "app": "test",
                        "uri": "/test",
                        "ip": "127.0.0.1",
                        "timestamp": %s
                    }
                    """.formatted(dateJson);

            EndpointHit hit = objectMapper.readValue(json, EndpointHit.class);
            assertNotNull(hit.getTimestamp());
        }
    }
}