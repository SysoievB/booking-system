package com.bookingsystem.service;

import com.bookingsystem.model.EntityType;
import com.bookingsystem.model.Event;
import com.bookingsystem.model.EventOperation;
import com.bookingsystem.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    @Transactional
    public void createEvent(EntityType entityType, EventOperation eventOperation, Long entityId, String description) {
        val event = new Event(entityType, eventOperation, entityId, description);
        eventRepository.save(event);
        log.info("Created event: {} for {} operation with id {}", entityType, eventOperation, entityId);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<Event> findByEntityType(EntityType entityType) {
        return eventRepository.findByEntityType(entityType);
    }
}
