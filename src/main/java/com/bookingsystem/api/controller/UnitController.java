package com.bookingsystem.api.controller;

import com.bookingsystem.api.dto.UnitCreateDto;
import com.bookingsystem.api.dto.UnitUpdateDto;
import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.Unit;
import com.bookingsystem.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/units")
@RequiredArgsConstructor
@Tag(name = "Units", description = "Unit management endpoints")
public class UnitController {

    private final UnitService unitService;

    @PostMapping
    @Operation(
            summary = "Create a new unit",
            description = "Create a new accommodation unit with specified properties. " +
                    "Final cost is automatically calculated as base cost + 15%.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "UnitCreateDto request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UnitCreateDto.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Unit created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Unit> createUnit(@Valid @RequestBody UnitCreateDto dto) {
        val unit = unitService.createUnit(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(unit);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update unit",
            description = "Update unit information. Only non-null fields will be updated.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "UnitUpdateDto request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = UnitUpdateDto.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unit updated successfully"),
            @ApiResponse(responseCode = "404", description = "Unit not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Unit> updateUnit(
            @Parameter(description = "Unit ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UnitUpdateDto dto
    ) {
        val unit = unitService.updateUnit(id, dto);
        return ResponseEntity.ok(unit);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get unit by ID",
            description = "Retrieve a unit by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unit found"),
            @ApiResponse(responseCode = "404", description = "Unit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Unit> getUnitById(
            @Parameter(description = "Unit ID", required = true)
            @PathVariable Long id
    ) {
        val unit = unitService.getUnitById(id);
        return ResponseEntity.ok(unit);
    }

    @GetMapping("/all")
    @Operation(
            summary = "Get all units (no pagination)",
            description = "Retrieve a complete list of all units without pagination"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Units retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Unit>> getAllUnitsList() {
        val units = unitService.getAllUnits();
        return ResponseEntity.ok(units);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search units by criteria with pagination",
            description = "Search for units based on number of rooms, accommodation type, and cost range. " +
                    "All parameters are optional - omit to get all units."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<Unit>> searchUnits(
            @Parameter(description = "Number of rooms (exact match)")
            @RequestParam(required = false) Integer numberOfRooms,

            @Parameter(description = "Accommodation type (HOME, FLAT, APARTMENTS)")
            @RequestParam(required = false) AccommodationType type,

            @Parameter(description = "Minimum cost (inclusive)")
            @RequestParam(required = false) Double minCost,

            @Parameter(description = "Maximum cost (inclusive)")
            @RequestParam(required = false) Double maxCost,

            @Parameter(description = "From date (inclusive)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate from,

            @Parameter(description = "To date (inclusive)")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate to,

            @Parameter(description = "Page number")
            @RequestParam(required = false) @Nullable Pageable pageable
    ) {
        val units = unitService.searchUnits(numberOfRooms, type, minCost, maxCost, from, to, pageable);
        return ResponseEntity.ok(units);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete unit",
            description = "Delete a unit by its ID. Cannot delete units with active bookings."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Unit deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Unit not found"),
            @ApiResponse(responseCode = "409", description = "Unit has active bookings"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteUnit(
            @Parameter(description = "Unit ID", required = true)
            @PathVariable Long id
    ) {
        unitService.deleteUnit(id);
        return ResponseEntity.noContent().build();
    }
}