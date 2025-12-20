package ru.practicum.explorewithme.service;

import ru.practicum.explorewithme.dto.CommentDto;
import ru.practicum.explorewithme.dto.CommentFullDto;
import ru.practicum.explorewithme.dto.CommentModerationRequest;
import ru.practicum.explorewithme.dto.NewCommentDto;
import ru.practicum.explorewithme.dto.UpdateCommentDto;
import ru.practicum.explorewithme.model.enums.CommentStatus;

import java.util.List;

public interface CommentService {

    List<CommentDto> getEventComments(Long eventId, Integer from, Integer size);

    CommentFullDto getCommentWithReplies(Long commentId, Integer from, Integer size);

    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto updateCommentDto);

    void deleteComment(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, Integer from, Integer size);

    List<CommentDto> getCommentsForModeration(CommentStatus status, Integer from, Integer size);

    CommentDto moderateComment(Long commentId, CommentModerationRequest moderationRequest);

    void restrictCommentEditing(Long commentId, String restrictionReason);

    void checkEditRestriction(Long userId, Long commentId);
}