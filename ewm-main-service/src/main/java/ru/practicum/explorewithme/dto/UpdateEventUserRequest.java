package ru.practicum.explorewithme.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explorewithme.dto.validation.ValidationGroups;
import ru.practicum.explorewithme.model.enums.StateAction;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {

    @Size(min = 20, max = 2000, groups = ValidationGroups.OnUpdate.class)
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, groups = ValidationGroups.OnUpdate.class)
    private String description;

    @Future(groups = ValidationGroups.OnUpdate.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;

    private LocationDto location;
    private Boolean paid;

    @PositiveOrZero(message = "Participant limit must be positive or zero",
                    groups = ValidationGroups.OnUpdate.class)
    private Integer participantLimit;

    private Boolean requestModeration;
    private StateAction stateAction;

    @Size(min = 3, max = 120, groups = ValidationGroups.OnUpdate.class)
    private String title;
}