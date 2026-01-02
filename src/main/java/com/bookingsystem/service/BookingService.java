package com.bookingsystem.service;

import com.bookingsystem.api.dto.BookingCreateDto;
import com.bookingsystem.api.dto.BookingUpdateDto;
import com.bookingsystem.exceptions.BookingNotFoundException;
import com.bookingsystem.exceptions.PaymentNotFoundException;
import com.bookingsystem.exceptions.UnitNotFoundException;
import com.bookingsystem.model.Booking;
import com.bookingsystem.model.Payment;
import com.bookingsystem.model.Unit;
import com.bookingsystem.properties.CancellationTimeProperties;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.bookingsystem.configuration.RedisConfig.UNIT_COUNT_CACHE;
import static com.bookingsystem.model.BookingStatus.AVAILABLE;
import static com.bookingsystem.model.BookingStatus.RESERVED;
import static com.bookingsystem.model.EntityType.BOOKING;
import static com.bookingsystem.model.EventOperation.*;
import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;

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
     * STEP 1: Create booking (Units become RESERVED immediately)</br>
     * STEP 2: Create payment record with 15-minute deadline</br>
     * STEP 3: User must call processPayment() to complete payment
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(
            retryFor = {
                    ObjectOptimisticLockingFailureException.class,
                    PessimisticLockingFailureException.class,
                    CannotAcquireLockException.class,
                    TransientDataAccessException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(
                    delay = 100,      // Start with 100ms
                    multiplier = 2.0, // Double each time: 100ms, 200ms, 400ms
                    maxDelay = 1000   // Cap at 1 second
            )
    )
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public Booking createBooking(BookingCreateDto dto) {
        val user = userService.getUserById(dto.userId());

        val units = Optional.ofNullable(dto.unitIds())
                .map(unitService::findAllById)
                .orElseThrow(() -> new IllegalArgumentException("Unit IDs are required"));

        validateAllUnitsAvailable(units);
        val booking = new Booking(units, user);
        units.forEach(unit -> unit.setBooking(booking));
        val savedBooking = bookingRepository.save(booking);
        unitService.setUnitsBookingStatus(units, RESERVED);

        val paymentDeadline = savedBooking.getCreatedAt().plusMinutes(cancellationTimeProperties.getMinutesValue());
        val payment = new Payment(savedBooking, paymentDeadline);
        paymentRepository.save(payment);

        log.info("Created booking {} for user {} with {} units", savedBooking.getId(), user.getId(), units.size());
        log.info("Payment deadline: {} -- {} minutes", payment.getPaymentDeadline(), cancellationTimeProperties.getMinutesValue());

        eventService.createEvent(
                BOOKING,
                CREATE,
                savedBooking.getId(),
                String.format("Booking created: %s", savedBooking.getId())
        );

        return savedBooking;
    }

    /**
     * Cancel booking - can only cancel if not paid yet</br>
     * Makes units available again
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public void cancelBooking(Long bookingId, Long userId) {
        val booking = getBookingById(bookingId);

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Only booking owner can cancel this booking");
        }

        val payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (payment.isPaid()) {
            throw new IllegalStateException("Cannot cancel a paid booking");
        }

        booking.getUnits().forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(booking.getUnits(), AVAILABLE);

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public Booking updateBooking(Long id, BookingUpdateDto dto) {
        val booking = getBookingById(id);

        if (isNull(dto.unitIds()) || dto.unitIds().isEmpty()) {
            return booking;
        }

        val payment = paymentRepository.findByBookingId(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (payment.isPaid()) {
            throw new IllegalStateException("Cannot update a paid booking");
        }

        val newUnits = unitService.findAllById(dto.unitIds());

        validateAllUnitsAvailable(newUnits);

        val oldUnits = booking.getUnits();

        booking.update(newUnits);
        val updated = bookingRepository.save(booking);

        oldUnits.forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(oldUnits, AVAILABLE);

        newUnits.forEach(unit -> unit.setBooking(updated));
        unitService.setUnitsBookingStatus(newUnits, RESERVED);

        log.info("Updated booking {} - replaced {} old units with {} new units",
                id, oldUnits.size(), newUnits.size());

        eventService.createEvent(
                BOOKING,
                UPDATE,
                id,
                String.format("Booking updated: %s", id)
        );

        return updated;
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    private void validateAllUnitsAvailable(Set<Unit> units) {
        if (units.isEmpty()) {
            throw new UnitNotFoundException("At least one unit must be selected");
        }

        val unavailableUnits = units
                .stream()
                .filter(not(unit -> unit.getStatus().equals(AVAILABLE)))
                .collect(Collectors.toSet());

        if (!unavailableUnits.isEmpty()) {
            String unavailableIds = unavailableUnits.stream()
                    .map(Unit::getId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new UnitNotFoundException("Units are not available: " + unavailableIds);
        }
    }

    /**
     * Recovery method for optimistic locking failures
     * Called after all retry attempts exhausted
     */
    @Recover
    public Booking recoverCreateBooking(
            ObjectOptimisticLockingFailureException e,
            BookingCreateDto dto
    ) {
        log.error("Failed to create booking after retries - Optimistic lock conflict for user {}: {}",
                dto.userId(), e.getMessage());
        throw new IllegalStateException(
                "Unable to create booking due to concurrent modifications. Please try again.", e
        );
    }

    /**
     * Recovery method for pessimistic locking failures (deadlocks)
     */
    @Recover
    public Booking recoverCreateBooking(
            PessimisticLockingFailureException e,
            BookingCreateDto dto
    ) {
        log.error("Failed to create booking after retries - Database deadlock for user {}: {}",
                dto.userId(), e.getMessage());
        throw new IllegalStateException(
                "System is currently experiencing high load. Please try again in a moment.", e
        );
    }

    /**
     * Recovery method for lock acquisition failures
     */
    @Recover
    public Booking recoverCreateBooking(
            CannotAcquireLockException e,
            BookingCreateDto dto
    ) {
        log.error("Failed to create booking after retries - Cannot acquire lock for user {}: {}",
                dto.userId(), e.getMessage());
        throw new IllegalStateException(
                "Unable to process your booking at this time. Please try again shortly.", e
        );
    }

    /**
     * Catch-all recovery for other transient database errors
     */
    @Recover
    public Booking recoverCreateBooking(
            TransientDataAccessException e,
            BookingCreateDto dto
    ) {
        log.error("Failed to create booking after retries - Transient error for user {}: {}",
                dto.userId(), e.getMessage(), e);
        throw new IllegalStateException(
                "A temporary error occurred. Please try again.", e
        );
    }
}
