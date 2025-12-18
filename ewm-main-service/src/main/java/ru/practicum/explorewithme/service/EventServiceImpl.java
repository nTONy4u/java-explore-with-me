package ru.practicum.explorewithme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.EventFullDto;
import ru.practicum.explorewithme.dto.EventShortDto;
import ru.practicum.explorewithme.dto.EventWithViewsDto;
import ru.practicum.explorewithme.dto.NewEventDto;
import ru.practicum.explorewithme.dto.UpdateEventAdminRequest;
import ru.practicum.explorewithme.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.exception.ConflictException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.CategoryMapper;
import ru.practicum.explorewithme.mapper.EventMapper;
import ru.practicum.explorewithme.mapper.UserMapper;
import ru.practicum.explorewithme.model.Category;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.User;
import ru.practicum.explorewithme.model.enums.EventState;
import ru.practicum.explorewithme.model.enums.RequestStatus;
import ru.practicum.explorewithme.model.enums.StateAction;
import ru.practicum.explorewithme.repository.CategoryRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.repository.RequestRepository;
import ru.practicum.explorewithme.repository.UserRepository;
import ru.practicum.explorewithme.repository.specification.EventSpecifications;
import ru.practicum.explorewithme.util.PaginationUtil;
import ru.practicum.explorewithme.util.ValidationUtil;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена"));

        ValidationUtil.validateEventDate(newEventDto.getEventDate(), "Event date");

        ValidationUtil.validateStringLength(newEventDto.getAnnotation(), "Annotation", 20, 2000);
        ValidationUtil.validateStringLength(newEventDto.getDescription(), "Description", 20, 7000);
        ValidationUtil.validateStringLength(newEventDto.getTitle(), "Title", 3, 120);

        Event event = EventMapper.toEvent(newEventDto);
        event.setCategory(category);
        event.setInitiator(user);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);
        log.info("Создано событие: {}", savedEvent);

        EventFullDto dto = EventMapper.toEventFullDto(savedEvent);
        dto.setCategory(CategoryMapper.toCategoryDto(savedEvent.getCategory()));
        dto.setInitiator(UserMapper.toUserShortDto(savedEvent.getInitiator()));
        dto.setConfirmedRequests(0L);
        dto.setViews(0L);

        return dto;
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        Pageable pageable = PaginationUtil.createPageRequest(from, size);

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                    dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                    dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        if (updateRequest.getEventDate() != null) {
            ValidationUtil.validateEventDateForUserUpdate(updateRequest.getEventDate(), event.getEventDate());
        }

        if (updateRequest.getAnnotation() != null) {
            ValidationUtil.validateStringLength(updateRequest.getAnnotation(), "Annotation", 20, 2000);
        }
        if (updateRequest.getDescription() != null) {
            ValidationUtil.validateStringLength(updateRequest.getDescription(), "Description", 20, 7000);
        }
        if (updateRequest.getTitle() != null) {
            ValidationUtil.validateStringLength(updateRequest.getTitle(), "Title", 3, 120);
        }

        updateEventFields(event, updateRequest);

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (updateRequest.getStateAction() == StateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие пользователем: {}", updatedEvent);

        EventFullDto dto = EventMapper.toEventFullDto(updatedEvent);
        dto.setCategory(CategoryMapper.toCategoryDto(updatedEvent.getCategory()));
        dto.setInitiator(UserMapper.toUserShortDto(updatedEvent.getInitiator()));
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        return dto;
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states,
                                               List<Long> categories, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, Integer from, Integer size) {
        Sort sort = Sort.by("createdOn").descending();
        Pageable pageable = PaginationUtil.createPageRequest(from, size, sort);

        Specification<Event> spec = Specification
                .where(EventSpecifications.hasUsers(users))
                .and(EventSpecifications.hasStates(states))
                .and(EventSpecifications.hasCategories(categories))
                .and(EventSpecifications.hasDateRange(rangeStart, rangeEnd));

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        List<Event> events = eventPage.getContent();

        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventMapper.toEventFullDto(event);
                    dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                    dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                    dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (updateRequest.getStateAction() == StateAction.PUBLISH_EVENT) {
            LocalDateTime eventDateToCheck = updateRequest.getEventDate() != null
                    ? updateRequest.getEventDate()
                    : event.getEventDate();

            ValidationUtil.validateEventDateForAdminPublish(eventDateToCheck);

        } else if (updateRequest.getEventDate() != null) {
            ValidationUtil.validateEventDateForAdminUpdate(
                    updateRequest.getEventDate(),
                    event.getEventDate(),
                    event.getPublishedOn()
            );
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == StateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException(
                            "Событие можно публиковать, только если оно в состоянии ожидания публикации"
                    );
                }

                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());

            } else if (updateRequest.getStateAction() == StateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException(
                            "Событие можно отклонить, только если оно еще не опубликовано"
                    );
                }
                event.setState(EventState.CANCELED);
            }
        }

        if (updateRequest.getAnnotation() != null) {
            ValidationUtil.validateStringLength(updateRequest.getAnnotation(), "Annotation", 20, 2000);
        }
        if (updateRequest.getDescription() != null) {
            ValidationUtil.validateStringLength(updateRequest.getDescription(), "Description", 20, 7000);
        }
        if (updateRequest.getTitle() != null) {
            ValidationUtil.validateStringLength(updateRequest.getTitle(), "Title", 3, 120);
        }

        updateEventFields(event, updateRequest);

        Event updatedEvent = eventRepository.save(event);
        log.info("Обновлено событие администратором: {}", updatedEvent);

        EventFullDto dto = EventMapper.toEventFullDto(updatedEvent);
        dto.setCategory(CategoryMapper.toCategoryDto(updatedEvent.getCategory()));
        dto.setInitiator(UserMapper.toUserShortDto(updatedEvent.getInitiator()));
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        return dto;
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort, Integer from, Integer size) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Дата начала должна быть раньше даты окончания");
        }

        PaginationUtil.validatePaginationParams(from, size);

        LocalDateTime statsStart = rangeStart != null ? rangeStart : LocalDateTime.now().minusYears(1);
        LocalDateTime statsEnd = rangeEnd != null ? rangeEnd : LocalDateTime.now().plusYears(1);

        if ("VIEWS".equals(sort)) {
            return getEventsPublicWithNativeQuery(text, categories, paid, rangeStart, rangeEnd,
                    onlyAvailable, sort, from, size, statsStart, statsEnd);
        } else {
            return getEventsPublicWithSpecification(text, categories, paid, rangeStart, rangeEnd,
                    onlyAvailable, sort, from, size, statsStart, statsEnd);
        }
    }

    private List<EventShortDto> getEventsPublicWithNativeQuery(String text, List<Long> categories, Boolean paid,
                                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                               Boolean onlyAvailable, String sort, Integer from,
                                                               Integer size, LocalDateTime statsStart,
                                                               LocalDateTime statsEnd) {

        List<Object[]> results = eventRepository.findEventsPublicWithViewsNative(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                sort, false, from, size, statsStart, statsEnd);

        List<EventWithViewsDto> eventsWithViews = results.stream()
                .map(this::mapToEventWithViews)
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(onlyAvailable)) {
            eventsWithViews = filterEventsByAvailability(eventsWithViews);
        }

        return eventsWithViews.stream()
                .map(this::convertToEventShortDto)
                .collect(Collectors.toList());
    }

    private List<EventWithViewsDto> filterEventsByAvailability(List<EventWithViewsDto> events) {
        return events.stream()
                .filter(event -> {
                    Long confirmedRequests = getConfirmedRequests(event.getId());
                    return event.getParticipantLimit() == 0 ||
                           event.getParticipantLimit() > confirmedRequests;
                })
                .collect(Collectors.toList());
    }

    private EventWithViewsDto mapToEventWithViews(Object[] row) {
        return EventWithViewsDto.builder()
                .id(((Number) row[0]).longValue())
                .annotation((String) row[1])
                .categoryId(((Number) row[2]).longValue())
                .eventDate(((java.sql.Timestamp) row[5]).toLocalDateTime())
                .initiatorId(((Number) row[6]).longValue())
                .lat(row[7] != null ? ((Number) row[7]).floatValue() : null)
                .lon(row[8] != null ? ((Number) row[8]).floatValue() : null)
                .paid((Boolean) row[9])
                .participantLimit(row[10] != null ? ((Number) row[10]).intValue() : 0)
                .publishedOn(row[11] != null ? ((java.sql.Timestamp) row[11]).toLocalDateTime() : null)
                .requestModeration(row[12] != null ? (Boolean) row[12] : true)
                .title((String) row[13])
                .views(row.length > 14 ? ((Number) row[14]).longValue() : 0L)
                .confirmedRequests(row.length > 15 ? ((Number) row[15]).longValue() : 0L)
                .build();
    }

    private EventShortDto convertToEventShortDto(EventWithViewsDto eventWithViews) {
        Category category = categoryRepository.findById(eventWithViews.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        User initiator = userRepository.findById(eventWithViews.getInitiatorId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Long confirmedRequests = getConfirmedRequests(eventWithViews.getId());

        return EventShortDto.builder()
                .id(eventWithViews.getId())
                .annotation(eventWithViews.getAnnotation())
                .category(CategoryMapper.toCategoryDto(category))
                .confirmedRequests(confirmedRequests)
                .eventDate(eventWithViews.getEventDate())
                .initiator(UserMapper.toUserShortDto(initiator))
                .paid(eventWithViews.getPaid())
                .title(eventWithViews.getTitle())
                .views(eventWithViews.getViews())
                .build();
    }

    private Long getConfirmedRequests(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    private List<EventShortDto> getEventsPublicWithSpecification(String text, List<Long> categories, Boolean paid,
                                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                                 Boolean onlyAvailable, String sort, Integer from,
                                                                 Integer size, LocalDateTime statsStart,
                                                                 LocalDateTime statsEnd) {

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
            log.debug("rangeStart не указан, используется текущее время: {}", rangeStart);
        }

        if (rangeEnd != null && rangeEnd.isBefore(LocalDateTime.now())) {
            log.debug("rangeEnd в прошлом, события не будут найдены");
            return List.of();
        }

        Pageable pageable;
        if ("EVENT_DATE".equals(sort)) {
            pageable = PaginationUtil.createPageRequest(from, size, Sort.by("eventDate"));
        } else {
            pageable = PaginationUtil.createPageRequest(from, size);
        }

        Specification<Event> spec = Specification
                .where(EventSpecifications.hasState(EventState.PUBLISHED))
                .and(EventSpecifications.hasText(text))
                .and(EventSpecifications.hasCategories(categories))
                .and(EventSpecifications.hasPaid(paid))
                .and(EventSpecifications.hasDateRange(rangeStart, rangeEnd));

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);
        List<Event> events = eventPage.getContent();

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            event.getParticipantLimit() > requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED))
                    .collect(Collectors.toList());
        }

        Map<Long, Long> viewsMap = getEventsViews(events, statsStart, statsEnd);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                    dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                    dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventPublic(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id=" + eventId + " не опубликовано");
        }

        EventFullDto dto = EventMapper.toEventFullDto(event);
        dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
        dto.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED));

        Map<Long, Long> viewsMap = getEventsViews(List.of(event),
                LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1));
        dto.setViews(viewsMap.getOrDefault(eventId, 0L));

        return dto;
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.setLat(updateRequest.getLocation().getLat());
            event.setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void updateEventFields(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(updateRequest.getEventDate());
        }
        if (updateRequest.getLocation() != null) {
            event.setLat(updateRequest.getLocation().getLat());
            event.setLon(updateRequest.getLocation().getLon());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }


    private Map<Long, Long> getEventsViews(List<Event> events, LocalDateTime start, LocalDateTime end) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        try {
            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);

            Map<Long, Long> viewsMap = new HashMap<>();
            for (ViewStats stat : stats) {
                String uri = stat.getUri();
                if (uri.startsWith("/events/")) {
                    try {
                        Long eventId = Long.parseLong(uri.substring("/events/".length()));
                        viewsMap.put(eventId, stat.getHits());
                    } catch (NumberFormatException e) {
                        log.warn("Не удалось извлечь id события из URI: {}", uri);
                    }
                }
            }

            events.forEach(event -> viewsMap.putIfAbsent(event.getId(), 0L));

            return viewsMap;
        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage());
            return events.stream().collect(Collectors.toMap(Event::getId, event -> 0L));
        }
    }
}