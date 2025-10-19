package com.bookingsystem.exceptions;

public class BookingNotFoundException extends BookingSystemEntityNotFoundException {
    public BookingNotFoundException(String message) {
        super(message);
    }
}
