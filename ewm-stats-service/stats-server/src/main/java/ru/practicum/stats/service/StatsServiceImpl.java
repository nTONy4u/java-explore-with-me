package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.model.EndpointHitEntity;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHit endpointHit) {
        EndpointHitEntity entity = StatsMapper.toEntity(endpointHit);
        statsRepository.save(entity);
        log.info("Saved hit: {}", endpointHit);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    List<String> uris, Boolean unique) {
        log.info("Getting stats from {} to {} for uris: {}, unique: {}",
                start, end, uris, unique);

        List<Object[]> results;
        if (unique != null && unique) {
            results = statsRepository.getUniqueStats(start, end, uris);
        } else {
            results = statsRepository.getStats(start, end, uris);
        }

        return results.stream()
                .map(row -> ViewStats.builder()
                        .app((String) row[0])
                        .uri((String) row[1])
                        .hits((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }
}