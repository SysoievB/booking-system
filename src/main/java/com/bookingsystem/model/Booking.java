package com.bookingsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    Set<Unit> units;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    User user;

    public Booking(Set<Unit> units, User user){
        this.units = units;
        this.user = user;
    }

    public Booking update(@Nullable Set<Unit> units, @Nullable User user){
        this.units = Optional.ofNullable(units).orElse(this.units);
        this.user = Optional.ofNullable(user).orElse(this.user);
        return this;
    }
}
