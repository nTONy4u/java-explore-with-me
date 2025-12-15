package ru.practicum.stats.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.model.EndpointHitEntity;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsRepository statsRepository;

    @InjectMocks
    private StatsServiceImpl statsService;

    private EndpointHit endpointHit;
    private EndpointHitEntity entity;

    @BeforeEach
    void setUp() {
        endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();

        entity = EndpointHitEntity.builder()
                .id(1L)
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();
    }

    @Test
    void saveHit_shouldSaveAndReturnHit() {
        when(statsRepository.save(any(EndpointHitEntity.class))).thenReturn(entity);

        statsService.saveHit(endpointHit);

        verify(statsRepository, times(1)).save(any(EndpointHitEntity.class));
    }

    @Test
    void getStats_whenUrisNull_shouldPassNullToRepository() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        List<Object[]> mockResult = List.of();

        when(statsRepository.getStats(start, end, null)).thenReturn(mockResult);

        List<ViewStats> result = statsService.getStats(start, end, null, false);

        assertTrue(result.isEmpty());
        verify(statsRepository, times(1)).getStats(start, end, null);
    }

    @Test
    void getStats_whenUrisEmpty_shouldPassEmptyList() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59, 59);
        List<String> uris = List.of();
        List<Object[]> mockResult = List.of();

        when(statsRepository.getStats(start, end, uris)).thenReturn(mockResult);

        List<ViewStats> result = statsService.getStats(start, end, uris, false);

        assertTrue(result.isEmpty());
        verify(statsRepository, times(1)).getStats(start, end, uris);
    }
}