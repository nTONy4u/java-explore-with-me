package ru.practicum.stats.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class StatsClient {
    private final RestTemplate rest;
    private final String serverUrl;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.rest = new RestTemplate();
    }

    public void saveHit(EndpointHit hit) {
        String url = serverUrl + "/hit";

        ResponseEntity<Void> response = rest.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(hit, createHeaders()),
                Void.class
        );

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new RuntimeException("Expected 201 Created, but got: " + response.getStatusCode());
        }
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end,
                                    @Nullable List<String> uris,
                                    @Nullable Boolean unique) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(serverUrl + "/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter));

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        String url = builder.build().toUriString();

        try {
            ResponseEntity<ViewStats[]> response = rest.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    ViewStats[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }

            return List.of();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("Error getting stats: " + e.getMessage());
            return List.of();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            return List.of();
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}