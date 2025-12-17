package ru.practicum.explorewithme.service;

import ru.practicum.explorewithme.dto.NewUserRequest;
import ru.practicum.explorewithme.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);
}