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
import ru.practicum.explorewithme.dto.CategoryDto;
import ru.practicum.explorewithme.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.dto.LocationDto;
import ru.practicum.explorewithme.dto.NewCategoryDto;
import ru.practicum.explorewithme.dto.NewCompilationDto;
import ru.practicum.explorewithme.dto.NewEventDto;
import ru.practicum.explorewithme.dto.NewUserRequest;
import ru.practicum.explorewithme.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.dto.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
class E2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    void fullEventLifecycle_shouldWork() throws Exception {
        String uniqueUserEmail = "user_" + UUID.randomUUID() + "@example.com";
        NewUserRequest userRequest = NewUserRequest.builder()
                .name("Test User")
                .email(uniqueUserEmail)
                .build();

        String userResponse = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value(uniqueUserEmail))
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        String uniqueCategoryName = "Category_" + UUID.randomUUID();
        NewCategoryDto categoryDto = NewCategoryDto.builder()
                .name(uniqueCategoryName)
                .build();

        String categoryResponse = mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(uniqueCategoryName))
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readTree(categoryResponse).get("id").asLong();

        String uniqueEventTitle = "Event_" + UUID.randomUUID();
        NewEventDto newEventDto = NewEventDto.builder()
                .annotation("This is a valid event annotation with sufficient length for validation requirements - at least 20 characters")
                .description("This is a valid event description that meets the minimum length requirement of twenty characters or more. It should be descriptive enough.")
                .category(categoryId)
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(LocationDto.builder().lat(55.754167f).lon(37.62f).build())
                .paid(false)
                .participantLimit(5)
                .requestModeration(true)
                .title(uniqueEventTitle)
                .build();

        String eventResponse = mockMvc.perform(post("/users/{userId}/events", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newEventDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(uniqueEventTitle))
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        Long eventId = objectMapper.readTree(eventResponse).get("id").asLong();

        mockMvc.perform(get("/users/{userId}/events", userId)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId))
                .andExpect(jsonPath("$[0].title").value(uniqueEventTitle));

        UpdateEventAdminRequest adminRequest = UpdateEventAdminRequest.builder()
                .stateAction(ru.practicum.explorewithme.model.enums.StateAction.PUBLISH_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/{eventId}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));

        String secondUserEmail = "user2_" + UUID.randomUUID() + "@example.com";
        NewUserRequest secondUserRequest = NewUserRequest.builder()
                .name("Second User")
                .email(secondUserEmail)
                .build();

        String secondUserResponse = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUserRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long secondUserId = objectMapper.readTree(secondUserResponse).get("id").asLong();

        mockMvc.perform(post("/users/{userId}/requests", secondUserId)
                        .param("eventId", eventId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        EventRequestStatusUpdateRequest statusUpdateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(ru.practicum.explorewithme.model.enums.RequestStatus.CONFIRMED)
                .build();

        String requestsResponse = mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", userId, eventId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/events/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));

        mockMvc.perform(get("/events")
                        .param("text", "valid event")
                        .param("categories", categoryId.toString())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId));
    }

    @Test
    void createCategoryAndCompilation_shouldWork() throws Exception {
        String uniqueCategoryName = "Выставки_" + UUID.randomUUID();
        NewCategoryDto categoryDto = NewCategoryDto.builder()
                .name(uniqueCategoryName)
                .build();

        String categoryResponse = mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(uniqueCategoryName))
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readTree(categoryResponse).get("id").asLong();

        mockMvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + categoryId + ")]").exists());

        mockMvc.perform(get("/categories/{catId}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value(uniqueCategoryName));

        String updatedCategoryName = "Обновленные_" + UUID.randomUUID();
        CategoryDto updateDto = CategoryDto.builder()
                .name(updatedCategoryName)
                .build();

        mockMvc.perform(patch("/admin/categories/{catId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updatedCategoryName));

        String uniqueCompilationTitle = "Подборка_" + UUID.randomUUID();
        NewCompilationDto compilationDto = NewCompilationDto.builder()
                .title(uniqueCompilationTitle)
                .pinned(true)
                .build();

        String compilationResponse = mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compilationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(uniqueCompilationTitle))
                .andExpect(jsonPath("$.pinned").value(true))
                .andReturn().getResponse().getContentAsString();

        Long compilationId = objectMapper.readTree(compilationResponse).get("id").asLong();

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + compilationId + ")]").exists());

        mockMvc.perform(get("/compilations/{compId}", compilationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compilationId))
                .andExpect(jsonPath("$.title").value(uniqueCompilationTitle));

        String updatedCompilationTitle = "Обновленная_" + UUID.randomUUID();
        UpdateCompilationRequest updateCompilationRequest = UpdateCompilationRequest.builder()
                .title(updatedCompilationTitle)
                .pinned(false)
                .build();

        mockMvc.perform(patch("/admin/compilations/{compId}", compilationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCompilationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(updatedCompilationTitle))
                .andExpect(jsonPath("$.pinned").value(false));

        mockMvc.perform(delete("/admin/compilations/{compId}", compilationId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/compilations/{compId}", compilationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void userManagement_shouldWork() throws Exception {
        String email1 = "user1_" + UUID.randomUUID() + "@example.com";
        String email2 = "user2_" + UUID.randomUUID() + "@example.com";

        NewUserRequest user1 = NewUserRequest.builder()
                .name("User One")
                .email(email1)
                .build();

        NewUserRequest user2 = NewUserRequest.builder()
                .name("User Two")
                .email(email2)
                .build();

        String response1 = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId1 = objectMapper.readTree(response1).get("id").asLong();

        String response2 = mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId2 = objectMapper.readTree(response2).get("id").asLong();

        mockMvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/admin/users")
                        .param("ids", userId1.toString(), userId2.toString())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/admin/users")
                        .param("ids", userId1.toString())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userId1));

        mockMvc.perform(delete("/admin/users/{userId}", userId1))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/admin/users")
                        .param("ids", userId1.toString())
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        NewUserRequest duplicateUser = NewUserRequest.builder()
                .name("Duplicate User")
                .email(email2)
                .build();

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isConflict());
    }
}