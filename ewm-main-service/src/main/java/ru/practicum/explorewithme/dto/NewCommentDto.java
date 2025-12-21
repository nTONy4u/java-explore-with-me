package ru.practicum.explorewithme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCommentDto {
    @NotBlank(message = "Comment text must not be blank")
    @Size(min = 1, max = 2000, message = "Comment text must be between 1 and 2000 characters")
    private String text;

    private Long parentId;
}