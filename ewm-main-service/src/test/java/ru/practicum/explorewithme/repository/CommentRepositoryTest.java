package ru.practicum.explorewithme.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.explorewithme.model.Category;
import ru.practicum.explorewithme.model.Comment;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.CommentStatus;
import ru.practicum.explorewithme.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Event event;
    private Category category;
    private Comment comment;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();
        user = userRepository.save(user);

        category = Category.builder()
                .name("Test Category")
                .build();
        category = categoryRepository.save(category);

        event = Event.builder()
                .annotation("Test annotation")
                .description("Test description")
                .category(category)
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiator(user)
                .lat(55.754167f)
                .lon(37.62f)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Test Event")
                .build();
        event = eventRepository.save(event);

        comment = Comment.builder()
                .text("Test comment")
                .author(user)
                .event(event)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .editableUntil(LocalDateTime.now().plusDays(30))
                .editRestricted(false)
                .build();
        comment = commentRepository.save(comment);
    }

    @Test
    void findByEventIdAndStatus_shouldReturnComments() {
        List<Comment> comments = commentRepository.findByEventIdAndStatus(
                event.getId(), CommentStatus.PUBLISHED, PageRequest.of(0, 10));

        assertFalse(comments.isEmpty());
        assertEquals(comment.getId(), comments.get(0).getId());
        assertEquals(event.getId(), comments.get(0).getEvent().getId());
    }

    @Test
    void findByEventIdAndStatusAndParentIsNull_shouldReturnRootComments() {
        List<Comment> comments = commentRepository.findByEventIdAndStatusAndParentIsNull(
                event.getId(), CommentStatus.PUBLISHED, PageRequest.of(0, 10));

        assertFalse(comments.isEmpty());
        assertNull(comments.get(0).getParent());
    }

    @Test
    void findByParentIdAndStatus_shouldReturnReplies() {
        Comment reply = Comment.builder()
                .text("Reply")
                .author(user)
                .event(event)
                .parent(comment)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .editableUntil(LocalDateTime.now().plusDays(30))
                .editRestricted(false)
                .build();
        commentRepository.save(reply);

        List<Comment> replies = commentRepository.findByParentIdAndStatus(
                comment.getId(), CommentStatus.PUBLISHED, PageRequest.of(0, 10));

        assertFalse(replies.isEmpty());
        assertEquals(comment.getId(), replies.get(0).getParent().getId());
    }

    @Test
    void findByAuthorIdAndStatus_shouldReturnUserComments() {
        List<Comment> comments = commentRepository.findByAuthorIdAndStatus(
                user.getId(), CommentStatus.PUBLISHED, PageRequest.of(0, 10));

        assertFalse(comments.isEmpty());
        assertEquals(user.getId(), comments.get(0).getAuthor().getId());
    }

    @Test
    void findByStatus_shouldReturnCommentsByStatus() {
        List<Comment> comments = commentRepository.findByStatus(
                CommentStatus.PUBLISHED, PageRequest.of(0, 10));

        assertFalse(comments.isEmpty());
        assertEquals(CommentStatus.PUBLISHED, comments.get(0).getStatus());
    }

    @Test
    void countByEventIdAndStatus_shouldReturnCount() {
        Long count = commentRepository.countByEventIdAndStatus(event.getId(), CommentStatus.PUBLISHED);

        assertEquals(1L, count);
    }

    @Test
    void countByParentIdAndStatus_shouldReturnRepliesCount() {
        Comment reply = Comment.builder()
                .text("Reply")
                .author(user)
                .event(event)
                .parent(comment)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .editableUntil(LocalDateTime.now().plusDays(30))
                .editRestricted(false)
                .build();
        commentRepository.save(reply);

        Long count = commentRepository.countByParentIdAndStatus(comment.getId(), CommentStatus.PUBLISHED);

        assertEquals(1L, count);
    }

    @Test
    void existsByEventIdAndAuthorId_shouldReturnTrueWhenExists() {
        boolean exists = commentRepository.existsByEventIdAndAuthorId(event.getId(), user.getId());

        assertTrue(exists);
    }

    @Test
    void existsByEventIdAndAuthorId_shouldReturnFalseWhenNotExists() {
        boolean exists = commentRepository.existsByEventIdAndAuthorId(event.getId(), 999L);

        assertFalse(exists);
    }

    @Test
    void findCommentsByEventAndParent_shouldReturnCommentsWithParent() {
        Comment reply = Comment.builder()
                .text("Reply")
                .author(user)
                .event(event)
                .parent(comment)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .editableUntil(LocalDateTime.now().plusDays(30))
                .editRestricted(false)
                .build();
        commentRepository.save(reply);

        List<Comment> comments = commentRepository.findCommentsByEventAndParent(
                event.getId(), comment.getId(), CommentStatus.PUBLISHED, PageRequest.of(0, 10));

        assertFalse(comments.isEmpty());
    }
}