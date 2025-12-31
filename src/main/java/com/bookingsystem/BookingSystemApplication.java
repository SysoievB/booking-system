package com.bookingsystem;

import com.bookingsystem.api.dto.UnitCreateDto;
import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDate;
import java.util.Random;
import java.util.stream.IntStream;

@EnableRetry
@EnableScheduling
@RequiredArgsConstructor
@SpringBootApplication
public class BookingSystemApplication implements CommandLineRunner {
    private final UnitService unitService;

    public static void main(String[] args) {
        SpringApplication.run(BookingSystemApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        val random = new Random();

        IntStream.rangeClosed(1, 90)
                .forEach(index -> {
                    val type = AccommodationType.values()[random.nextInt(AccommodationType.values().length)];
                    val dto = new UnitCreateDto(
                            random.nextInt(1, 5),
                            type,
                            random.nextInt(1, 11),
                            50 + random.nextDouble(450),
                            LocalDate.now().minusYears(1).plusDays(index),
                            "Unit " + index + " - " + type + " with random features"
                    );

                    unitService.createUnit(dto);
                });
    }
}