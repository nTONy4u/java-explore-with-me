package ru.practicum.stats.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class StatsClientErrorHandlingTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void saveHit_whenServerReturnsError_shouldThrowException() {
        StatsClient client = new StatsClient("http://localhost:9090");

        RestTemplate restTemplate;
        try {
            var field = StatsClient.class.getDeclaredField("rest");
            field.setAccessible(true);
            restTemplate = (RestTemplate) field.get(client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

        EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockServer.expect(requestTo("http://localhost:9090/hit"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(RuntimeException.class, () -> client.saveHit(hit));

        mockServer.verify();
    }

    @Test
    void getStats_whenInvalidResponse_shouldReturnEmptyList() {
        StatsClient client = new StatsClient("http://localhost:9090");

        RestTemplate restTemplate;
        try {
            var field = StatsClient.class.getDeclaredField("rest");
            field.setAccessible(true);
            restTemplate = (RestTemplate) field.get(client);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        String startStr = start.format(formatter);
        String endStr = end.format(formatter);

        String expectedUrl = String.format(
                "http://localhost:9090/stats?start=%s&end=%s&unique=false",
                startStr.replace(" ", "%20"),
                endStr.replace(" ", "%20")
        );

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        var result = client.getStats(start, end, null, false);

        assertTrue(result.isEmpty());
        mockServer.verify();
    }
}