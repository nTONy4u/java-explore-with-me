package ru.practicum.explorewithme.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.controller.admin.AdminCategoryController;
import ru.practicum.explorewithme.controller.publicapi.PublicCategoryController;
import ru.practicum.explorewithme.service.CategoryService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AdminCategoryController.class, PublicCategoryController.class})
class ErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void handleNotFoundException_shouldReturn404() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new NotFoundException("Категория с id=999 не найдена"));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.reason").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Категория с id=999 не найдена"));
    }

    @Test
    void handleConflictException_shouldReturn409() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(new ConflictException("Категория с таким именем уже существует"));
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Существующая категория\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("CONFLICT"))
                .andExpect(jsonPath("$.reason").value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value("Категория с таким именем уже существует"));
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturn400() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"));
    }

    @Test
    void handleMethodArgumentNotValidException_whenNameTooLong_shouldReturn400() throws Exception {
        String longName = "A".repeat(51);
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + longName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleMethodArgumentTypeMismatchException_shouldReturn400() throws Exception {
        mockMvc.perform(get("/categories/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message", Matchers.containsString("Failed to convert value")));
    }

    @Test
    void handleHttpMessageNotReadableException_shouldReturn400() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void handleConstraintViolationException_shouldReturn400() throws Exception {
        String veryLongName = "A".repeat(100);
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + veryLongName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleIllegalArgumentExceptionFromService_shouldReturn400() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(new IllegalArgumentException("Invalid category name"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value("Invalid category name"));
    }

    @Test
    void handleIllegalStateExceptionFromService_shouldReturn400() throws Exception {
        when(categoryService.createCategory(any()))
                .thenThrow(new IllegalStateException("Invalid state for category creation"));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.reason").value("Incorrectly made request."))
                .andExpect(jsonPath("$.message").value("Invalid state for category creation"));
    }

    @Test
    void handleMissingServletRequestParameterException_shouldReturn400() throws Exception {
        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void handleRuntimeException_shouldReturn500() throws Exception {
        when(categoryService.getCategoryById(1L))
                .thenThrow(new RuntimeException("Unexpected database error"));

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.reason").value("Internal server error."))
                .andExpect(jsonPath("$.message").value("Unexpected database error"));
    }
}