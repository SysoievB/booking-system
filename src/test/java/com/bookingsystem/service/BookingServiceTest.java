package com.bookingsystem.service;

import com.bookingsystem.api.dto.BookingCreateDto;
import com.bookingsystem.api.dto.BookingUpdateDto;
import com.bookingsystem.exceptions.BookingNotFoundException;
import com.bookingsystem.exceptions.PaymentNotFoundException;
import com.bookingsystem.exceptions.UnitNotFoundException;
import com.bookingsystem.model.Unit;
import com.bookingsystem.properties.CancellationTimeProperties;
import com.bookingsystem.repository.BookingRepository;
import com.bookingsystem.repository.PaymentRepository;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.bookingsystem.model.BookingStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final Long BOOKING_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long UNIT_ID_1 = 1L;
    private static final Long UNIT_ID_2 = 2L;
    private static final Long PAYMENT_ID = 1L;
    private static final Long NON_EXISTENT_BOOKING_ID = 999L;
    private static final int TIMEOUT_MINUTES = 15;
    private static final LocalDateTime CREATED_AT = LocalDateTime.now();

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserService userService;

    @Mock
    private UnitService unitService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CancellationTimeProperties cancellationTimeProperties;

    @Mock
    private EventService eventService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void create_booking_should_create_booking_with_payment_and_reserve_units() {
        // given
        val user = EntitiesUtil.user().build();
        val unit1 = EntitiesUtil.unit().id(UNIT_ID_1).status(AVAILABLE).build();
        val unit2 = EntitiesUtil.unit().id(UNIT_ID_2).status(AVAILABLE).build();
        val units = Set.of(unit1, unit2);
        val unitIds = Set.of(UNIT_ID_1, UNIT_ID_2);
        val dto = new BookingCreateDto(unitIds, USER_ID);
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).units(units).createdAt(CREATED_AT).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).paid(false).build();

        given(userService.getUserById(any())).willReturn(user);
        given(unitService.findAllById(any())).willReturn(units);
        given(bookingRepository.save(any())).willReturn(booking);
        given(cancellationTimeProperties.getMinutesValue()).willReturn(TIMEOUT_MINUTES);
        doNothing().when(unitService).setUnitsBookingStatus(any(), any());
        given(paymentRepository.save(any())).willReturn(payment);
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when
        val result = bookingService.createBooking(dto);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(BOOKING_ID, result.getId());
            assertEquals(user, result.getUser());
        });
    }

    @Test
    void create_booking_should_throw_exception_when_unit_ids_null() {
        // given
        val dto = new BookingCreateDto(null, USER_ID);
        val user = EntitiesUtil.user().build();

        given(userService.getUserById(any())).willReturn(user);

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.createBooking(dto),
                    "Unit IDs are required"
            );

            verify(unitService, never()).findAllById(any());
            verify(bookingRepository, never()).save(any());
            verify(paymentRepository, never()).save(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void create_booking_should_throw_exception_when_units_empty() {
        // given
        val user = EntitiesUtil.user().build();
        val unitIds = Set.of(UNIT_ID_1);
        val emptyUnits = Collections.<Unit>emptySet();
        val dto = new BookingCreateDto(unitIds, USER_ID);

        given(userService.getUserById(any())).willReturn(user);
        given(unitService.findAllById(any())).willReturn(emptyUnits);

        // when & then
        assertAll(() -> {
            assertThrows(
                    UnitNotFoundException.class,
                    () -> bookingService.createBooking(dto),
                    "At least one unit must be selected"
            );

            verify(bookingRepository, never()).save(any());
            verify(paymentRepository, never()).save(any());
        });
    }

    @Test
    void create_booking_should_throw_exception_when_units_not_available() {
        // given
        val user = EntitiesUtil.user().build();
        val unit1 = EntitiesUtil.unit().id(UNIT_ID_1).status(RESERVED).build();
        val unit2 = EntitiesUtil.unit().id(UNIT_ID_2).status(BOOKED).build();
        val units = Set.of(unit1, unit2);
        val unitIds = Set.of(UNIT_ID_1, UNIT_ID_2);
        val dto = new BookingCreateDto(unitIds, USER_ID);

        given(userService.getUserById(any())).willReturn(user);
        given(unitService.findAllById(any())).willReturn(units);

        // when & then
        assertAll(() -> {
            val exception = assertThrows(
                    UnitNotFoundException.class,
                    () -> bookingService.createBooking(dto)
            );

            assertTrue(exception.getMessage().contains("Units are not available"));

            verify(bookingRepository, never()).save(any());
            verify(paymentRepository, never()).save(any());
        });
    }

    @Test
    void cancel_booking_should_delete_booking_and_free_units() {
        // given
        val user = EntitiesUtil.user().id(USER_ID).build();
        val unit1 = EntitiesUtil.unit().id(UNIT_ID_1).status(RESERVED).build();
        val unit2 = EntitiesUtil.unit().id(UNIT_ID_2).status(RESERVED).build();
        val units = Set.of(unit1, unit2);
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).units(units).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).paid(false).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));
        doNothing().when(unitService).setUnitsBookingStatus(any(), any());
        doNothing().when(paymentRepository).delete(any());
        doNothing().when(bookingRepository).delete(booking);
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when & then
        assertAll(() -> {
            assertDoesNotThrow(() -> bookingService.cancelBooking(BOOKING_ID, USER_ID));

            verify(bookingRepository).findById(any());
            verify(paymentRepository).findByBookingId(any());
            verify(unit1).setBooking(null);
            verify(unit2).setBooking(null);
            verify(unitService).setUnitsBookingStatus(any(), any());
            verify(paymentRepository).delete(any());
            verify(bookingRepository).delete(any());
            verify(eventService).createEvent(any(), any(), any(), any());
        });
    }

    @Test
    void cancel_booking_should_throw_exception_when_user_is_not_owner() {
        // given
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> bookingService.cancelBooking(BOOKING_ID, OTHER_USER_ID),
                    "Only booking owner can cancel this booking"
            );

            verify(paymentRepository, never()).findByBookingId(any());
            verify(paymentRepository, never()).delete(any());
            verify(bookingRepository, never()).delete(any());
        });
    }

    @Test
    void cancel_booking_should_throw_exception_when_payment_not_found() {
        // given
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    PaymentNotFoundException.class,
                    () -> bookingService.cancelBooking(BOOKING_ID, USER_ID),
                    "Payment not found"
            );

            verify(paymentRepository, never()).delete(any());
            verify(bookingRepository, never()).delete(any());
        });
    }

    @Test
    void cancel_booking_should_throw_exception_when_payment_already_paid() {
        // given
        val user = EntitiesUtil.user().id(USER_ID).build();
        val booking = EntitiesUtil.booking().id(BOOKING_ID).user(user).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).paid(true).build();

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> bookingService.cancelBooking(BOOKING_ID, USER_ID),
                    "Cannot cancel a paid booking"
            );

            verify(paymentRepository, never()).delete(any());
            verify(bookingRepository, never()).delete(any());
        });
    }

    @Test
    void update_booking_should_return_unchanged_when_unit_ids_null() {
        // given
        val booking = EntitiesUtil.booking().id(BOOKING_ID).build();
        val dto = new BookingUpdateDto(null);

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));

        // when
        val result = bookingService.updateBooking(BOOKING_ID, dto);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(booking, result);

            verify(paymentRepository, never()).findByBookingId(any());
            verify(unitService, never()).findAllById(any());
            verify(bookingRepository, never()).save(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void update_booking_should_return_unchanged_when_unit_ids_empty() {
        // given
        val booking = EntitiesUtil.booking().id(BOOKING_ID).build();
        val dto = new BookingUpdateDto(Set.of());

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));

        // when
        val result = bookingService.updateBooking(BOOKING_ID, dto);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(booking, result);

            verify(paymentRepository, never()).findByBookingId(any());
        });
    }

    @Test
    void update_booking_should_throw_exception_when_booking_not_found() {
        // given
        val dto = new BookingUpdateDto(Set.of(UNIT_ID_1));

        given(bookingRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    BookingNotFoundException.class,
                    () -> bookingService.updateBooking(NON_EXISTENT_BOOKING_ID, dto),
                    "Booking not found with id: 999"
            );

            verify(paymentRepository, never()).findByBookingId(any());
        });
    }

    @Test
    void update_booking_should_throw_exception_when_payment_not_found() {
        // given
        val booking = EntitiesUtil.booking().id(BOOKING_ID).build();
        val dto = new BookingUpdateDto(Set.of(UNIT_ID_1));

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    PaymentNotFoundException.class,
                    () -> bookingService.updateBooking(BOOKING_ID, dto),
                    "Payment not found"
            );
        });
    }

    @Test
    void update_booking_should_throw_exception_when_payment_already_paid() {
        // given
        val booking = EntitiesUtil.booking().id(BOOKING_ID).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).paid(true).build();
        val dto = new BookingUpdateDto(Set.of(UNIT_ID_1));

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));

        // when & then
        assertAll(() -> {
            assertThrows(
                    IllegalStateException.class,
                    () -> bookingService.updateBooking(BOOKING_ID, dto),
                    "Cannot update a paid booking"
            );

            verify(unitService, never()).findAllById(any());
        });
    }

    @Test
    void update_booking_should_throw_exception_when_new_units_not_available() {
        // given
        val oldUnits = Set.of(EntitiesUtil.unit().id(UNIT_ID_1).status(RESERVED).build());
        val booking = EntitiesUtil.booking().id(BOOKING_ID).units(oldUnits).build();
        val payment = EntitiesUtil.payment().id(PAYMENT_ID).paid(false).build();
        val newUnit = EntitiesUtil.unit().id(3L).status(BOOKED).build();
        val newUnits = Set.of(newUnit);
        val newUnitIds = Set.of(3L);
        val dto = new BookingUpdateDto(newUnitIds);

        given(bookingRepository.findById(any())).willReturn(Optional.of(booking));
        given(paymentRepository.findByBookingId(any())).willReturn(Optional.of(payment));
        given(unitService.findAllById(newUnitIds)).willReturn(newUnits);

        // when & then
        assertAll(() -> {
            assertThrows(
                    UnitNotFoundException.class,
                    () -> bookingService.updateBooking(BOOKING_ID, dto),
                    "Units are not available: 3"
            );

            verify(bookingRepository, never()).save(any());
        });
    }
}