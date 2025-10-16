package com.bookingsystem.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.bookingsystem.model.User.EMAIL_PATTERN;

@Schema(description = "Data transfer object for creating a new user")
public record UserCreateDto(
        @Schema(
                description = "Unique username for the user",
                example = "john_doe",
                minLength = 3,
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Username is required and cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(
                description = "Email address of the user",
                example = "john.doe@example.com",
                format = "email",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required and cannot be blank")
        @Email(message = "Email must be a valid email address", regexp = EMAIL_PATTERN)
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email
) {}
