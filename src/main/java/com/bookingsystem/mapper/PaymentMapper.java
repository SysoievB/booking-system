package com.bookingsystem.mapper;

import com.bookingsystem.api.dto.PaymentResponseDto;
import com.bookingsystem.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface PaymentMapper {

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(target = "expired", expression = "java(payment.isExpired())")
    PaymentResponseDto toDto(Payment payment);
}
