package ru.practicum.explorewithme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.CommentDto;
import ru.practicum.explorewithme.dto.CommentFullDto;
import ru.practicum.explorewithme.dto.CommentModerationRequest;
import ru.practicum.explorewithme.dto.NewCommentDto;
import ru.practicum.explorewithme.dto.UpdateCommentDto;
import ru.practicum.explorewithme.exception.ConflictException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.CommentMapper;
import ru.practicum.explorewithme.model.Comment;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.CommentStatus;
import ru.practicum.explorewithme.model.enums.EventState;
import ru.practicum.explorewithme.repository.CommentRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.UserRepository;
import ru.practicum.explorewithme.util.PaginationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int EDIT_WINDOW_DAYS = 30;

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        Pageable pageable = PaginationUtil.createPageRequest(from, size);

        List<Comment> comments = commentRepository.findByEventIdAndStatusAndParentIsNull(
                eventId, CommentStatus.PUBLISHED, pageable);

        return comments.stream()
                .map(comment -> {
                    Integer repliesCount = commentRepository.countByParentIdAndStatus(
                            comment.getId(), CommentStatus.PUBLISHED).intValue();
                    return CommentMapper.toCommentDto(comment, repliesCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CommentFullDto getCommentWithReplies(Long commentId, Integer from, Integer size) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        if (comment.getStatus() != CommentStatus.PUBLISHED) {
            throw new NotFoundException("Comment is not published");
        }

        Pageable pageable = PaginationUtil.createPageRequest(from, size);
        List<Comment> replies = commentRepository.findByParentIdAndStatus(
                commentId, CommentStatus.PUBLISHED, pageable);

        List<CommentDto> replyDtos = replies.stream()
                .map(reply -> CommentMapper.toCommentDto(reply, 0))
                .collect(Collectors.toList());

        return CommentMapper.toCommentFullDto(comment, replyDtos);
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment on unpublished event");
        }

        Comment parent = null;
        if (newCommentDto.getParentId() != null) {
            parent = commentRepository.findById(newCommentDto.getParentId())
                    .orElseThrow(() -> new NotFoundException("Parent comment not found"));

            if (!parent.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Parent comment belongs to different event");
            }

            if (parent.getStatus() != CommentStatus.PUBLISHED) {
                throw new ConflictException("Cannot reply to unpublished comment");
            }
        }

        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .author(user)
                .event(event)
                .parent(parent)
                .status(CommentStatus.PUBLISHED)
                .created(LocalDateTime.now())
                .editableUntil(LocalDateTime.now().plusDays(EDIT_WINDOW_DAYS))
                .editRestricted(false)
                .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Created comment: id={}, event={}, author={}",
                savedComment.getId(), eventId, userId);

        return CommentMapper.toCommentDto(savedComment, 0);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Comment does not belong to user");
        }

        if (comment.getEditRestricted()) {
            throw new ConflictException("Editing is restricted for this comment: " +
                    (comment.getRestrictionReason() != null ?
                            comment.getRestrictionReason() : "Contact administrator"));
        }

        if (comment.getEditableUntil() != null &&
                comment.getEditableUntil().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Edit window has expired (30 days limit)");
        }

        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new ConflictException("Cannot edit rejected comment");
        }

        comment.setText(updateCommentDto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment.setStatus(CommentStatus.EDITED);

        Comment updatedComment = commentRepository.save(comment);
        log.info("Updated comment: id={}, author={}", commentId, userId);

        Integer repliesCount = commentRepository.countByParentIdAndStatus(
                commentId, CommentStatus.PUBLISHED).intValue();

        return CommentMapper.toCommentDto(updatedComment, repliesCount);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Comment does not belong to user");
        }

        comment.setStatus(CommentStatus.REJECTED);
        comment.setText("[deleted by user]");
        commentRepository.save(comment);

        log.info("User deleted comment: id={}, author={}", commentId, userId);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Integer from, Integer size) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " not found"));

        Pageable pageable = PaginationUtil.createPageRequest(from, size);

        List<Comment> comments = commentRepository.findByAuthorIdAndStatus(
                userId, CommentStatus.PUBLISHED, pageable);

        return comments.stream()
                .map(comment -> {
                    Integer repliesCount = commentRepository.countByParentIdAndStatus(
                            comment.getId(), CommentStatus.PUBLISHED).intValue();
                    return CommentMapper.toCommentDto(comment, repliesCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(CommentStatus status, Integer from, Integer size) {
        Pageable pageable = PaginationUtil.createPageRequest(from, size);

        List<Comment> comments = commentRepository.findByStatus(status, pageable);

        return comments.stream()
                .map(comment -> CommentMapper.toCommentDto(comment, 0))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, CommentModerationRequest moderationRequest) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        comment.setStatus(moderationRequest.getStatus());

        if (moderationRequest.getEditRestricted() != null) {
            comment.setEditRestricted(moderationRequest.getEditRestricted());
            if (moderationRequest.getEditRestricted()) {
                comment.setRestrictionReason(moderationRequest.getRestrictionReason());
            } else {
                comment.setRestrictionReason(null);
            }
        }

        Comment moderatedComment = commentRepository.save(comment);
        log.info("Moderated comment: id={}, status={}", commentId, moderationRequest.getStatus());

        Integer repliesCount = commentRepository.countByParentIdAndStatus(
                commentId, CommentStatus.PUBLISHED).intValue();

        return CommentMapper.toCommentDto(moderatedComment, repliesCount);
    }

    @Override
    @Transactional
    public void restrictCommentEditing(Long commentId, String restrictionReason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        comment.setEditRestricted(true);
        comment.setRestrictionReason(restrictionReason);
        commentRepository.save(comment);

        log.info("Restricted editing for comment: id={}, reason={}", commentId, restrictionReason);
    }

    @Override
    public void checkEditRestriction(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Comment does not belong to user");
        }

        if (comment.getEditRestricted()) {
            throw new ConflictException("Editing is restricted for this comment. Reason: " +
                    (comment.getRestrictionReason() != null ?
                            comment.getRestrictionReason() : "Contact administrator"));
        }

        if (comment.getEditableUntil() != null &&
                comment.getEditableUntil().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Edit window has expired (30 days limit)");
        }
    }
}