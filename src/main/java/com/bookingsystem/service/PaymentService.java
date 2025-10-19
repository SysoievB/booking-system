package com.bookingsystem.service;

import com.bookingsystem.api.dto.PaymentResponseDto;
import com.bookingsystem.exceptions.BookingNotFoundException;
import com.bookingsystem.exceptions.PaymentNotFoundException;
import com.bookingsystem.mapper.PaymentMapper;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.bookingsystem.configuration.RedisConfig.UNIT_COUNT_CACHE;
import static com.bookingsystem.model.BookingStatus.BOOKED;
import static com.bookingsystem.model.EntityType.PAYMENT;
import static com.bookingsystem.model.EventOperation.CREATE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final UnitService unitService;
    private final PaymentMapper paymentMapper;

    /**
     * EMULATION of payment processing</br>
     * This is called by the user to confirm payment</br>
     * Units remain BOOKED, status changes to COMPLETED</br>
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public PaymentResponseDto processPayment(Long bookingId, Long userId) {
        val booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Only the booking owner can process payment");
        }

        val payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking: " + bookingId));

        if (payment.isPaid()) {
            throw new IllegalStateException("Payment already completed");
        }

        if (payment.isExpired()) {
            throw new IllegalStateException("Payment deadline has passed. Booking has been cancelled.");
        }

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

        return paymentMapper.toDto(paid);
    }

    public PaymentResponseDto getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(paymentMapper::toDto)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
    }

    public List<PaymentResponseDto> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toDto)
                .toList();
    }
}