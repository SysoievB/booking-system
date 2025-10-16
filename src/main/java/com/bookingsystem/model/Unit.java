package com.bookingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Optional;

@Entity
@Table(name = "units")
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Unit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    int numberOfRooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "accommodation_type")
    AccommodationType type;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    BookingStatus status;

    int floor;

    LocalDate bookingDate;

    @Version
    int version;

    double baseCost;
    double totalCost;

    @Column(columnDefinition = "TEXT")
    String description;

    @Nullable
    @Setter
    @ManyToOne
    @JoinColumn(name = "booking_id", referencedColumnName = "id")
    @JsonIgnore
    Booking booking;

    public Unit(
            int numberOfRooms,
            AccommodationType type,
            int floor,
            double baseCost,
            String description
    ) {
        this.numberOfRooms = numberOfRooms;
        this.type = type;
        this.status = BookingStatus.AVAILABLE;
        this.floor = floor;
        this.baseCost = baseCost;
        this.totalCost = getTotalCost(baseCost);
        this.description = Optional.ofNullable(description).orElse("");
    }

    public Unit update(
            @Nullable Integer numberOfRooms,
            @Nullable AccommodationType type,
            @Nullable BookingStatus status,
            @Nullable Integer floor,
            @Nullable LocalDate bookingDate,
            @Nullable Double baseCost,
            @Nullable String description,
            @Nullable Booking booking
    ) {
        this.numberOfRooms = Optional.ofNullable(numberOfRooms).orElse(this.numberOfRooms);
        this.type = Optional.ofNullable(type).orElse(this.type);
        this.status = Optional.ofNullable(status).orElse(this.status);
        this.floor = Optional.ofNullable(floor).orElse(this.floor);
        this.bookingDate = Optional.ofNullable(bookingDate).orElse(this.bookingDate);
        this.baseCost = Optional.ofNullable(baseCost).orElse(this.baseCost);
        this.totalCost = Optional.ofNullable(baseCost).map(this::getTotalCost).orElse(this.totalCost);
        this.description = Optional.ofNullable(description).orElse(this.description);
        this.booking = Optional.ofNullable(booking).orElse(this.booking);

        return this;
    }

    public double getTotalCost(double cost) {
        //baseCost + 15% system markup
        return cost * 1.15;
    }
}
