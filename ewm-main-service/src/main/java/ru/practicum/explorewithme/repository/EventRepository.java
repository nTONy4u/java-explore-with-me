package ru.practicum.explorewithme.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    boolean existsByCategoryId(Long categoryId);

    @Query(value = """
    SELECT e.*, COALESCE(s.hits, 0) as view_count, 
           (SELECT COUNT(*) FROM participation_requests pr 
            WHERE pr.event_id = e.id AND pr.status = 'CONFIRMED') as confirmed_count
    FROM events e
    LEFT JOIN (
        SELECT 
            uri,
            COUNT(CASE WHEN :unique = false THEN 1 END) as hits_all,
            COUNT(DISTINCT CASE WHEN :unique = true THEN ip END) as hits_unique
        FROM endpoint_hits 
        WHERE timestamp BETWEEN :statsStart AND :statsEnd
        AND uri LIKE '/events/%'
        GROUP BY uri
    ) s ON s.uri = CONCAT('/events/', e.id)
    WHERE e.state = 'PUBLISHED'
    AND (:categories IS NULL OR e.category_id IN (:categories))
    AND (:text IS NULL OR 
         LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR 
         LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%')))
    AND (:paid IS NULL OR e.paid = :paid)
    AND e.event_date >= COALESCE(:rangeStart, CURRENT_TIMESTAMP)
    AND (:rangeEnd IS NULL OR e.event_date <= :rangeEnd)
    AND (:onlyAvailable = false OR 
         e.participant_limit = 0 OR 
         (SELECT COUNT(*) FROM participation_requests pr 
          WHERE pr.event_id = e.id AND pr.status = 'CONFIRMED') < e.participant_limit)
    ORDER BY 
        CASE WHEN :sort = 'EVENT_DATE' THEN e.event_date END ASC NULLS LAST,
        CASE WHEN :sort = 'VIEWS' AND :unique = false THEN COALESCE(s.hits_all, 0) END DESC NULLS LAST,
        CASE WHEN :sort = 'VIEWS' AND :unique = true THEN COALESCE(s.hits_unique, 0) END DESC NULLS LAST,
        e.id ASC
    LIMIT :size OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findEventsPublicWithViewsNative(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("sort") String sort,
            @Param("unique") Boolean unique,
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("statsStart") LocalDateTime statsStart,
            @Param("statsEnd") LocalDateTime statsEnd);
}