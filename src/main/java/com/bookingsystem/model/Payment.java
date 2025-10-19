package com.bookingsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "payments")
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", referencedColumnName = "id")
    Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    PaymentStatus status;

    @Column(name = "payment_deadline", nullable = false)
    LocalDateTime paymentDeadline;

    LocalDateTime paidAt;

    double paymentAmount;

    boolean paid;

    int amountOfBookedDays;

    LocalDateTime paymentTimestamp;

    public Payment(@NotNull Booking booking, LocalDateTime paymentDeadline) {
        this.booking = booking;
        this.status = PaymentStatus.PENDING;
        this.paid = false;
        this.paymentAmount = getTotalAmount(booking.getUnits());
        this.amountOfBookedDays = getAmountOfBookedDays(booking.getUnits());
        this.paymentTimestamp = LocalDateTime.now();
        this.paymentDeadline = paymentDeadline;
    }

    public void markAsPaid() {
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
        this.paid = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(paymentDeadline) && (status == PaymentStatus.PENDING );
    }

    private double getTotalAmount(Set<Unit> units) {
        return units.stream()
                .map(Unit::getTotalCost)
                .reduce(0.0, Double::sum);
    }

    private int getAmountOfBookedDays(Set<Unit> units) {
        return units.size();
    }
}
