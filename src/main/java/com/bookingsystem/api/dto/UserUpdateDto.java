package com.bookingsystem.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.bookingsystem.model.User.EMAIL_PATTERN;

@Schema(description = "Data transfer object for updating an existing user")
public record UserUpdateDto(
        @Schema(
                description = "Unique username for the user. If null, remains unchanged",
                example = "john_doe",
                minLength = 3,
                maxLength = 50,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(
                description = "Email address of the user. If null, remains unchanged",
                example = "john.doe@example.com",
                format = "email",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Nullable
        @Email(message = "Email must be a valid email address", regexp = EMAIL_PATTERN)
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email
) {}
