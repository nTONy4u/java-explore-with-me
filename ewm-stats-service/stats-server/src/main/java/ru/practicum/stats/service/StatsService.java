package ru.practicum.stats.service;

import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void saveHit(EndpointHit endpointHit);

    List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                             List<String> uris, Boolean unique);
}