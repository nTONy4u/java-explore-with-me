package ru.practicum.stats.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testSerializationDeserialization() throws Exception {
        ApiError error = ApiError.builder()
                .errors(List.of("Error 1", "Error 2"))
                .message("Validation failed")
                .reason("Incorrectly made request.")
                .status("BAD_REQUEST")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();

        String json = objectMapper.writeValueAsString(error);
        assertTrue(json.contains("\"status\":\"BAD_REQUEST\""));
        assertTrue(json.contains("\"message\":\"Validation failed\""));
        assertTrue(json.contains("\"timestamp\":\"2024-01-01 12:00:00\""));

        ApiError deserialized = objectMapper.readValue(json, ApiError.class);
        assertEquals("BAD_REQUEST", deserialized.getStatus());
        assertEquals("Validation failed", deserialized.getMessage());
        assertEquals(2, deserialized.getErrors().size());
    }
}