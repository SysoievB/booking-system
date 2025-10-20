package com.bookingsystem.service;

import com.bookingsystem.api.dto.PaymentResponseDto;
import com.bookingsystem.exceptions.BookingNotFoundException;
import com.bookingsystem.exceptions.PaymentNotFoundException;
import com.bookingsystem.mapper.PaymentMapper;
import com.bookingsystem.model.Unit;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        val user = EntitiesUtil.user().id(USER_ID).build();
        val units = Set.of(mock(Unit.class), mock(Unit.class));
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).units(units).build();
        val payment = EntitiesUtil.payment()
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
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();

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
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();

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
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).booking(booking).paid(true).expired(false).build();

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
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).booking(booking).paid(false).expired(true).build();

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
}
