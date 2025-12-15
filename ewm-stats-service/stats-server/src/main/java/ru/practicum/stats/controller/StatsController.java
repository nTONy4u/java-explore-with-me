package ru.practicum.stats.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.service.StatsService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveHit(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("POST /hit: app={}, uri={}, ip={}",
                endpointHit.getApp(), endpointHit.getUri(), endpointHit.getIp());
        statsService.saveHit(endpointHit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getStats(
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime start,

            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime end,

            @RequestParam(required = false) String[] uris,
            @RequestParam(defaultValue = "false") Boolean unique) {

        log.info("GET /stats?start={}&end={}&uris={}&unique={}", start, end, uris, unique);

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<String> uriList = (uris != null) ? Arrays.asList(uris) : null;

        return statsService.getStats(start, end, uriList, unique);
    }
}