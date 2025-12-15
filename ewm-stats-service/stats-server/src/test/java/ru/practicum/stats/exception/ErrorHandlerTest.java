package ru.practicum.stats.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stats.controller.StatsController;
import ru.practicum.stats.service.StatsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class ErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService statsService;

    @Test
    void handleIllegalArgumentException_shouldReturn400() throws Exception {
        when(statsService.getStats(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("End date must be after start date"));

        mockMvc.perform(get("/stats")
                        .param("start", "2024-12-31 23:59:59")
                        .param("end", "2024-01-01 00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldReturn400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "not-a-date")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleMissingServletRequestParameterException_shouldReturn400() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));
    }

    @Test
    void handleHttpMessageNotReadableException_shouldReturn400() throws Exception {
        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturn400() throws Exception {
        String invalidHit = "{\"app\":\"\",\"uri\":\"/test\",\"ip\":\"127.0.0.1\",\"timestamp\":\"2024-01-01 12:00:00\"}";

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidHit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."));
    }

    @Test
    void handleAllUncaughtException_shouldReturn500() throws Exception {
        when(statsService.getStats(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/stats")
                        .param("start", "2024-01-01 00:00:00")
                        .param("end", "2024-12-31 23:59:59"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"));
    }
}