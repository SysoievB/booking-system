package com.bookingsystem.service;

import com.bookingsystem.model.Booking;
import com.bookingsystem.model.BookingStatus;
import com.bookingsystem.model.Payment;
import com.bookingsystem.properties.CancellationTimeProperties;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.bookingsystem.model.EntityType.PAYMENT;
import static com.bookingsystem.model.EventOperation.UPDATE;

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
    public void checkExpiredPayments() {
        val now = LocalDateTime.now();
        paymentRepository.findAll().stream()
                .filter(p -> !p.isPaid())
                .forEach(payment -> {
                    val timeoutMinutes = cancellationTimeProperties.getMinutesValue();
                    val deadline = payment.getPaymentTimestamp().plusMinutes(timeoutMinutes);

                    if (now.isAfter(deadline)) {
                        try {
                            val booking = payment.getBooking();
                            expireBooking(booking, payment);
                            eventService.createEvent(
                                    PAYMENT,
                                    UPDATE,
                                    payment.getId(),
                                    String.format("Payment expired: %s", payment.getId())
                            );
                            log.info("Auto-expired booking {} due to {}-minute timeout", booking.getId(), timeoutMinutes);
                        } catch (Exception e) {
                            log.error("Error expiring booking for payment {}: {}", payment.getId(), e.getMessage());
                        }
                    }
                });
    }

    private void expireBooking(Booking booking, Payment payment) {
        booking.getUnits().forEach(unit -> unit.setBooking(null));
        unitService.setUnitsBookingStatus(booking.getUnits(), BookingStatus.AVAILABLE);

        paymentRepository.delete(payment);
        bookingRepository.delete(booking);

        log.info("Expired booking {} due to payment timeout", booking.getId());
    }
}
