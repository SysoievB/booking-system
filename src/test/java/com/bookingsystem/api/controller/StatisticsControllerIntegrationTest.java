package com.bookingsystem.api.controller;

import com.bookingsystem.api.dto.UnitCreateDto;
import com.bookingsystem.configuration.TestcontainersConfiguration;
import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.BookingStatus;
import com.bookingsystem.repository.UnitRepository;
import com.bookingsystem.service.UnitService;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class StatisticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnitService unitService;

    @Autowired
    private UnitRepository unitRepository;

    @BeforeEach
    void setUp() {
        unitRepository.deleteAll();
    }

    @Test
    void should_return_zero_when_no_units_exist() throws Exception {
        //when & then
        mockMvc.perform(get("/api/units/statistics/count/available"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void should_return_correct_count_of_available_units() throws Exception {
        //given
        createAvailableUnit("Spacious 2-bedroom apartment with ocean view");
        createAvailableUnit("Cozy studio apartment in downtown area");
        createAvailableUnit("Luxury penthouse with panoramic city views");

        //when & then
        mockMvc.perform(get("/api/units/statistics/count/available"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void should_count_only_available_units() throws Exception {
        //given
        createAvailableUnit("Available apartment with great amenities");
        createAvailableUnit("Another available unit with pool access");

        val reservedUnit = createUnitDto("Reserved unit with garden view", AccommodationType.APARTMENT);
        val savedReserved = unitService.createUnit(reservedUnit);
        savedReserved.setStatus(BookingStatus.RESERVED);
        unitRepository.save(savedReserved);

        val bookedUnit = createUnitDto("Booked luxury suite with spa", AccommodationType.HOME);
        val savedBooked = unitService.createUnit(bookedUnit);
        savedBooked.setStatus(BookingStatus.BOOKED);
        unitRepository.save(savedBooked);

        createAvailableUnit("Third available unit near beach");

        //when & then
        mockMvc.perform(get("/api/units/statistics/count/available"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    private void createAvailableUnit(String description) {
        val createDto = new UnitCreateDto(
                2,
                AccommodationType.APARTMENT,
                3,
                150.00,
                LocalDate.now(),
                description
        );
        unitService.createUnit(createDto);
    }

    private UnitCreateDto createUnitDto(String description, AccommodationType type) {
        return new UnitCreateDto(
                2,
                type,
                3,
                150.00,
                LocalDate.now(),
                description
        );
    }
}