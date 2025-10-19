package com.bookingsystem.api.controller;

import com.bookingsystem.api.dto.BookingCreateDto;
import com.bookingsystem.api.dto.BookingUpdateDto;
import com.bookingsystem.model.Booking;
import com.bookingsystem.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(
            summary = "Create a new booking",
            description = "Book one or more units for a user. Units are immediately marked as RESERVED. " +
                    "Payment must be completed within 15 minutes or the booking will be automatically cancelled. " +
                    "Units become unavailable for other users immediately upon booking creation.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "BookingCreateDto request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookingCreateDto.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or units not available"),
            @ApiResponse(responseCode = "404", description = "User or units not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody BookingCreateDto dto) {
        val booking = bookingService.createBooking(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update booking",
            description = "Update an existing booking by changing units. " +
                    "Can only update bookings that haven't been paid yet. " +
                    "Old units will be freed and new units will be reserved.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "BookingUpdateDto request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BookingUpdateDto.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking updated successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot update paid booking or invalid input"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Booking> updateBooking(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody BookingUpdateDto dto
    ) {
        val booking = bookingService.updateBooking(id, dto);
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel a booking",
            description = "Cancel a booking and free up the reserved units. " +
                    "Only the booking owner can cancel. " +
                    "Cannot cancel bookings that have already been paid. " +
                    "Units will become AVAILABLE again after cancellation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel paid booking"),
            @ApiResponse(responseCode = "403", description = "Only booking owner can cancel"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long id,

            @Parameter(description = "User ID requesting cancellation", required = true)
            @RequestParam Long userId
    ) {
        bookingService.cancelBooking(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get booking by ID",
            description = "Retrieve detailed information about a specific booking"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Booking> getBookingById(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long id
    ) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @GetMapping
    @Operation(
            summary = "Get all bookings",
            description = "Retrieve a list of all bookings in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Booking>> getAllBookings() {
        val bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
}
