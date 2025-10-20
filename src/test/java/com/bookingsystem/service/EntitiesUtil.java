package com.bookingsystem.service;

import com.bookingsystem.model.*;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

@UtilityClass
class EntitiesUtil {

    @Builder(builderMethodName = "user")
    User getUser(@Nullable Long id, @Nullable String username, @Nullable String email) {
        val user = mock(User.class, withSettings().strictness(Strictness.LENIENT));
        if (id != null) {
            given(user.getId()).willReturn(id);
        }
        if (username != null) {
            given(user.getUsername()).willReturn(username);
        }
        if (email != null) {
            given(user.getEmail()).willReturn(email);
        }
        return user;
    }

    @Builder(builderMethodName = "unit")
    Unit getUnit(
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

    @Builder(builderMethodName = "booking")
    Booking getBooking(
            @Nullable Long id,
            @Nullable User user,
            @Nullable LocalDateTime createdAt,
            @Nullable Set<Unit> units
    ) {
        val booking = mock(Booking.class, withSettings().strictness(Strictness.LENIENT));
        if (id != null) {
            given(booking.getId()).willReturn(id);
        }
        if (user != null) {
            given(booking.getUser()).willReturn(user);
        }
        if (createdAt != null) {
            given(booking.getCreatedAt()).willReturn(createdAt);
        }
        if (units != null) {
            given(booking.getUnits()).willReturn(units);
        }
        return booking;
    }

    @Builder(builderMethodName = "payment")
    Payment getPayment(
            @Nullable Long id,
            @Nullable Booking booking,
            @Nullable Boolean paid,
            @Nullable Boolean expired,
            @Nullable Double amount,
            @Nullable PaymentStatus status,
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
        if (status != null) {
            given(payment.getStatus()).willReturn(status);
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
