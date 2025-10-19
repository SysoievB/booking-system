package com.bookingsystem.exceptions;

public class UserNotFoundException extends BookingSystemEntityNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
