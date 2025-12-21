package ru.practicum.explorewithme.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.explorewithme.dto.CommentDto;
import ru.practicum.explorewithme.dto.CommentFullDto;
import ru.practicum.explorewithme.model.Comment;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class CommentMapper {

    public static CommentDto toCommentDto(Comment comment, Integer repliesCount) {
        if (comment == null) {
            return null;
        }

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .status(comment.getStatus())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .editableUntil(comment.getEditableUntil())
                .editRestricted(comment.getEditRestricted())
                .restrictionReason(comment.getRestrictionReason())
                .repliesCount(repliesCount != null ? repliesCount : 0)
                .build();
    }

    public static CommentFullDto toCommentFullDto(Comment comment, List<CommentDto> replies) {
        if (comment == null) {
            return null;
        }

        return CommentFullDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toUserShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .status(comment.getStatus())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .editableUntil(comment.getEditableUntil())
                .editRestricted(comment.getEditRestricted())
                .restrictionReason(comment.getRestrictionReason())
                .replies(replies != null ? replies : Collections.emptyList())
                .build();
    }
}