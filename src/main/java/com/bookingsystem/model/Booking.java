package com.bookingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "bookings")
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotEmpty
    @OneToMany(mappedBy = "booking", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    Set<Unit> units;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    User user;

    LocalDateTime createdAt;

    public Booking(Set<Unit> units, User user) {
        this.units = units;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public Booking update(@Nullable Set<Unit> units) {
        this.units = Optional.ofNullable(units).orElse(this.units);
        return this;
    }
}
