package ru.practicum.explorewithme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.exception.ConflictException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.ParticipationRequest;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.EventState;
import ru.practicum.explorewithme.model.enums.RequestStatus;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.RequestRepository;
import ru.practicum.explorewithme.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    private User user;
    private Event event;
    private ParticipationRequest participationRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        event = Event.builder()
                .id(1L)
                .annotation("Test annotation with more than 20 characters")
                .description("Test description with more than 20 characters")
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(user)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Test Event")
                .build();

        participationRequest = ParticipationRequest.builder()
                .id(1L)
                .created(LocalDateTime.now())
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .build();
    }

    @Test
    void getUserRequests_whenUserExists_shouldReturnRequests() {
        when(requestRepository.findByRequesterId(1L)).thenReturn(List.of(participationRequest));

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(requestRepository).findByRequesterId(1L);
    }

    @Test
    void createRequest_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                requestService.createRequest(999L, 1L));

        assertTrue(exception.getMessage().contains("Пользователь с id=999 не найден"));
    }

    @Test
    void createRequest_whenEventNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                requestService.createRequest(1L, 999L));

        assertTrue(exception.getMessage().contains("Событие с id=999 не найдено"));
    }

    @Test
    void createRequest_whenDuplicateRequest_shouldThrowConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                requestService.createRequest(1L, 1L));

        assertTrue(exception.getMessage().contains("Нельзя добавить повторный запрос"));
    }

    @Test
    void createRequest_whenUserIsInitiator_shouldThrowConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 1L)).thenReturn(false);

        event.setInitiator(user);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                requestService.createRequest(1L, 1L));

        assertTrue(exception.getMessage().contains("Инициатор события не может добавить запрос"));
    }

    @Test
    void createRequest_whenEventNotPublished_shouldThrowConflictException() {
        User anotherUser = User.builder().id(2L).name("Another User").email("another@example.com").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 2L)).thenReturn(false);

        event.setState(EventState.PENDING);
        event.setInitiator(user);

        ConflictException exception = assertThrows(ConflictException.class, () ->
                requestService.createRequest(2L, 1L));

        assertTrue(exception.getMessage().contains("Нельзя участвовать в неопубликованном событии"));
    }

    @Test
    void cancelRequest_whenRequestNotFound_shouldThrowNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                requestService.cancelRequest(1L, 999L));

        assertTrue(exception.getMessage().contains("Запрос с id=999 не найден"));
    }

    @Test
    void cancelRequest_whenRequestNotBelongsToUser_shouldThrowNotFoundException() {
        User anotherUser = User.builder().id(2L).name("Another User").build();
        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .requester(anotherUser)
                .build();

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                requestService.cancelRequest(1L, 1L));

        assertTrue(exception.getMessage().contains("Запрос не принадлежит пользователю"));
    }

    @Test
    void getEventParticipants_whenEventNotFound_shouldThrowNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                requestService.getEventParticipants(1L, 999L));

        assertTrue(exception.getMessage().contains("Событие с id=999 не найдено"));
    }

    @Test
    void getEventParticipants_whenUserNotInitiator_shouldThrowNotFoundException() {
        User anotherUser = User.builder().id(2L).name("Another User").build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        event.setInitiator(anotherUser);

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
                requestService.getEventParticipants(1L, 1L));

        assertTrue(exception.getMessage().contains("Событие не принадлежит пользователю"));
    }

    @Test
    void getEventParticipants_whenValid_shouldReturnParticipants() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventId(1L)).thenReturn(List.of(participationRequest));

        List<ParticipationRequestDto> result = requestService.getEventParticipants(1L, 1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void createRequest_whenValidAndNoModeration_shouldReturnConfirmedRequest() {
        User anotherUser = User.builder().id(2L).name("Another User").email("another@example.com").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByEventIdAndRequesterId(1L, 2L)).thenReturn(false);

        event.setRequestModeration(false);
        event.setInitiator(user);
        event.setState(EventState.PUBLISHED);

        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest req = invocation.getArgument(0);
            req.setId(1L);
            return req;
        });

        ParticipationRequestDto result = requestService.createRequest(2L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(RequestStatus.CONFIRMED, result.getStatus());
    }
}