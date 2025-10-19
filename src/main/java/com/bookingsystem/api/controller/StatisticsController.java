package com.bookingsystem.api.controller;

import com.bookingsystem.service.UnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/units/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final UnitService statisticsService;

    @GetMapping("/count/available")
    @Operation(
            summary = "Get count of available units",
            description = "Get the total number of units available for booking (cached for performance)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Long> getAvailableUnitsCount() {
        val count = statisticsService.getAvailableUnitsCount();
        return ResponseEntity.ok(count);
    }
}
