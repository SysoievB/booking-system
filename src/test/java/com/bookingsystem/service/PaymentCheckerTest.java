package com.bookingsystem.service;

import com.bookingsystem.properties.CancellationTimeProperties;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.bookingsystem.model.BookingStatus.AVAILABLE;
import static com.bookingsystem.model.PaymentStatus.COMPLETED;
import static com.bookingsystem.model.PaymentStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCheckerTest {
    private static final Long BOOKING_ID_1 = 1L;
    private static final Long BOOKING_ID_2 = 2L;
    private static final Long BOOKING_ID_3 = 3L;
    private static final Long PAYMENT_ID_1 = 1L;
    private static final Long PAYMENT_ID_2 = 2L;
    private static final Long UNIT_ID_1 = 1L;
    private static final Long UNIT_ID_2 = 2L;
    private static final int TIMEOUT_MINUTES = 15;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UnitService unitService;

    @Mock
    private CancellationTimeProperties cancellationTimeProperties;

    @Mock
    private EventService eventService;

    @InjectMocks
    private PaymentChecker paymentChecker;

    @Test
    void check_expired_payments_should_expire_booking_with_pending_payment() {
        // given
        val unit1 = EntitiesUtil.unit().id(UNIT_ID_1).build();
        val unit2 = EntitiesUtil.unit().id(UNIT_ID_2).build();
        val units = Set.of(unit1, unit2);
        val booking = EntitiesUtil.booking().id(BOOKING_ID_1).units(units).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID_1).status(PENDING).build();

        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        given(bookingRepository.findExpiredBookings(any())).willReturn(List.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));
        doNothing().when(unitService).setUnitsBookingStatus(any(), any());
        doNothing().when(paymentRepository).delete(any());
        doNothing().when(bookingRepository).delete(any());
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> paymentChecker.checkExpiredPayments());

            verify(bookingRepository).findExpiredBookings(any());
            verify(paymentRepository).findByBookingId(any());
            verify(unit1).setBooking(null);
            verify(unit2).setBooking(null);
            verify(unitService).setUnitsBookingStatus(any(), any());
            verify(paymentRepository).delete(any());
            verify(bookingRepository).delete(any());
            verify(eventService, times(2)).createEvent(any(), any(), any(), any());
        });
    }

    @Test
    void check_expired_payments_should_expire_booking_without_payment() {
        // given
        val unit = EntitiesUtil.unit().id(UNIT_ID_1).build();
        val units = Set.of(unit);
        val booking = EntitiesUtil.booking().id(BOOKING_ID_1).units(units).build();

        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        given(bookingRepository.findExpiredBookings(any())).willReturn(List.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.empty());
        doNothing().when(unitService).setUnitsBookingStatus(any(), any());
        doNothing().when(bookingRepository).delete(any());
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> paymentChecker.checkExpiredPayments());

            verify(bookingRepository).findExpiredBookings(any());
            verify(paymentRepository).findByBookingId(any());
            verify(unit).setBooking(null);
            verify(unitService).setUnitsBookingStatus(any(), any());
            verify(paymentRepository, never()).delete(any());
            verify(bookingRepository).delete(any());
            verify(eventService).createEvent(any(), any(), any(), any());
            verify(eventService, times(1)).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void check_expired_payments_should_skip_booking_with_completed_payment() {
        // given
        val units = Set.of(EntitiesUtil.unit().id(UNIT_ID_1).build());
        val booking = EntitiesUtil.booking().id(BOOKING_ID_1).units(units).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID_1).status(COMPLETED).build();

        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        given(bookingRepository.findExpiredBookings(any())).willReturn(List.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> paymentChecker.checkExpiredPayments());

            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(paymentRepository, never()).delete(any());
            verify(bookingRepository, never()).delete(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void check_expired_payments_should_process_multiple_expired_bookings() {
        // given
        val units1 = Set.of(EntitiesUtil.unit().id(UNIT_ID_1).build());
        val units2 = Set.of(EntitiesUtil.unit().id(UNIT_ID_2).build());
        val units3 = Set.of(EntitiesUtil.unit().id(3L).build());
        val booking1 = EntitiesUtil.booking().id(BOOKING_ID_1).units(units1).build();
        val booking2 = EntitiesUtil.booking().id(BOOKING_ID_2).units(units2).build();
        val booking3 = EntitiesUtil.booking().id(BOOKING_ID_3).units(units3).build();
        val payment1 = EntitiesUtil.payment().id(PAYMENT_ID_1).status(PENDING).build();
        val payment2 = EntitiesUtil.payment().id(PAYMENT_ID_2).status(PENDING).build();

        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        given(bookingRepository.findExpiredBookings(any())).willReturn(List.of(booking1, booking2, booking3));
        given(paymentRepository.findByBookingId(BOOKING_ID_1)).willReturn(Optional.of(payment1));
        given(paymentRepository.findByBookingId(BOOKING_ID_2)).willReturn(Optional.of(payment2));
        given(paymentRepository.findByBookingId(BOOKING_ID_3)).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> paymentChecker.checkExpiredPayments());

            verify(bookingRepository).findExpiredBookings(any());
            verify(paymentRepository, times(3)).findByBookingId(any());
            verify(unitService, times(3)).setUnitsBookingStatus(any(), eq(AVAILABLE));
            verify(paymentRepository, times(2)).delete(any());
            verify(bookingRepository, times(3)).delete(any());
            verify(eventService, times(5)).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void check_expired_payments_should_do_nothing_when_no_expired_bookings() {
        // given
        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        given(bookingRepository.findExpiredBookings(any())).willReturn(Collections.emptyList());

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> paymentChecker.checkExpiredPayments());

            verify(bookingRepository).findExpiredBookings(any());
            verify(paymentRepository, never()).findByBookingId(any());
            verify(unitService, never()).setUnitsBookingStatus(any(), any());
            verify(paymentRepository, never()).delete(any());
            verify(bookingRepository, never()).delete(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void check_expired_payments_should_handle_booking_with_multiple_units() {
        // given
        val unit1 = EntitiesUtil.unit().id(UNIT_ID_1).build();
        val unit2 = EntitiesUtil.unit().id(UNIT_ID_2).build();
        val unit3 = EntitiesUtil.unit().id(3L).build();
        val unit4 = EntitiesUtil.unit().id(4L).build();
        val units = Set.of(unit1, unit2, unit3, unit4);
        val booking = EntitiesUtil.booking().id(BOOKING_ID_1).units(units).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID_1).status(PENDING).build();

        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        given(bookingRepository.findExpiredBookings(any())).willReturn(List.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> paymentChecker.checkExpiredPayments());

            verify(unit1).setBooking(null);
            verify(unit2).setBooking(null);
            verify(unit3).setBooking(null);
            verify(unit4).setBooking(null);
            verify(unitService).setUnitsBookingStatus(any(), any());
            verify(paymentRepository).delete(any());
            verify(bookingRepository).delete(any());
        });
    }
}
