package ru.practicum.explorewithme.controller.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.dto.EventShortDto;
import ru.practicum.explorewithme.service.EventService;
import ru.practicum.stats.client.StatsClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private StatsClient statsClient;

    private EventShortDto eventShortDto;

    @BeforeEach
    void setUp() {
        eventShortDto = EventShortDto.builder()
                .id(1L)
                .annotation("Test Annotation")
                .title("Test Event")
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(false)
                .views(10L)
                .confirmedRequests(5L)
                .build();
    }

    @Test
    void getEvents_whenValidParams_shouldReturn200() throws Exception {
        when(eventService.getEventsPublic(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(eventShortDto));

        mockMvc.perform(get("/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void getEvents_whenWithTextFilter_shouldReturnFiltered() throws Exception {
        when(eventService.getEventsPublic(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(eventShortDto));

        mockMvc.perform(get("/events")
                        .param("text", "test")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getEvents_whenInvalidSort_shouldReturn400() throws Exception {
        when(eventService.getEventsPublic(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid sort parameter"));

        mockMvc.perform(get("/events")
                        .param("sort", "INVALID")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvent_whenExists_shouldReturn200() throws Exception {
        when(eventService.getEventPublic(1L)).thenReturn(null);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getEvent_whenInvalidId_shouldReturn400() throws Exception {
        mockMvc.perform(get("/events/invalid"))
                .andExpect(status().isBadRequest());
    }
}