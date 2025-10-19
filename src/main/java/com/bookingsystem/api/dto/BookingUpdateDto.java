package com.bookingsystem.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.util.Set;

@Schema(description = "DTO for updating an existing booking")
public record BookingUpdateDto(

        @Schema(
                description = "Set of unit IDs to be included in this booking. If null, units remain unchanged",
                example = "[1, 2, 3]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        Set<Long> unitIds
) {
}