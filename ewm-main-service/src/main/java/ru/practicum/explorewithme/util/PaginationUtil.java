package ru.practicum.explorewithme.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtil {

    private static final int DEFAULT_FROM = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 1000;

    public static Pageable createPageRequest(Integer from, Integer size) {
        return createPageRequest(from, size, null);
    }

    public static Pageable createPageRequest(Integer from, Integer size, Sort sort) {
        if (from == null || from < 0) {
            from = DEFAULT_FROM;
        }

        if (size == null || size <= 0) {
            size = DEFAULT_SIZE;
        }

        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }

        int page = from / size;

        return sort != null
                ? PageRequest.of(page, size, sort)
                : PageRequest.of(page, size);
    }

    public static void validatePaginationParams(Integer from, Integer size) {
        if (from == null || from < 0) {
            throw new IllegalArgumentException("Параметр 'from' должен быть неотрицательным");
        }
        if (size == null || size <= 0) {
            throw new IllegalArgumentException("Параметр 'size' должен быть положительным");
        }
        if (size > MAX_SIZE) {
            throw new IllegalArgumentException("Параметр 'size' не может превышать " + MAX_SIZE);
        }
    }
}