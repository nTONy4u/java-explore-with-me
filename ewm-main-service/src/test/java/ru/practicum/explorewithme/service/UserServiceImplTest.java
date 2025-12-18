package ru.practicum.explorewithme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.explorewithme.dto.NewUserRequest;
import ru.practicum.explorewithme.dto.UserDto;
import ru.practicum.explorewithme.exception.ConflictException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.repository.UserRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private NewUserRequest newUserRequest;
    private User user;

    @BeforeEach
    void setUp() {
        newUserRequest = NewUserRequest.builder()
                .name("Test User")
                .email("test@example.com")
                .build();

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();
    }

    @Test
    void createUser_whenValid_shouldCreateUser() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.createUser(newUserRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_whenEmailExists_shouldThrowConflictException() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        assertThrows(ConflictException.class, () ->
                userService.createUser(newUserRequest));
    }

    @Test
    void getUsers_whenIdsProvided_shouldReturnFilteredUsers() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        when(userRepository.findByIdIn(eq(List.of(1L)), eq(pageable)))
                .thenReturn(List.of(user));

        List<UserDto> result = userService.getUsers(List.of(1L), 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getName());
    }

    @Test
    void getUsers_whenNoIds_shouldReturnAllUsers() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(eq(pageable))).thenReturn(userPage);

        List<UserDto> result = userService.getUsers(null, 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test User", result.get(0).getName());
    }

    @Test
    void getUsers_whenEmptyIds_shouldReturnAllUsers() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(eq(pageable))).thenReturn(userPage);

        List<UserDto> result = userService.getUsers(Collections.emptyList(), 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getUsers_withPagination_shouldUseCorrectPage() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by("id").ascending());
        Page<User> userPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(userRepository.findAll(eq(pageable))).thenReturn(userPage);

        List<UserDto> result = userService.getUsers(null, 5, 5);

        assertTrue(result.isEmpty());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void deleteUser_whenExists_shouldDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_whenNotExists_shouldThrowNotFoundException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(NotFoundException.class, () ->
                userService.deleteUser(1L));

        verify(userRepository, never()).deleteById(any());
    }
}