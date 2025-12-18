package ru.practicum.explorewithme.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationUtilTest {

    @Test
    void createPageRequest_withDefaultParams_shouldReturnDefaultPage() {
        Pageable pageable = PaginationUtil.createPageRequest(null, null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
    }

    @Test
    void createPageRequest_withCustomParams_shouldReturnCorrectPage() {
        Pageable pageable = PaginationUtil.createPageRequest(20, 5);

        assertEquals(4, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
    }

    @Test
    void createPageRequest_withSort_shouldReturnSortedPage() {
        Sort sort = Sort.by("name").ascending();
        Pageable pageable = PaginationUtil.createPageRequest(0, 10, sort);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertTrue(pageable.getSort().getOrderFor("name").isAscending());
    }

    @Test
    void validatePaginationParams_whenValid_shouldNotThrow() {
        assertDoesNotThrow(() -> PaginationUtil.validatePaginationParams(0, 10));
        assertDoesNotThrow(() -> PaginationUtil.validatePaginationParams(100, 50));
    }

    @Test
    void validatePaginationParams_whenInvalidFrom_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtil.validatePaginationParams(-1, 10));
    }

    @Test
    void validatePaginationParams_whenInvalidSize_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtil.validatePaginationParams(0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtil.validatePaginationParams(0, -5));

        assertThrows(IllegalArgumentException.class,
                () -> PaginationUtil.validatePaginationParams(0, 2000));
    }
}