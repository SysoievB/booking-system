package com.bookingsystem.exceptions;

public class PaymentNotFoundException extends  BookingSystemEntityNotFoundException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}
