package com.bookingsystem.api.dto;

import com.bookingsystem.model.AccommodationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "DTO for creating a new accommodation unit")
public record UnitCreateDto(
        @Schema(
                description = "Number of rooms in the unit",
                example = "2",
                minimum = "1",
                maximum = "10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1, message = "Number of rooms must be at least 1")
        @Max(value = 10, message = "Number of rooms cannot exceed 10")
        int numberOfRooms,

        @Schema(
                description = "Type of accommodation",
                example = "APARTMENT",
                allowableValues = {"APARTMENT", "HOTEL_ROOM", "STUDIO", "VILLA", "PENTHOUSE"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Accommodation type is required")
        AccommodationType type,

        @Schema(
                description = "Floor number where the unit is located",
                example = "3",
                minimum = "0",
                maximum = "50",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 0, message = "Floor must be at least 0")
        @Max(value = 50, message = "Floor cannot exceed 50")
        int floor,

        @Schema(
                description = "Base cost per night for the unit in USD",
                example = "150.00",
                minimum = "0.01",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @DecimalMin(value = "0.01", message = "Base cost must be greater than 0")
        double baseCost,

        @Schema(
                description = "Detailed description of the unit, including amenities and features",
                example = "Spacious 2-bedroom apartment with ocean view, fully equipped kitchen, and balcony",
                minLength = 10,
                maxLength = 1000,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Description is required")
        @NotBlank(message = "Description cannot be blank")
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        String description
) {
}