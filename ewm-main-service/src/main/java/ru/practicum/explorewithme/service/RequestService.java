package ru.practicum.explorewithme.service;

import ru.practicum.explorewithme.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequest);
}