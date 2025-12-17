package ru.practicum.explorewithme.controller.privateapi;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Slf4j
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId) {
        log.info("GET /users/{}/requests: получение запросов пользователя", userId);
        return requestService.getUserRequests(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam @NotNull Long eventId) {
        log.info("POST /users/{}/requests: создание запроса на участие в событии {}", userId, eventId);
        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel: отмена запроса", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}