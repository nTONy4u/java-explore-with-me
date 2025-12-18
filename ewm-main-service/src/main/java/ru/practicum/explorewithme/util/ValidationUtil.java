package ru.practicum.explorewithme.util;

import ru.practicum.explorewithme.exception.ConflictException;

import java.time.LocalDateTime;

public class ValidationUtil {

    public static void validateEventDate(LocalDateTime eventDate, String fieldName) {
        if (eventDate == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        LocalDateTime minDateTime = LocalDateTime.now().plusHours(2);
        if (eventDate.isBefore(minDateTime)) {
            throw new IllegalArgumentException(
                    fieldName + " must be at least 2 hours from now. Current: " + eventDate
            );
        }
    }

    public static void validateEventDateForUserUpdate(LocalDateTime newEventDate,
                                                      LocalDateTime existingEventDate) {
        if (newEventDate == null) {
            return;
        }

        if (newEventDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date cannot be in the past");
        }

        LocalDateTime minDateTime = LocalDateTime.now().plusHours(2);
        if (newEventDate.isBefore(minDateTime)) {
            throw new ConflictException(
                    "Дата начала изменяемого события должна быть не ранее чем за 2 часа от текущего времени"
            );
        }
    }

    public static void validateEventDateForAdminPublish(LocalDateTime eventDate) {
        if (eventDate == null) {
            throw new IllegalArgumentException("Event date must not be null");
        }

        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        if (eventDate.isBefore(oneHourFromNow)) {
            throw new ConflictException(
                    "Нельзя опубликовать событие, которое начинается менее чем через час"
            );
        }
    }

    public static void validateEventDateForAdminUpdate(LocalDateTime newEventDate,
                                                       LocalDateTime existingEventDate,
                                                       LocalDateTime publishedOn) {
        if (newEventDate == null) {
            return;
        }

        if (newEventDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Event date cannot be in the past");
        }

        if (publishedOn != null) {
            LocalDateTime minAllowedDate = publishedOn.plusHours(1);
            if (newEventDate.isBefore(minAllowedDate)) {
                throw new ConflictException(
                        "Дата начала изменяемого события должна быть не ранее чем за час от даты публикации"
                );
            }
        }
    }

    public static void validateStringLength(String value, String fieldName, int min, int max) {
        if (value == null) {
            return;
        }

        int length = value.length();
        if (length < min || length > max) {
            throw new IllegalArgumentException(
                    String.format("%s length must be between %d and %d characters. Current: %d",
                            fieldName, min, max, length)
            );
        }
    }
}