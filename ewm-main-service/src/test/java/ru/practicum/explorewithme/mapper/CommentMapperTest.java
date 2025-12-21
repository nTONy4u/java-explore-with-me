package ru.practicum.explorewithme.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.explorewithme.dto.CommentDto;
import ru.practicum.explorewithme.dto.CommentFullDto;
import ru.practicum.explorewithme.model.Comment;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.CommentStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentMapperTest {

    @Test
    void toCommentDto_shouldMapCorrectly() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .text("Test comment")
                .author(user)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .updated(LocalDateTime.of(2024, 1, 1, 13, 0, 0))
                .editableUntil(LocalDateTime.of(2024, 1, 31, 12, 0, 0))
                .editRestricted(false)
                .restrictionReason(null)
                .build();

        CommentDto dto = CommentMapper.toCommentDto(comment, 5);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test comment", dto.getText());
        assertEquals(1L, dto.getAuthor().getId());
        assertEquals("Test User", dto.getAuthor().getName());
        assertEquals(1L, dto.getEventId());
        assertEquals(CommentStatus.PUBLISHED, dto.getStatus());
        assertEquals(LocalDateTime.of(2024, 1, 1, 12, 0, 0), dto.getCreated());
        assertEquals(LocalDateTime.of(2024, 1, 1, 13, 0, 0), dto.getUpdated());
        assertEquals(LocalDateTime.of(2024, 1, 31, 12, 0, 0), dto.getEditableUntil());
        assertFalse(dto.getEditRestricted());
        assertNull(dto.getRestrictionReason());
        assertEquals(5, dto.getRepliesCount());
    }

    @Test
    void toCommentDto_whenNull_shouldReturnNull() {
        CommentDto dto = CommentMapper.toCommentDto(null, 0);
        assertNull(dto);
    }

    @Test
    void toCommentFullDto_shouldMapCorrectly() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .text("Test comment")
                .author(user)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.of(2024, 1, 1, 12, 0, 0))
                .updated(LocalDateTime.of(2024, 1, 1, 13, 0, 0))
                .editableUntil(LocalDateTime.of(2024, 1, 31, 12, 0, 0))
                .editRestricted(false)
                .restrictionReason(null)
                .build();

        CommentDto replyDto = CommentDto.builder()
                .id(2L)
                .text("Reply")
                .author(UserMapper.toUserShortDto(user))
                .eventId(1L)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.of(2024, 1, 1, 14, 0, 0))
                .build();

        CommentFullDto fullDto = CommentMapper.toCommentFullDto(comment, List.of(replyDto));

        assertNotNull(fullDto);
        assertEquals(1L, fullDto.getId());
        assertEquals("Test comment", fullDto.getText());
        assertEquals(1L, fullDto.getAuthor().getId());
        assertEquals(1L, fullDto.getEventId());
        assertEquals(CommentStatus.PUBLISHED, fullDto.getStatus());
        assertFalse(fullDto.getEditRestricted());
        assertNotNull(fullDto.getReplies());
        assertEquals(1, fullDto.getReplies().size());
        assertEquals(2L, fullDto.getReplies().get(0).getId());
        assertEquals("Reply", fullDto.getReplies().get(0).getText());
    }

    @Test
    void toCommentFullDto_whenNullReplies_shouldUseEmptyList() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        Event event = Event.builder()
                .id(1L)
                .title("Test Event")
                .build();

        Comment comment = Comment.builder()
                .id(1L)
                .text("Test comment")
                .author(user)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .build();

        CommentFullDto fullDto = CommentMapper.toCommentFullDto(comment, null);

        assertNotNull(fullDto);
        assertNotNull(fullDto.getReplies());
        assertTrue(fullDto.getReplies().isEmpty());
    }

    @Test
    void toCommentFullDto_whenNullComment_shouldReturnNull() {
        CommentFullDto fullDto = CommentMapper.toCommentFullDto(null, List.of());
        assertNull(fullDto);
    }
}