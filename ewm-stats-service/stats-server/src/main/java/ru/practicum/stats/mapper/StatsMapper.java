package ru.practicum.stats.mapper;

import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.model.EndpointHitEntity;

public class StatsMapper {

    public static EndpointHitEntity toEntity(EndpointHit endpointHit) {
        if (endpointHit == null) {
            return null;
        }

        return EndpointHitEntity.builder()
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(endpointHit.getTimestamp())
                .build();
    }
}