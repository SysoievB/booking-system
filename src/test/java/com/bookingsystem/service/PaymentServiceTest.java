package com.bookingsystem.service;

import com.bookingsystem.api.dto.PaymentResponseDto;
import com.bookingsystem.exceptions.BookingNotFoundException;
import com.bookingsystem.exceptions.PaymentNotFoundException;
import com.bookingsystem.mapper.PaymentMapper;
import com.bookingsystem.model.Booking;
import com.bookingsystem.model.Payment;
import com.bookingsystem.model.Unit;
import com.bookingsystem.model.User;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final Long PAYMENT_ID = 1L;
    private static final Long BOOKING_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long NON_EXISTENT_BOOKING_ID = 999L;
    private static final double PAYMENT_AMOUNT = 230.0;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventService eventService;

    @Mock
    private UnitService unitService;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void process_payment_should_complete_payment_and_update_units() {
        // given
        val user = user().id(USER_ID).build();
        val units = Set.of(mock(Unit.class), mock(Unit.class));
        val booking = booking().id(BOOKING_ID).user(user).units(units).build();
        val payment = payment()
                .id(PAYMENT_ID)
                .booking(booking)
                .paid(false)
                .expired(false)
                .amount(PAYMENT_AMOUNT)
                .build();
        val paymentDto = mock(PaymentResponseDto.class);

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));
        given(paymentRepository.save(any())).willReturn(payment);
        given(paymentMapper.toDto(any())).willReturn(paymentDto);
        doNothing().when(unitService).setUnitsBookingStatus(any(), any());
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when
        val result = paymentService.processPayment(BOOKING_ID, USER_ID);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(paymentDto, result);
        });
    }

    @Test
    void process_payment_should_throw_exception_when_booking_not_found() {
        // given
        given(bookingRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    BookingNotFoundException.class,
                    () -> paymentService.processPayment(NON_EXISTENT_BOOKING_ID, USER_ID),
                    "Booking not found with id: 999"
            );
            verify(paymentRepository, never()).findByBookingId(any());
            verify(paymentRepository, never()).save(any());
            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void process_payment_should_throw_exception_when_user_not_owner() {
        // given
        val user = user().id(USER_ID).build();
        val booking = booking().id(BOOKING_ID).user(user).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> paymentService.processPayment(BOOKING_ID, OTHER_USER_ID),
                    "Only the booking owner can process payment"
            );

            verify(paymentRepository, never()).findByBookingId(any());
            verify(paymentRepository, never()).save(any());
            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void process_payment_should_throw_exception_when_payment_not_found() {
        // given
        val user = user().id(USER_ID).build();
        val booking = booking().id(BOOKING_ID).user(user).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    PaymentNotFoundException.class,
                    () -> paymentService.processPayment(BOOKING_ID, USER_ID),
                    "Payment not found for booking: 1"
            );

            verify(paymentRepository, never()).save(any());
            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void process_payment_should_throw_exception_when_payment_already_completed() {
        // given
        val user = user().id(USER_ID).build();
        val booking = booking().id(BOOKING_ID).user(user).build();
        val payment = payment().id(PAYMENT_ID).booking(booking).paid(true).expired(false).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> paymentService.processPayment(BOOKING_ID, USER_ID),
                    "Payment already completed"
            );

            verify(paymentRepository, never()).save(any());
            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void process_payment_should_throw_exception_when_payment_expired() {
        // given
        val user = user().id(USER_ID).build();
        val booking = booking().id(BOOKING_ID).user(user).build();
        val payment = payment().id(PAYMENT_ID).booking(booking).paid(false).expired(true).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> paymentService.processPayment(BOOKING_ID, USER_ID),
                    "Payment deadline has passed. Booking has been cancelled."
            );

            verify(payment, never()).markAsPaid();
            verify(paymentRepository, never()).save(any());
            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }


    @Builder(builderMethodName = "user")
    private User getUser(@Nullable Long id) {
        val user = mock(User.class);
        if (id != null) {
            given(user.getId()).willReturn(id);
        }
        return user;
    }

    @Builder(builderMethodName = "booking")
    private Booking getBooking(
            @Nullable Long id,
            @Nullable User user,
            @Nullable Set<Unit> units
    ) {
        val booking = mock(Booking.class, withSettings().strictness(Strictness.LENIENT));
        if (id != null) {
            given(booking.getId()).willReturn(id);
        }
        if (user != null) {
            given(booking.getUser()).willReturn(user);
        }
        if (units != null) {
            given(booking.getUnits()).willReturn(units);
        }
        return booking;
    }

    @Builder(builderMethodName = "payment")
    private Payment getPayment(
            @Nullable Long id,
            @Nullable Booking booking,
            @Nullable Boolean paid,
            @Nullable Boolean expired,
            @Nullable Double amount,
            @Nullable LocalDateTime paidAt
    ) {
        val payment = mock(Payment.class, withSettings().strictness(Strictness.LENIENT));
        if (id != null) {
            given(payment.getId()).willReturn(id);
        }
        if (booking != null) {
            given(payment.getBooking()).willReturn(booking);
        }
        if (paid != null) {
            given(payment.isPaid()).willReturn(paid);
        }
        if (expired != null) {
            given(payment.isExpired()).willReturn(expired);
        }
        if (amount != null) {
            given(payment.getPaymentAmount()).willReturn(amount);
        }
        if (paidAt != null) {
            given(payment.getPaidAt()).willReturn(paidAt);
        }
        return payment;
    }
}
