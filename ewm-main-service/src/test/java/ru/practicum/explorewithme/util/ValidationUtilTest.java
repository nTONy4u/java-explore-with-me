package ru.practicum.explorewithme.util;

import org.junit.jupiter.api.Test;
import ru.practicum.explorewithme.exception.ConflictException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilTest {

    @Test
    void validateEventDate_whenValid_shouldNotThrow() {
        LocalDateTime validDate = LocalDateTime.now().plusHours(3);

        assertDoesNotThrow(() ->
                ValidationUtil.validateEventDate(validDate, "Event date"));
    }

    @Test
    void validateEventDate_whenTooClose_shouldThrowIllegalArgumentException() {
        LocalDateTime invalidDate = LocalDateTime.now().plusMinutes(30);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtil.validateEventDate(invalidDate, "Event date"));

        assertTrue(exception.getMessage().contains("at least 2 hours from now"));
    }

    @Test
    void validateEventDate_whenNull_shouldThrowIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtil.validateEventDate(null, "Event date"));

        assertEquals("Event date must not be null", exception.getMessage());
    }

    @Test
    void validateEventDateForUserUpdate_whenValid_shouldNotThrow() {
        LocalDateTime newDate = LocalDateTime.now().plusHours(3);
        LocalDateTime existingDate = LocalDateTime.now().plusDays(1);

        assertDoesNotThrow(() ->
                ValidationUtil.validateEventDateForUserUpdate(newDate, existingDate));
    }

    @Test
    void validateEventDateForUserUpdate_whenTooClose_shouldThrowConflictException() {
        LocalDateTime newDate = LocalDateTime.now().plusMinutes(30);
        LocalDateTime existingDate = LocalDateTime.now().plusDays(1);

        assertThrows(ConflictException.class, () ->
                ValidationUtil.validateEventDateForUserUpdate(newDate, existingDate));
    }

    @Test
    void validateEventDateForAdminPublish_whenValid_shouldNotThrow() {
        LocalDateTime eventDate = LocalDateTime.now().plusHours(2);

        assertDoesNotThrow(() ->
                ValidationUtil.validateEventDateForAdminPublish(eventDate));
    }

    @Test
    void validateEventDateForAdminPublish_whenTooClose_shouldThrowConflictException() {
        LocalDateTime eventDate = LocalDateTime.now().plusMinutes(30);

        assertThrows(ConflictException.class, () ->
                ValidationUtil.validateEventDateForAdminPublish(eventDate));
    }

    @Test
    void validateStringLength_whenValid_shouldNotThrow() {
        assertDoesNotThrow(() ->
                ValidationUtil.validateStringLength("Valid string", "Field", 1, 100));
    }

    @Test
    void validateStringLength_whenTooShort_shouldThrowIllegalArgumentException() {
        String shortString = "a";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtil.validateStringLength(shortString, "Field", 5, 100));

        assertTrue(exception.getMessage().contains("between 5 and 100 characters"));
    }

    @Test
    void validateStringLength_whenTooLong_shouldThrowIllegalArgumentException() {
        String longString = "a".repeat(101);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ValidationUtil.validateStringLength(longString, "Field", 1, 100));

        assertTrue(exception.getMessage().contains("between 1 and 100 characters"));
    }

    @Test
    void validateStringLength_whenNull_shouldNotThrow() {
        assertDoesNotThrow(() ->
                ValidationUtil.validateStringLength(null, "Field", 1, 100));
    }
}