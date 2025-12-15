package ru.practicum.stats.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;
import ru.practicum.stats.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    private EndpointHit endpointHit;
    private ViewStats viewStats;

    @BeforeEach
    void setUp() {
        endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .build();

        viewStats = ViewStats.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .hits(10L)
                .build();
    }

    @Test
    void saveHit_whenValid_shouldReturn201() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isCreated());
    }

    @Test
    void saveHit_whenInvalidApp_shouldReturn400() throws Exception {
        endpointHit.setApp("");

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveHit_whenInvalidUri_shouldReturn400() throws Exception {
        endpointHit.setUri("");

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveHit_whenInvalidIp_shouldReturn400() throws Exception {
        endpointHit.setIp("");

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointHit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_whenValidParams_shouldReturn200() throws Exception {
        when(statsService.getStats(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(viewStats));

        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(10));
    }

    @Test
    void getStats_whenEndBeforeStart_shouldReturn400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2024-12-31 23:59:59")
                        .param("end", "2024-01-01 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_whenMissingStart_shouldReturn400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_whenMissingEnd_shouldReturn400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStats_withUrisFilter_shouldReturnFiltered() throws Exception {
        when(statsService.getStats(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(viewStats));

        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-12-31 23:59:59")
                        .param("uris", "/events/1", "/events/2"))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_withUniqueTrue_shouldReturnUniqueStats() throws Exception {
        when(statsService.getStats(any(), any(), any(), eq(true)))
                .thenReturn(List.of(viewStats));

        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-12-31 23:59:59")
                        .param("unique", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_withInvalidDateFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "invalid-date")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isBadRequest());
    }
}