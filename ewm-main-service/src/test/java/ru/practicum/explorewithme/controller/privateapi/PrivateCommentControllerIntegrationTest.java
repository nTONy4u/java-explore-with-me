package ru.practicum.explorewithme.controller.privateapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.NewCommentDto;
import ru.practicum.explorewithme.dto.UpdateCommentDto;
import ru.practicum.explorewithme.model.Category;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.EventState;
import ru.practicum.explorewithme.repository.CategoryRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.UserRepository;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PrivateCommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Event event;
    private Category category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();
        user = userRepository.save(user);

        category = Category.builder()
                .name("Test Category")
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
                .state(EventState.PUBLISHED)
                .publishedOn(LocalDateTime.now().minusHours(1))
                .title("Test Event")
                .build();
        event = eventRepository.save(event);
    }

    @Test
    void createComment_whenValid_shouldReturn201() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Test comment text")
                .parentId(null)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user.getId())
                        .param("eventId", event.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Test comment text"))
                .andExpect(jsonPath("$.eventId").value(event.getId()))
                .andExpect(jsonPath("$.author.id").value(user.getId()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void createComment_whenEmptyText_shouldReturn400() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("")
                .parentId(null)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user.getId())
                        .param("eventId", event.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createComment_whenUserNotFound_shouldReturn404() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Test comment")
                .parentId(null)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", 999L)
                        .param("eventId", event.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createComment_whenEventNotPublished_shouldReturn409() throws Exception {
        Event unpublishedEvent = Event.builder()
                .annotation("Unpublished event")
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
                .title("Unpublished Event")
                .build();
        unpublishedEvent = eventRepository.save(unpublishedEvent);

        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Test comment")
                .parentId(null)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user.getId())
                        .param("eventId", unpublishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateComment_whenValid_shouldReturn200() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Initial text")
                .parentId(null)
                .build();

        String commentJson = mockMvc.perform(post("/users/{userId}/comments", user.getId())
                        .param("eventId", event.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andReturn().getResponse().getContentAsString();

        Long commentId = objectMapper.readTree(commentJson).get("id").asLong();

        UpdateCommentDto updateCommentDto = UpdateCommentDto.builder()
                .text("Updated text")
                .build();

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", user.getId(), commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommentDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Updated text"))
                .andExpect(jsonPath("$.status").value("EDITED"));
    }

    @Test
    void getUserComments_shouldReturnUserComments() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Test comment")
                .parentId(null)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user.getId())
                .param("eventId", event.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCommentDto)));

        mockMvc.perform(get("/users/{userId}/comments", user.getId())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].author.id").value(user.getId()));
    }

    @Test
    void deleteComment_shouldReturn204() throws Exception {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("To be deleted")
                .parentId(null)
                .build();

        String commentJson = mockMvc.perform(post("/users/{userId}/comments", user.getId())
                        .param("eventId", event.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andReturn().getResponse().getContentAsString();

        Long commentId = objectMapper.readTree(commentJson).get("id").asLong();

        mockMvc.perform(delete("/users/{userId}/comments/{commentId}", user.getId(), commentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/events/{eventId}/comments", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}