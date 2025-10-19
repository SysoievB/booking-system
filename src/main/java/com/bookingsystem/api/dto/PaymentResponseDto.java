package com.bookingsystem.api.dto;

import com.bookingsystem.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response DTO for payment information")
public record PaymentResponseDto(
        @Schema(description = "Payment ID", example = "1")
        Long id,

        @Schema(description = "Associated booking ID", example = "5")
        Long bookingId,

        @Schema(description = "Payment status", example = "COMPLETED")
        PaymentStatus status,

        @Schema(description = "Payment deadline", example = "2025-10-18T08:15:00")
        LocalDateTime paymentDeadline,

        @Schema(description = "Timestamp when payment was completed", example = "2025-10-18T08:05:30")
        LocalDateTime paidAt,

        @Schema(description = "Total payment amount", example = "450.00")
        double paymentAmount,

        @Schema(description = "Whether payment is completed", example = "true")
        boolean paid,

        @Schema(description = "Number of booked days", example = "3")
        int amountOfBookedDays,

        @Schema(description = "Payment creation timestamp", example = "2025-10-18T08:00:00")
        LocalDateTime paymentTimestamp,

        @Schema(description = "Whether payment is expired", example = "false")
        boolean expired
) {
}
