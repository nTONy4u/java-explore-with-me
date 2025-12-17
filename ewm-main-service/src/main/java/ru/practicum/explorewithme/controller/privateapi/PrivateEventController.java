package ru.practicum.explorewithme.controller.privateapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.dto.EventFullDto;
import ru.practicum.explorewithme.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.dto.EventShortDto;
import ru.practicum.explorewithme.dto.NewEventDto;
import ru.practicum.explorewithme.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.dto.validation.ValidationGroups;
import ru.practicum.explorewithme.service.EventService;
import ru.practicum.explorewithme.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
public class PrivateEventController {

    private final EventService eventService;
    private final RequestService requestService;

    @GetMapping
    public List<EventShortDto> getEvents(@PathVariable Long userId,
                                         @RequestParam(defaultValue = "0") Integer from,
                                         @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /users/{}/events: получение событий пользователя", userId);
        return eventService.getUserEvents(userId, from, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Long userId,
                                    @Valid @RequestBody NewEventDto newEventDto) {
        log.info("POST /users/{}/events: создание события", userId);
        return eventService.createEvent(userId, newEventDto);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(@PathVariable Long userId,
                                 @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}: получение события", userId, eventId);
        return eventService.getUserEventById(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable Long userId,
                                    @PathVariable Long eventId,
                                    @Validated(ValidationGroups.OnUpdate.class)
                                    @RequestBody UpdateEventUserRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}: обновление события", userId, eventId);
        return eventService.updateEventByUser(userId, eventId, updateRequest);
    }

    @GetMapping("/{eventId}/requests")
    public List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId,
                                                              @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests: получение участников события", userId, eventId);
        return requestService.getEventParticipants(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}/requests: обновление статуса заявок", userId, eventId);
        return requestService.updateRequestStatus(userId, eventId, updateRequest);
    }
}