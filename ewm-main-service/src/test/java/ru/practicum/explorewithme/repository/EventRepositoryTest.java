package ru.practicum.explorewithme.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.explorewithme.model.Category;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Category category;
    private Event event;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();
        user = userRepository.save(user);

        category = Category.builder()
                .name("Концерты")
                .build();
        category = categoryRepository.save(category);

        event = Event.builder()
                .annotation("Test annotation")
                .description("Test description")
                .category(category)
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(user)
                .lat(55.754167f)
                .lon(37.62f)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .title("Test Event")
                .build();
        event = eventRepository.save(event);
    }

    @Test
    void findByInitiatorId_shouldReturnUserEvents() {
        List<Event> events = eventRepository.findByInitiatorId(user.getId(), PageRequest.of(0, 10));

        assertFalse(events.isEmpty());
        assertEquals(event.getId(), events.get(0).getId());
        assertEquals(user.getId(), events.get(0).getInitiator().getId());
    }

    @Test
    void findByIdAndInitiatorId_whenExists_shouldReturnEvent() {
        var result = eventRepository.findByIdAndInitiatorId(event.getId(), user.getId());

        assertTrue(result.isPresent());
        assertEquals(event.getId(), result.get().getId());
    }

    @Test
    void findByIdAndInitiatorId_whenNotExists_shouldReturnEmpty() {
        var result = eventRepository.findByIdAndInitiatorId(999L, user.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void existsByCategoryId_whenEventsExist_shouldReturnTrue() {
        boolean exists = eventRepository.existsByCategoryId(category.getId());

        assertTrue(exists);
    }

    @Test
    void existsByCategoryId_whenNoEvents_shouldReturnFalse() {
        Category newCategory = categoryRepository.save(
                Category.builder().name("Новая категория").build()
        );

        boolean exists = eventRepository.existsByCategoryId(newCategory.getId());

        assertFalse(exists);
    }

    @Test
    void saveEvent_shouldGenerateId() {
        Event newEvent = Event.builder()
                .annotation("New annotation")
                .description("New description")
                .category(category)
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(2))
                .initiator(user)
                .lat(55.754167f)
                .lon(37.62f)
                .paid(true)
                .participantLimit(20)
                .requestModeration(false)
                .state(EventState.PENDING)
                .title("New Event")
                .build();

        Event saved = eventRepository.save(newEvent);

        assertNotNull(saved.getId());
        assertEquals("New Event", saved.getTitle());
        assertEquals(EventState.PENDING, saved.getState());
    }
}