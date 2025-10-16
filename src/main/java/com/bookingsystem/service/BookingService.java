package com.bookingsystem.service;

import com.bookingsystem.api.dto.BookingCreateDto;
import com.bookingsystem.api.dto.BookingUpdateDto;
import com.bookingsystem.model.*;
import com.bookingsystem.properties.CancellationTimeProperties;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bookingsystem.model.BookingStatus.AVAILABLE;
import static com.bookingsystem.model.BookingStatus.RESERVED;
import static com.bookingsystem.model.EntityType.BOOKING;
import static com.bookingsystem.model.EntityType.USER;
import static com.bookingsystem.model.EventOperation.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final UnitService unitService;
    private final PaymentRepository paymentRepository;
    private final CancellationTimeProperties cancellationTimeProperties;
    private final EventService eventService;

    /**
     * STEP 1: Create booking (Units become RESERVED immediately)
     * STEP 2: Create payment record with 15-minute deadline
     * STEP 3: User must call processPayment() to complete payment
     */
    @Transactional
    public Booking createBooking(BookingCreateDto dto) {
        // 1. Get user
        val user = userService.getUserById(dto.userId());

        // 2. Get units
        val units = Optional.ofNullable(dto.unitIds())
                .map(unitService::findAllById)
                .orElseThrow(() -> new IllegalArgumentException("Unit IDs are required"));

        // 3. Validate units are available
        validateAllUnitsAvailable(units);

        // 4. Create booking entity
        val booking = new Booking(units, user);
        val savedBooking = bookingRepository.save(booking);

        // 5. Mark units as RESERVED (booked, waiting for payment)
        // Units are locked from this moment - other users cannot book them
        unitService.setUnitsBookingStatus(units, RESERVED);

        // 6. Create payment record with deadline
        val paymentTimeoutMinutes = LocalDateTime.now().plusMinutes(cancellationTimeProperties.getMinutesValue());
        val payment = new Payment(savedBooking, paymentTimeoutMinutes);
        paymentRepository.save(payment);

        log.info("Created booking {} for user {} with {} units", savedBooking.getId(), user.getId(), units.size());
        log.info("Payment deadline: {} (in {} minutes)", payment.getPaymentDeadline(), paymentTimeoutMinutes);

        eventService.createEvent(
                BOOKING,
                CREATE,
                savedBooking.getId(),
                String.format("Booking created: %s", savedBooking.getId())
        );

        return savedBooking;
    }

    /**
     * Cancel booking - can only cancel if not paid yet
     * Makes units available again
     */
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        val booking = getBookingById(bookingId);

        // Verify ownership
        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Only booking owner can cancel this booking");
        }

        // Get payment
        val payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        // Cannot cancel paid bookings
        if (payment.isPaid()) {
            throw new IllegalStateException("Cannot cancel a paid booking");
        }

        // Free up units
        booking.getUnits().forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(booking.getUnits(), AVAILABLE);

        // Delete payment and booking
        paymentRepository.delete(payment);
        bookingRepository.delete(booking);

        eventService.createEvent(
                BOOKING,
                DELETE,
                booking.getId(),
                String.format("Booking deleted: %s", booking.getId())
        );

        log.info("Cancelled booking {} by user {}", bookingId, userId);
    }

    @Transactional
    public Booking updateBooking(Long id, BookingUpdateDto dto) {
        val booking = getBookingById(id);

        // Check if already paid - cannot update paid bookings
        val payment = paymentRepository.findByBookingId(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        if (payment.isPaid()) {
            throw new IllegalStateException("Cannot update a paid booking");
        }

        // Free up old units
        booking.getUnits().forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(booking.getUnits(), AVAILABLE);

        // Get new units if provided
        val newUnits = Optional.ofNullable(dto.unitIds())
                .map(unitService::findAllById)
                .orElse(null);

        // Get new user if provided
        val newUser = Optional.ofNullable(dto.userId())
                .map(userService::getUserById)
                .orElse(null);

        // Update booking
        booking.update(newUnits, newUser);
        Booking updated = bookingRepository.save(booking);

        // Mark new units as reserved
        if (newUnits != null && !newUnits.isEmpty()) {
            newUnits.forEach(unit -> unit.setBooking(updated));
            unitService.setUnitsBookingStatus(newUnits, RESERVED);
        }

        log.info("Updated booking {}", id);

        eventService.createEvent(
                BOOKING,
                UPDATE,
                id,
                String.format("Booking updated: %s", id)
        );

        return updated;
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);

        eventService.createEvent(
                BOOKING,
                DELETE,
                id,
                String.format("Booking deleted: %s", id)
        );
    }

    private void validateAllUnitsAvailable(Set<Unit> units) {
        if (units.isEmpty()) {
            throw new RuntimeException("At least one unit must be selected");
        }

        val unavailableUnits = units
                .stream()
                .filter(unit -> !unit.getStatus().equals(AVAILABLE))
                .collect(Collectors.toSet());

        if (!unavailableUnits.isEmpty()) {
            String unavailableIds = unavailableUnits.stream()
                    .map(Unit::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new RuntimeException("Units are not available: " + unavailableIds);
        }
    }
}
