package ru.practicum.explorewithme.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.CompilationDto;
import ru.practicum.explorewithme.dto.EventShortDto;
import ru.practicum.explorewithme.dto.NewCompilationDto;
import ru.practicum.explorewithme.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.CategoryMapper;
import ru.practicum.explorewithme.mapper.CompilationMapper;
import ru.practicum.explorewithme.mapper.EventMapper;
import ru.practicum.explorewithme.mapper.UserMapper;
import ru.practicum.explorewithme.model.Compilation;
import ru.practicum.explorewithme.model.Event;
import ru.practicum.explorewithme.model.enums.EventState;
import ru.practicum.explorewithme.repository.CompilationRepository;
import ru.practicum.explorewithme.repository.EventRepository;
import ru.practicum.explorewithme.util.PaginationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        if (newCompilationDto.getTitle().length() < 1 || newCompilationDto.getTitle().length() > 50) {
            throw new IllegalArgumentException("Title length must be between 1 and 50 characters");
        }

        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> loadedEvents = eventRepository.findAllById(newCompilationDto.getEvents());
            events = new HashSet<>(loadedEvents);
        }

        Compilation compilation = Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .events(events)
                .build();

        Compilation savedCompilation = compilationRepository.save(compilation);
        log.info("Создана подборка: {}", savedCompilation);

        List<EventShortDto> eventDtos = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = EventShortDto.builder()
                    .id(event.getId())
                    .annotation(event.getAnnotation())
                    .category(CategoryMapper.toCategoryDto(event.getCategory()))
                    .confirmedRequests(0L)
                    .eventDate(event.getEventDate())
                    .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                    .paid(event.getPaid())
                    .title(event.getTitle())
                    .views(0L)
                    .build();
            eventDtos.add(dto);
        }

        return CompilationMapper.toCompilationDto(savedCompilation, eventDtos);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        if (updateRequest.getTitle() != null) {
            if (updateRequest.getTitle().isBlank()) {
                throw new IllegalArgumentException("Title must not be blank");
            }
            if (updateRequest.getTitle().length() > 50) {
                throw new IllegalArgumentException("Title length must not exceed 50 characters");
            }
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        log.info("Обновлена подборка: {}", updatedCompilation);

        List<EventShortDto> eventDtos;
        if (!updatedCompilation.getEvents().isEmpty()) {
            eventDtos = updatedCompilation.getEvents().stream()
                    .filter(event -> event.getState() == EventState.PUBLISHED)
                    .map(event -> {
                        EventShortDto dto = EventMapper.toEventShortDto(event);
                        dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                        return dto;
                    })
                    .collect(Collectors.toList());
        } else {
            eventDtos = Collections.emptyList();
        }

        return CompilationMapper.toCompilationDto(updatedCompilation, eventDtos);
    }


    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
        log.info("Удалена подборка с id={}", compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PaginationUtil.createPageRequest(from, size);

        List<Compilation> compilations;

        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> eventDtos;
                    if (!compilation.getEvents().isEmpty()) {
                        eventDtos = compilation.getEvents().stream()
                                .filter(event -> event.getState() == EventState.PUBLISHED)
                                .map(event -> {
                                    EventShortDto dto = EventMapper.toEventShortDto(event);
                                    dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                                    dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                                    return dto;
                                })
                                .collect(Collectors.toList());
                    } else {
                        eventDtos = Collections.emptyList();
                    }
                    return CompilationMapper.toCompilationDto(compilation, eventDtos);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        List<EventShortDto> eventDtos;
        if (!compilation.getEvents().isEmpty()) {
            eventDtos = compilation.getEvents().stream()
                    .filter(event -> event.getState() == EventState.PUBLISHED)
                    .map(event -> {
                        EventShortDto dto = EventMapper.toEventShortDto(event);
                        dto.setCategory(CategoryMapper.toCategoryDto(event.getCategory()));
                        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator()));
                        return dto;
                    })
                    .collect(Collectors.toList());
        } else {
            eventDtos = Collections.emptyList();
        }

        return CompilationMapper.toCompilationDto(compilation, eventDtos);
    }
}