package com.bookingsystem.service;

import com.bookingsystem.model.Booking;
import com.bookingsystem.model.Payment;
import com.bookingsystem.properties.CancellationTimeProperties;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.bookingsystem.configuration.RedisConfig.UNIT_COUNT_CACHE;
import static com.bookingsystem.model.BookingStatus.AVAILABLE;
import static com.bookingsystem.model.EntityType.BOOKING;
import static com.bookingsystem.model.EntityType.PAYMENT;
import static com.bookingsystem.model.EventOperation.DELETE;
import static com.bookingsystem.model.PaymentStatus.COMPLETED;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentChecker {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UnitService unitService;
    private final CancellationTimeProperties cancellationTimeProperties;
    private final EventService eventService;

    @Scheduled(cron = "${booking.scheduler.payment-check-cron}")
    @Transactional
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public void checkExpiredPayments() {
        val now = LocalDateTime.now();
        val timeoutMinutes = cancellationTimeProperties.getMinutesValue();
        val deadline = now.minusMinutes(timeoutMinutes);

        bookingRepository.findExpiredBookings(deadline)
                .forEach(booking -> {
                    paymentRepository.findByBookingId(booking.getId())
                            .ifPresentOrElse(
                                    payment -> {
                                        if (payment.getStatus() == COMPLETED) {
                                            return;
                                        }
                                        expireBookingWithPayment(booking, payment);
                                    },
                                    () -> expireBookingWithoutPayment(booking)
                            );
                });
    }

    private void expireBookingWithPayment(Booking booking, Payment payment) {
        booking.getUnits().forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(booking.getUnits(), AVAILABLE);

        paymentRepository.delete(payment);
        bookingRepository.delete(booking);

        eventService.createEvent(
                PAYMENT,
                DELETE,
                payment.getId(),
                String.format("Payment expired: %s", payment.getId())
        );
        eventService.createEvent(
                BOOKING,
                DELETE,
                booking.getId(),
                String.format("Booking expired without payment: %s", booking.getId())
        );

        log.info("Expired booking {} with payment due to timeout", booking.getId());
    }

    private void expireBookingWithoutPayment(Booking booking) {
        booking.getUnits().forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(booking.getUnits(), AVAILABLE);

        bookingRepository.delete(booking);

        eventService.createEvent(
                BOOKING,
                DELETE,
                booking.getId(),
                String.format("Booking expired without payment: %s", booking.getId())
        );

        log.info("Expired booking {} without payment", booking.getId());
    }
}