package com.bookingsystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    EntityType entityType;

    @Enumerated(EnumType.STRING)
    EventOperation eventOperation;

    Long entityId;

    @Column(columnDefinition = "TEXT")
    String description;

    LocalDateTime createdAt;

    public Event(
            EntityType entityType,
            EventOperation eventOperation,
            Long entityId,
            String description
    ) {
        this.entityType = entityType;
        this.eventOperation = eventOperation;
        this.entityId = entityId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}