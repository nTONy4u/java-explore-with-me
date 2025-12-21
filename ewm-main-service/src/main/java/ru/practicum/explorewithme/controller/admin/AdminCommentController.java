package ru.practicum.explorewithme.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.dto.CommentDto;
import ru.practicum.explorewithme.dto.CommentModerationRequest;
import ru.practicum.explorewithme.model.enums.CommentStatus;
import ru.practicum.explorewithme.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getCommentsForModeration(
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /admin/comments: получение комментариев для модерации status={}", status);
        return commentService.getCommentsForModeration(status, from, size);
    }

    @PatchMapping("/{commentId}/moderate")
    public CommentDto moderateComment(@PathVariable Long commentId,
                                      @Valid @RequestBody CommentModerationRequest moderationRequest) {
        log.info("PATCH /admin/comments/{}/moderate: модерация комментария", commentId);
        return commentService.moderateComment(commentId, moderationRequest);
    }

    @PatchMapping("/{commentId}/restrict")
    public void restrictCommentEditing(@PathVariable Long commentId,
                                       @RequestParam String reason) {
        log.info("PATCH /admin/comments/{}/restrict: ограничение редактирования комментария", commentId);
        commentService.restrictCommentEditing(commentId, reason);
    }
}