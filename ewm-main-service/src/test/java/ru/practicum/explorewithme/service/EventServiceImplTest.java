package ru.practicum.explorewithme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.dto.LocationDto;
import ru.practicum.explorewithme.dto.NewEventDto;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.Category;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.repository.CategoryRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.UserRepository;
import ru.practicum.stats.client.StatsClient;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Category category;
    private NewEventDto newEventDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        newEventDto = NewEventDto.builder()
                .annotation("Valid annotation with more than twenty characters length")
                .description("Valid description that contains more than twenty characters as required by validation rules")
                .category(1L)
                .eventDate(LocalDateTime.now().plusHours(3))
                .location(LocationDto.builder().lat(55.754167f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event")
                .build();
    }

    @Test
    void createEvent_whenValid_shouldCreateEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(1L);
            return event;
        });

        var result = eventService.createEvent(1L, newEventDto);

        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
        assertEquals("PENDING", result.getState().toString());
    }

    @Test
    void createEvent_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                eventService.createEvent(1L, newEventDto));
    }

    @Test
    void createEvent_whenCategoryNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                eventService.createEvent(1L, newEventDto));
    }

    @Test
    void createEvent_whenEventDateTooClose_shouldThrowIllegalArgumentException() {
        newEventDto.setEventDate(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThrows(IllegalArgumentException.class, () ->
                eventService.createEvent(1L, newEventDto));
    }

    @Test
    void getEventPublic_whenEventNotPublished_shouldThrowNotFoundException() {
        Event event = Event.builder()
                .id(1L)
                .state(ru.practicum.explorewithme.model.enums.EventState.PENDING)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(NotFoundException.class, () ->
                eventService.getEventPublic(1L));
    }

    @Test
    void getEventPublic_whenEventNotFound_shouldThrowNotFoundException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                eventService.getEventPublic(1L));
    }
}