package com.bookingsystem.repository;

import com.bookingsystem.model.EntityType;
import com.bookingsystem.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEntityType(EntityType entityType);
}
