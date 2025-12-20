package ru.practicum.explorewithme.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explorewithme.model.enums.CommentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentModerationRequest {

    @NotNull(message = "Status must not be null")
    private CommentStatus status;

    @Size(max = 500, message = "Restriction reason must not exceed 500 characters")
    private String restrictionReason;

    private Boolean editRestricted;
}