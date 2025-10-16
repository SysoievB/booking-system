package com.bookingsystem.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class User {
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9.-]+@[A-Za-z0-9.-]+\\.com$";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank
    String username;

    @NotBlank
    @Email(regexp = EMAIL_PATTERN)
    String email;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<Booking> bookings;

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.bookings = Collections.emptyList();
    }

    public User update(
            @Nullable String username,
            @Nullable String email
    ) {
        this.username = Optional.ofNullable(username).orElse(this.username);
        this.email = Optional.ofNullable(email).orElse(this.email);
        return this;
    }
}
