package ru.practicum.explorewithme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventWithViewsDto {
    private Long id;
    private String annotation;
    private Long categoryId;
    private String categoryName;
    private String description;
    private LocalDateTime eventDate;
    private Long initiatorId;
    private String initiatorName;
    private Float lat;
    private Float lon;
    private Boolean paid;
    private Integer participantLimit;
    private LocalDateTime publishedOn;
    private Boolean requestModeration;
    private String title;
    private Long views;
    private Long confirmedRequests;
}