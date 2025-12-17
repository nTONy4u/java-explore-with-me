package ru.practicum.explorewithme.service;

import ru.practicum.explorewithme.dto.EventFullDto;
import ru.practicum.explorewithme.dto.EventShortDto;
import ru.practicum.explorewithme.dto.NewEventDto;
import ru.practicum.explorewithme.dto.UpdateEventAdminRequest;
import ru.practicum.explorewithme.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventFullDto createEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states,
                                        List<Long> categories, LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd, Integer from, Integer size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);

    List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                        LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                        Boolean onlyAvailable, String sort, Integer from, Integer size);

    EventFullDto getEventPublic(Long eventId);
}