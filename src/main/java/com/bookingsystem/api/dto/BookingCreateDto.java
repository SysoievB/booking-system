package com.bookingsystem.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(description = "DTO for creating a new booking")
public record BookingCreateDto(
        @Schema(
                description = "ID of the payment associated with this booking",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Payment ID is required")
        Long paymentId,

        @Schema(
                description = "Set of unit IDs to be included in this booking",
                example = "[1, 2, 3]",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1
        )
        @NotEmpty(message = "At least one unit ID is required")
        Set<Long> unitIds,

        @Schema(
                description = "ID of the user making the booking",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "User ID is required")
        Long userId
) {
}