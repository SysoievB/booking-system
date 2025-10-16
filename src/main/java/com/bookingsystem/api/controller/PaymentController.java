package com.bookingsystem.api.controller;

import com.bookingsystem.model.Payment;
import com.bookingsystem.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/process")
    @Operation(
            summary = "Process payment for a booking (emulation)",
            description = "Emulate payment processing for a booking. " +
                    "This must be called within 15 minutes of booking creation. " +
                    "After successful payment: " +
                    "- Payment status changes to COMPLETED " +
                    "- Units status changes from RESERVED to BOOKED " +
                    "- Booking is confirmed and cannot be cancelled"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "400", description = "Payment already completed or expired"),
            @ApiResponse(responseCode = "403", description = "Only booking owner can process payment"),
            @ApiResponse(responseCode = "404", description = "Booking or payment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Payment> processPayment(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long bookingId,

            @Parameter(description = "User ID making the payment", required = true)
            @RequestParam Long userId
    ) {
        val payment = paymentService.processPayment(bookingId, userId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get payment by ID",
            description = "Retrieve detailed information about a specific payment"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Payment> getPaymentById(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long id
    ) {
        val payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(
            summary = "Get payment by booking ID",
            description = "Retrieve payment information for a specific booking"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found for this booking"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Payment> getPaymentByBookingId(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long bookingId
    ) {
        val payment = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(payment);
    }

    @GetMapping
    @Operation(
            summary = "Get all payments",
            description = "Retrieve a list of all payments in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payments retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Payment>> getAllPayments() {
        val payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete payment",
            description = "Endpoint to delete a payment record. Use with caution."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deletePayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable Long id
    ) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}