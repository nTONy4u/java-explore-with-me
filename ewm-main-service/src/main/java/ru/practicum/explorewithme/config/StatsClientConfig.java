package ru.practicum.explorewithme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.stats.client.StatsClient;

@Configuration
public class StatsClientConfig {
    @Value("${stats.service.url}")
    private String serverUrl;

    @Bean
    public StatsClient statsClient() {
        return new StatsClient(serverUrl);
    }
}