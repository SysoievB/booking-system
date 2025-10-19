package com.bookingsystem.service;

import com.bookingsystem.api.dto.UnitCreateDto;
import com.bookingsystem.api.dto.UnitUpdateDto;
import com.bookingsystem.exceptions.UnitNotFoundException;
import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.BookingStatus;
import com.bookingsystem.model.Unit;
import com.bookingsystem.repository.UnitRepository;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.bookingsystem.model.AccommodationType.APARTMENT;
import static com.bookingsystem.model.AccommodationType.HOME;
import static com.bookingsystem.model.BookingStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {
    private static final Long UNIT_ID = 1L;
    private static final Long NON_EXISTENT_ID = 999L;
    private static final int NUMBER_OF_ROOMS = 3;
    private static final int FLOOR = 2;
    private static final double BASE_COST = 100.0;
    private static final String DESCRIPTION = "Spacious apartment";
    private static final LocalDate BOOKING_DATE = LocalDate.now();

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private UnitService unitService;

    @Test
    void create_unit_should_save_unit_and_create_event() {
        // given
        val dto = new UnitCreateDto(
                NUMBER_OF_ROOMS,
                APARTMENT,
                FLOOR,
                BASE_COST,
                BOOKING_DATE,
                DESCRIPTION
        );
        val unit = unit().id(UNIT_ID).numberOfRooms(NUMBER_OF_ROOMS).type(APARTMENT).build();

        given(unitRepository.save(any(Unit.class))).willReturn(unit);
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when
        val result = unitService.createUnit(dto);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(UNIT_ID, result.getId());
            assertEquals(NUMBER_OF_ROOMS, result.getNumberOfRooms());
            assertEquals(APARTMENT, result.getType());
        });
    }

    @Test
    void update_unit_should_update_existing_unit_and_create_event() {
        // given
        val existingUnit = unit().id(UNIT_ID).numberOfRooms(2).type(APARTMENT).status(AVAILABLE).build();
        val dto = new UnitUpdateDto(
                NUMBER_OF_ROOMS,
                HOME,
                RESERVED,
                FLOOR,
                BOOKING_DATE,
                BASE_COST,
                DESCRIPTION
        );

        given(unitRepository.findById(any())).willReturn(Optional.of(existingUnit));
        given(existingUnit.update(any(), any(), any(), any(), any(), any(), any())).willReturn(existingUnit);
        given(unitRepository.save(any())).willReturn(existingUnit);
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when
        val result = unitService.updateUnit(UNIT_ID, dto);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(UNIT_ID, result.getId());
        });
    }

    @Test
    void update_unit_should_throw_exception_when_unit_not_found() {
        // given
        val dto = new UnitUpdateDto(NUMBER_OF_ROOMS, APARTMENT, AVAILABLE, FLOOR, BOOKING_DATE, BASE_COST, DESCRIPTION);
        given(unitRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    UnitNotFoundException.class,
                    () -> unitService.updateUnit(NON_EXISTENT_ID, dto),
                    "Unit not found with id: 999"
            );

            verify(unitRepository, never()).save(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void get_unit_by_id_should_return_unit_when_exists() {
        // given
        val unit = unit().id(UNIT_ID).numberOfRooms(NUMBER_OF_ROOMS).type(APARTMENT).build();
        given(unitRepository.findById(any())).willReturn(Optional.of(unit));

        // when
        val result = unitService.getUnitById(UNIT_ID);

        // then
        assertAll(() -> {
            assertNotNull(result);
            assertEquals(UNIT_ID, result.getId());
            assertEquals(NUMBER_OF_ROOMS, result.getNumberOfRooms());
            assertEquals(APARTMENT, result.getType());

            verify(unitRepository).findById(any());
            verifyNoInteractions(eventService);
        });
    }

    @Test
    void get_unit_by_id_should_throw_exception_when_unit_not_found() {
        // given
        given(unitRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertAll(() -> {
            assertThrows(
                    UnitNotFoundException.class,
                    () -> unitService.getUnitById(NON_EXISTENT_ID),
                    "Unit not found with id: 999"
            );

            verify(unitRepository).findById(any());
            verifyNoInteractions(eventService);
        });
    }

    @Test
    void delete_unit_should_delete_existing_unit_and_create_event() {
        // given
        given(unitRepository.existsById(any())).willReturn(true);
        doNothing().when(unitRepository).deleteById(any());
        doNothing().when(eventService).createEvent(any(), any(), anyLong(), anyString());

        // when
        unitService.deleteUnit(UNIT_ID);

        // then
        assertAll(() -> {
            verify(unitRepository).existsById(any());
            verify(unitRepository).deleteById(any());
            verify(eventService).createEvent(any(), any(), any(), any());
        });
    }

    @Test
    void delete_unit_should_throw_exception_when_unit_not_found() {
        // given
        given(unitRepository.existsById(any())).willReturn(false);

        // when & then
        assertAll(() -> {
            assertThrows(
                    UnitNotFoundException.class,
                    () -> unitService.deleteUnit(NON_EXISTENT_ID),
                    "Unit not found with id: 999"
            );

            verify(unitRepository, never()).deleteById(any());
            verify(eventService, never()).createEvent(any(), any(), anyLong(), anyString());
        });
    }

    @Test
    void set_units_booking_status_should_update_status_for_all_units() {
        // given
        val unit1 = unit().id(1L).status(RESERVED).build();
        val unit2 = unit().id(2L).status(RESERVED).build();
        val unit3 = unit().id(3L).status(RESERVED).build();
        val units = Set.of(unit1, unit2, unit3);

        given(unitRepository.save(any())).willReturn(unit1);
        given(unitRepository.save(any())).willReturn(unit2);
        given(unitRepository.save(any())).willReturn(unit3);

        // when
        unitService.setUnitsBookingStatus(units, BOOKED);

        // then
        assertAll(() -> {
            verify(unit1).setStatus(BOOKED);
            verify(unit2).setStatus(BOOKED);
            verify(unit3).setStatus(BOOKED);
            verify(unitRepository, times(3)).save(any());
            verifyNoInteractions(eventService);
        });
    }

    @Builder(builderMethodName = "unit")
    private Unit getUnit(
            @Nullable Long id,
            @Nullable Integer numberOfRooms,
            @Nullable AccommodationType type,
            @Nullable BookingStatus status,
            @Nullable Integer floor,
            @Nullable Double baseCost,
            @Nullable LocalDate bookingDate,
            @Nullable String description
    ) {
        val unit = mock(Unit.class, withSettings().strictness(Strictness.LENIENT));
        if (id != null) {
            given(unit.getId()).willReturn(id);
        }
        if (numberOfRooms != null) {
            given(unit.getNumberOfRooms()).willReturn(numberOfRooms);
        }
        if (type != null) {
            given(unit.getType()).willReturn(type);
        }
        if (status != null) {
            given(unit.getStatus()).willReturn(status);
        }
        if (floor != null) {
            given(unit.getFloor()).willReturn(floor);
        }
        if (baseCost != null) {
            given(unit.getBaseCost()).willReturn(baseCost);
        }
        if (bookingDate != null) {
            given(unit.getBookingDate()).willReturn(bookingDate);
        }
        if (description != null) {
            given(unit.getDescription()).willReturn(description);
        }
        return unit;
    }
}
