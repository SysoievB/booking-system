package com.bookingsystem.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "cancellation")
@PropertySource("classpath:system.properties")
@Validated
@Data
public class CancellationTimeProperties {

    @Min(value = 1L, message = "Value must be positive")
    @Max(value = 30L, message = "Cancellation cannot last`s more than 30 minutes")
    private int minutesValue;
}
