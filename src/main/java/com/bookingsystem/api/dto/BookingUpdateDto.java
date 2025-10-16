package com.bookingsystem.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(description = "DTO for updating an existing booking")
public record BookingUpdateDto(
        @Schema(
                description = "ID of the payment associated with this booking",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Payment ID is required")
        Long paymentId,

        @Schema(
                description = "Set of unit IDs to be included in this booking. If null, units remain unchanged",
                example = "[1, 2, 3]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        Set<Long> unitIds,

        @Schema(
                description = "ID of the user making the booking. If null, user remains unchanged",
                example = "5",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        Long userId
) {
}