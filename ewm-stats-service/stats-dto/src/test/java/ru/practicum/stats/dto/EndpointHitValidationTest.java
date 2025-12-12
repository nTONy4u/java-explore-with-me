package ru.practicum.stats.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointHitValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void whenValidEndpointHit_thenNoViolations() {
        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenAppIsBlank_thenValidationFails() {
        EndpointHit hit = EndpointHit.builder()
                .app("")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("App must not be blank", violations.iterator().next().getMessage());
    }

    @Test
    void whenUriIsBlank_thenValidationFails() {
        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);
        assertFalse(violations.isEmpty());
    }

    @Test
    void whenIpIsBlank_thenValidationFails() {
        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("")
                .timestamp(LocalDateTime.now())
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);
        assertFalse(violations.isEmpty());
    }

    @Test
    void whenTimestampIsNull_thenValidationFails() {
        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(null)
                .build();

        Set<ConstraintViolation<EndpointHit>> violations = validator.validate(hit);
        assertFalse(violations.isEmpty());
        assertEquals("Timestamp must not be null", violations.iterator().next().getMessage());
    }
}