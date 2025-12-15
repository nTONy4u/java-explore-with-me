package ru.practicum.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class StatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private EndpointHit endpointHit;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void fullFlow_saveHitAndGetStats() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isCreated());

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(1));
    }

    @Test
    void saveMultipleHitsAndGetUniqueStats() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(endpointHit)));
        }

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(3));

        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(1));
    }
}