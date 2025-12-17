package ru.practicum.explorewithme.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.dto.EventFullDto;
import ru.practicum.explorewithme.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @Test
    void getEvents_withInvalidState_shouldReturn400() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("states", "INVALID_STATE")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void getEvents_withValidParams_shouldReturn200() throws Exception {
        EventFullDto event = EventFullDto.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test annotation")
                .eventDate(LocalDateTime.now().plusDays(1))
                .build();

        when(eventService.getEventsByAdmin(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/admin/events")
                        .param("states", "PENDING")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void getEvents_withInvalidDateFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("rangeStart", "not-a-date")
                        .param("rangeEnd", "2024-12-31 23:59:59")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }
}