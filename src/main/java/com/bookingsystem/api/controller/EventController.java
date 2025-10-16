package com.bookingsystem.api.controller;

import com.bookingsystem.model.EntityType;
import com.bookingsystem.model.Event;
import com.bookingsystem.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event audit trail endpoints")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(
            summary = "Get all events",
            description = "Retrieve the complete audit trail of all events in the system. " +
                    "Events track all important operations: create, update, delete for users, units, bookings, and payments."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Event>> getAllEvents() {
        val events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/type/{entityType}")
    @Operation(
            summary = "Get events by entity type",
            description = "Retrieve all events for a specific entity type (USER, UNIT, BOOKING, or PAYMENT)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid entity type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Event>> getEventsByType(
            @Parameter(description = "Entity type (USER, UNIT, BOOKING, PAYMENT)", required = true)
            @PathVariable EntityType entityType
    ) {
        val events = eventService.findByEntityType(entityType);
        return ResponseEntity.ok(events);
    }
}