package ru.practicum.explorewithme.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.model.Comment;
import ru.practicum.explorewithme.model.enums.CommentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByEventIdAndStatusAndParentIsNull(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByParentIdAndStatus(Long parentId, CommentStatus status, Pageable pageable);

    List<Comment> findByAuthorIdAndStatus(Long authorId, CommentStatus status, Pageable pageable);

    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    Long countByEventIdAndStatus(Long eventId, CommentStatus status);

    Long countByParentIdAndStatus(Long parentId, CommentStatus status);

    boolean existsByEventIdAndAuthorId(Long eventId, Long authorId);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.status = :status " +
            "AND (c.parent.id IS NULL OR c.parent.id = :parentId)")
    List<Comment> findCommentsByEventAndParent(@Param("eventId") Long eventId,
                                               @Param("parentId") Long parentId,
                                               @Param("status") CommentStatus status,
                                               Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.editableUntil < :now AND c.editRestricted = false")
    List<Comment> findCommentsWithExpiredEditTime(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Comment c WHERE c.status = 'PENDING' AND c.created < :threshold")
    List<Comment> findOldPendingComments(@Param("threshold") LocalDateTime threshold);
}