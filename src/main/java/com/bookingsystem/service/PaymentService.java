package com.bookingsystem.service;

import com.bookingsystem.model.Payment;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bookingsystem.model.BookingStatus.BOOKED;
import static com.bookingsystem.model.EntityType.PAYMENT;
import static com.bookingsystem.model.EventOperation.CREATE;
import static com.bookingsystem.model.EventOperation.DELETE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final UnitService unitService;

    /**
     * EMULATION of payment processing
     * This is called by the user to confirm payment
     * Units remain RESERVED, status changes to COMPLETED
     */
    @Transactional
    public Payment processPayment(Long bookingId, Long userId) {
        // 1. Get booking
        val booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + bookingId));

        // 2. Verify ownership
        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Only the booking owner can process payment");
        }

        // 3. Get payment
        val payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for booking: " + bookingId));

        // 4. Check if already paid
        if (payment.isPaid()) {
            throw new RuntimeException("Payment already completed");
        }

        // 5. Check if expired
        if (payment.isExpired()) {
            throw new RuntimeException("Payment deadline has passed. Booking has been cancelled.");
        }

        // 6. Mark payment as completed
        payment.markAsPaid();
        unitService.setUnitsBookingStatus(booking.getUnits(), BOOKED);

        val paid = paymentRepository.save(payment);

        log.info("Processed payment {} for booking {} by user {}", paid.getId(), bookingId, userId);

        eventService.createEvent(
                PAYMENT,
                CREATE,
                paid.getId(),
                String.format("Payment created: %s", paid.getId())
        );
        log.info("Payment completed at: {}", paid.getPaidAt());

        return paid;
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found for booking: " + bookingId));
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Transactional
    public void delete(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new EntityNotFoundException("Payment not found with id: " + id);
        }
        paymentRepository.deleteById(id);
        eventService.createEvent(
                PAYMENT,
                DELETE,
                id,
                String.format("Payment deleted: %s", id)
        );
    }
}