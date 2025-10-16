package com.bookingsystem.api.dto;

import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.Booking;
import com.bookingsystem.model.BookingStatus;
import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@Schema(description = "DTO for updating an existing accommodation unit")
public record UnitUpdateDto(
        @Schema(
                description = "Number of rooms in the unit. If null, remains unchanged",
                example = "2",
                minimum = "1",
                maximum = "10",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @Min(value = 1, message = "Number of rooms must be at least 1")
        @Max(value = 10, message = "Number of rooms cannot exceed 10")
        Integer numberOfRooms,

        @Schema(
                description = "Type of accommodation. If null, remains unchanged",
                example = "APARTMENT",
                allowableValues = {"APARTMENT", "HOTEL_ROOM", "STUDIO", "VILLA", "PENTHOUSE"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        AccommodationType type,

        @Schema(
                description = "Current booking status of the unit. If null, remains unchanged",
                example = "AVAILABLE",
                allowableValues = {"AVAILABLE", "BOOKED", "MAINTENANCE", "UNAVAILABLE"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        BookingStatus status,

        @Schema(
                description = "Floor number where the unit is located. If null, remains unchanged",
                example = "3",
                minimum = "0",
                maximum = "50",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @Min(value = 0, message = "Floor must be at least 0")
        @Max(value = 50, message = "Floor cannot exceed 50")
        Integer floor,

        @Schema(
                description = "Date when the unit is booked. If null, remains unchanged",
                example = "2025-12-25",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @FutureOrPresent(message = "Booking date must be in the present or future")
        LocalDate bookingDate,

        @Schema(
                description = "Base cost per night for the unit in USD. If null, remains unchanged",
                example = "150.00",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @DecimalMin(value = "0.01", message = "Base cost must be greater than 0")
        Double baseCost,

        @Schema(
                description = "Detailed description of the unit. If null, remains unchanged",
                example = "Spacious 2-bedroom apartment with ocean view",
                minLength = 10,
                maxLength = 1000,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        String description,

        @Schema(
                description = "Associated booking object. If null, remains unchanged",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        Booking booking
) {
}