package ru.practicum.explorewithme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.explorewithme.dto.CommentDto;
import ru.practicum.explorewithme.dto.CommentFullDto;
import ru.practicum.explorewithme.dto.CommentModerationRequest;
import ru.practicum.explorewithme.dto.NewCommentDto;
import ru.practicum.explorewithme.dto.UpdateCommentDto;
import ru.practicum.explorewithme.exception.ConflictException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.Comment;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.CommentStatus;
import ru.practicum.explorewithme.model.enums.EventState;
import ru.practicum.explorewithme.repository.CommentRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private Event event;
    private Comment comment;
    private NewCommentDto newCommentDto;
    private UpdateCommentDto updateCommentDto;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        event = Event.builder()
                .id(1L)
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .publishedOn(LocalDateTime.now().minusHours(1))
                .build();

        comment = Comment.builder()
                .id(1L)
                .text("Original text")
                .author(user)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now().minusHours(1))
                .editableUntil(LocalDateTime.now().plusDays(30))
                .editRestricted(false)
                .build();

        newCommentDto = NewCommentDto.builder()
                .text("Test comment text")
                .parentId(null)
                .build();

        updateCommentDto = UpdateCommentDto.builder()
                .text("Updated text")
                .build();
    }

    @Test
    void createComment_whenValid_shouldCreateComment() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CommentDto result = commentService.createComment(1L, 1L, newCommentDto);

        assertNotNull(result);
        assertEquals("Test comment text", result.getText());
        assertEquals(CommentStatus.PUBLISHED, result.getStatus());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                commentService.createComment(1L, 1L, newCommentDto));
    }

    @Test
    void createComment_whenEventNotPublished_shouldThrowConflictException() {
        event.setState(EventState.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class, () ->
                commentService.createComment(1L, 1L, newCommentDto));
    }

    @Test
    void createComment_whenParentExists_shouldCreateReply() {
        Comment parent = Comment.builder()
                .id(2L)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .build();

        newCommentDto.setParentId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(3L);
            return c;
        });

        CommentDto result = commentService.createComment(1L, 1L, newCommentDto);

        assertNotNull(result);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_whenParentToDifferentEvent_shouldThrowConflictException() {
        Event otherEvent = Event.builder().id(2L).build();
        Comment parent = Comment.builder()
                .id(2L)
                .event(otherEvent)
                .status(CommentStatus.PUBLISHED)
                .build();

        newCommentDto.setParentId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(commentRepository.findById(2L)).thenReturn(Optional.of(parent));

        assertThrows(ConflictException.class, () ->
                commentService.createComment(1L, 1L, newCommentDto));
    }

    @Test
    void updateComment_whenValid_shouldUpdate() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.updateComment(1L, 1L, updateCommentDto);

        assertNotNull(result);
        assertEquals("Updated text", result.getText());
        assertEquals(CommentStatus.EDITED, comment.getStatus());
        assertNotNull(comment.getUpdated());
    }

    @Test
    void updateComment_whenNotAuthor_shouldThrowNotFoundException() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(NotFoundException.class, () ->
                commentService.updateComment(2L, 1L, updateCommentDto));
    }

    @Test
    void updateComment_whenEditRestricted_shouldThrowConflictException() {
        comment.setEditRestricted(true);
        comment.setRestrictionReason("Violated rules");

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                commentService.updateComment(1L, 1L, updateCommentDto));

        assertTrue(exception.getMessage().contains("Editing is restricted"));
    }

    @Test
    void updateComment_whenEditWindowExpired_shouldThrowConflictException() {
        comment.setEditableUntil(LocalDateTime.now().minusDays(1));

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                commentService.updateComment(1L, 1L, updateCommentDto));

        assertTrue(exception.getMessage().contains("Edit window has expired"));
    }

    @Test
    void updateComment_whenRejected_shouldThrowConflictException() {
        comment.setStatus(CommentStatus.REJECTED);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(ConflictException.class, () ->
                commentService.updateComment(1L, 1L, updateCommentDto));
    }

    @Test
    void deleteComment_shouldMarkAsRejected() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.deleteComment(1L, 1L);

        assertEquals(CommentStatus.REJECTED, comment.getStatus());
        assertEquals("[deleted by user]", comment.getText());
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_whenNotAuthor_shouldThrowNotFoundException() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(NotFoundException.class, () ->
                commentService.deleteComment(2L, 1L));
    }

    @Test
    void getEventComments_shouldReturnPublishedComments() {
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findByEventIdAndStatusAndParentIsNull(
                eq(1L), eq(CommentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(List.of(comment));
        when(commentRepository.countByParentIdAndStatus(anyLong(), eq(CommentStatus.PUBLISHED)))
                .thenReturn(0L);

        List<CommentDto> result = commentService.getEventComments(1L, 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getCommentWithReplies_shouldReturnCommentAndReplies() {
        Comment reply = Comment.builder()
                .id(2L)
                .text("Reply")
                .author(user)
                .event(event)
                .parent(comment)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentIdAndStatus(
                eq(1L), eq(CommentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(List.of(reply));

        CommentFullDto result = commentService.getCommentWithReplies(1L, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertFalse(result.getReplies().isEmpty());
        assertEquals(1, result.getReplies().size());
    }

    @Test
    void getCommentWithReplies_whenNotPublished_shouldThrowNotFoundException() {
        comment.setStatus(CommentStatus.PENDING);

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(NotFoundException.class, () ->
                commentService.getCommentWithReplies(1L, 0, 10));
    }

    @Test
    void getUserComments_shouldReturnUserComments() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentRepository.findByAuthorIdAndStatus(
                eq(1L), eq(CommentStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(List.of(comment));
        when(commentRepository.countByParentIdAndStatus(anyLong(), eq(CommentStatus.PUBLISHED)))
                .thenReturn(0L);

        List<CommentDto> result = commentService.getUserComments(1L, 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getCommentsForModeration_shouldReturnCommentsByStatus() {
        when(commentRepository.findByStatus(eq(CommentStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(comment));

        List<CommentDto> result = commentService.getCommentsForModeration(CommentStatus.PENDING, 0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void moderateComment_shouldUpdateStatusAndRestrictions() {
        CommentModerationRequest request = CommentModerationRequest.builder()
                .status(CommentStatus.REJECTED)
                .editRestricted(true)
                .restrictionReason("Violated rules")
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDto result = commentService.moderateComment(1L, request);

        assertEquals(CommentStatus.REJECTED, comment.getStatus());
        assertTrue(comment.getEditRestricted());
        assertEquals("Violated rules", comment.getRestrictionReason());
    }

    @Test
    void restrictCommentEditing_shouldSetRestriction() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        commentService.restrictCommentEditing(1L, "Test restriction");

        assertTrue(comment.getEditRestricted());
        assertEquals("Test restriction", comment.getRestrictionReason());
    }

    @Test
    void checkEditRestriction_whenAllowed_shouldNotThrow() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertDoesNotThrow(() -> commentService.checkEditRestriction(1L, 1L));
    }

    @Test
    void checkEditRestriction_whenNotAuthor_shouldThrowNotFoundException() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(NotFoundException.class, () ->
                commentService.checkEditRestriction(2L, 1L));
    }

    @Test
    void checkEditRestriction_whenRestricted_shouldThrowConflictException() {
        comment.setEditRestricted(true);
        comment.setRestrictionReason("Test");

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                commentService.checkEditRestriction(1L, 1L));

        assertTrue(exception.getMessage().contains("Editing is restricted"));
    }

    @Test
    void checkEditRestriction_whenWindowExpired_shouldThrowConflictException() {
        comment.setEditableUntil(LocalDateTime.now().minusDays(1));

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                commentService.checkEditRestriction(1L, 1L));

        assertTrue(exception.getMessage().contains("Edit window has expired"));
    }
}