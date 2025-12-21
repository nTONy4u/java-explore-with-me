package ru.practicum.explorewithme;

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
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class E2ECommentsTest {

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

    private User user1;
    private User user2;
    private Event publishedEvent;
    private Category category;

    @BeforeEach
    void setUp() {
        String uniqueEmail1 = "user1_" + UUID.randomUUID() + "@example.com";
        String uniqueEmail2 = "user2_" + UUID.randomUUID() + "@example.com";

        user1 = User.builder()
                .name("User One")
                .email(uniqueEmail1)
                .build();
        user1 = userRepository.save(user1);

        user2 = User.builder()
                .name("User Two")
                .email(uniqueEmail2)
                .build();
        user2 = userRepository.save(user2);

        category = Category.builder()
                .name("Category_" + UUID.randomUUID())
                .build();
        category = categoryRepository.save(category);

        publishedEvent = Event.builder()
                .annotation("Published event annotation")
                .description("Detailed description for published event")
                .category(category)
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(user1)
                .lat(55.754167f)
                .lon(37.62f)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .publishedOn(LocalDateTime.now().minusHours(1))
                .title("Published Event for Comments")
                .build();
        publishedEvent = eventRepository.save(publishedEvent);
    }


    @Test
    void validationTests_shouldReturnAppropriateErrors() throws Exception {
        NewCommentDto emptyTextDto = NewCommentDto.builder()
                .text("")
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user1.getId())
                        .param("eventId", publishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyTextDto)))
                .andExpect(status().isBadRequest());

        String longText = "a".repeat(2001);
        NewCommentDto longTextDto = NewCommentDto.builder()
                .text(longText)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user1.getId())
                        .param("eventId", publishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longTextDto)))
                .andExpect(status().isBadRequest());

        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Test comment for editing")
                .build();

        String commentResponse = mockMvc.perform(post("/users/{userId}/comments", user1.getId())
                        .param("eventId", publishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCommentDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long commentId = objectMapper.readTree(commentResponse).get("id").asLong();

        UpdateCommentDto longUpdateDto = UpdateCommentDto.builder()
                .text(longText)
                .build();

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", user1.getId(), commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longUpdateDto)))
                .andExpect(status().isBadRequest());

        UpdateCommentDto updateDto = UpdateCommentDto.builder()
                .text("Attempt to edit someone else's comment")
                .build();

        mockMvc.perform(patch("/users/{userId}/comments/{commentId}", user2.getId(), commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createComment_onUnpublishedEvent_shouldReturn409() throws Exception {
        Event unpublishedEvent = Event.builder()
                .annotation("Unpublished event")
                .description("Unpublished event description")
                .category(category)
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(user1)
                .lat(55.754167f)
                .lon(37.62f)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PENDING)
                .title("Unpublished Event")
                .build();
        unpublishedEvent = eventRepository.save(unpublishedEvent);

        NewCommentDto commentDto = NewCommentDto.builder()
                .text("Comment on unpublished event")
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user1.getId())
                        .param("eventId", unpublishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void createReply_onParentComment_shouldWork() throws Exception {
        NewCommentDto parentCommentDto = NewCommentDto.builder()
                .text("Parent comment")
                .build();

        String parentResponse = mockMvc.perform(post("/users/{userId}/comments", user1.getId())
                        .param("eventId", publishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentCommentDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long parentId = objectMapper.readTree(parentResponse).get("id").asLong();

        NewCommentDto replyDto = NewCommentDto.builder()
                .text("Reply to parent")
                .parentId(parentId)
                .build();

        mockMvc.perform(post("/users/{userId}/comments", user2.getId())
                        .param("eventId", publishedEvent.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(parentId));

        mockMvc.perform(get("/events/{eventId}/comments/{commentId}", publishedEvent.getId(), parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies", hasSize(1)))
                .andExpect(jsonPath("$.replies[0].text").value("Reply to parent"));
    }
}