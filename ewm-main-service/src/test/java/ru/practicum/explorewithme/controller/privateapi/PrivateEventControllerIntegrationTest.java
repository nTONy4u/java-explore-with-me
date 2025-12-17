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
import ru.practicum.explorewithme.dto.LocationDto;
import ru.practicum.explorewithme.dto.NewEventDto;
import ru.practicum.explorewithme.model.Category;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.repository.CategoryRepository;
import ru.practicum.explorewithme.repository.UserRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PrivateEventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Category category;
    private NewEventDto newEventDto;

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

        newEventDto = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .description("Detailed description of the test event")
                .category(category.getId())
                .eventDate(LocalDateTime.now().plusDays(2))
                .location(LocationDto.builder().lat(55.754167f).lon(37.62f).build())
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event Title")
                .build();
    }

    @Test
    void createEvent_whenValid_shouldReturn201() throws Exception {
        mockMvc.perform(post("/users/{userId}/events", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.annotation").value("Test annotation for the event"))
                .andExpect(jsonPath("$.title").value("Test Event Title"))
                .andExpect(jsonPath("$.state").value("PENDING"));
    }

    @Test
    void createEvent_whenEventDateTooClose_shouldReturn400() throws Exception {
        newEventDto.setEventDate(LocalDateTime.now().plusMinutes(30));

        mockMvc.perform(post("/users/{userId}/events", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_whenUserNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(post("/users/{userId}/events", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEvent_whenCategoryNotFound_shouldReturn404() throws Exception {
        newEventDto.setCategory(999L);

        mockMvc.perform(post("/users/{userId}/events", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isNotFound());
    }
}