package com.bookingsystem.service;

import com.bookingsystem.api.dto.UnitCreateDto;
import com.bookingsystem.api.dto.UnitUpdateDto;
import com.bookingsystem.exceptions.UnitNotFoundException;
import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.BookingStatus;
import com.bookingsystem.model.Unit;
import com.bookingsystem.repository.UnitRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.bookingsystem.configuration.RedisConfig.UNIT_COUNT_CACHE;
import static com.bookingsystem.model.EntityType.UNIT;
import static com.bookingsystem.model.EventOperation.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitService {
    private final UnitRepository unitRepository;
    private final EventService eventService;

    @Transactional
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public Unit createUnit(UnitCreateDto dto) {
        val newUnit = new Unit(
                dto.numberOfRooms(),
                dto.type(),
                dto.floor(),
                dto.baseCost(),
                dto.bookingDate(),
                dto.description()
        );

        val saved = unitRepository.save(newUnit);

        eventService.createEvent(
                UNIT,
                CREATE,
                saved.getId(),
                String.format("Unit created: %s", saved.getId())
        );
        return saved;
    }

    @Transactional
    public Unit updateUnit(Long unitId, UnitUpdateDto dto) {
        return unitRepository.findById(unitId)
                .map(unit -> unit.update(
                                dto.numberOfRooms(),
                                dto.type(),
                                dto.status(),
                                dto.floor(),
                                dto.bookingDate(),
                                dto.baseCost(),
                                dto.description()
                                //booking
                        )
                )
                .map(unitRepository::save)
                .map(unit -> {
                    eventService.createEvent(
                            UNIT,
                            UPDATE,
                            unit.getId(),
                            String.format("Unit updated: %s", unit.getId())
                    );
                    return unit;
                })
                .orElseThrow(() -> new UnitNotFoundException("Unit not found with id: " + unitId));
    }

    public Unit getUnitById(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new UnitNotFoundException("Unit not found with id: " + id));
    }

    public List<Unit> getAllUnits() {
        return unitRepository.findAll();
    }

    public Page<Unit> searchUnits(
            @Nullable Integer numberOfRooms,
            @Nullable AccommodationType type,
            @Nullable Double minCost,
            @Nullable Double maxCost,
            @Nullable LocalDate from,
            @Nullable LocalDate to,
            @Nullable Pageable pageable
    ) {
        return unitRepository.searchUnits(numberOfRooms, type, minCost, maxCost, from, to, Optional.ofNullable(pageable).orElse(Pageable.unpaged()));
    }

    public Set<Unit> findAllById(Set<Long> ids) {
        return new HashSet<>(unitRepository.findAllById(ids));
    }

    @Transactional
    @CacheEvict(value = UNIT_COUNT_CACHE, key = "'count'")
    public void deleteUnit(Long unitId) {
        if (!unitRepository.existsById(unitId)) {
            throw new UnitNotFoundException("Unit not found with id: " + unitId);
        }
        unitRepository.deleteById(unitId);
        eventService.createEvent(
                UNIT,
                DELETE,
                unitId,
                String.format("Unit deleted: %s", unitId)
        );
    }

    @Transactional
    public void setUnitsBookingStatus(Set<Unit> units, BookingStatus bookingStatus) {
        units.forEach(unit -> {
            unit.setStatus(bookingStatus);
            unitRepository.save(unit);
            eventService.createEvent(
                    UNIT,
                    UPDATE,
                    unit.getId(),
                    String.format("Unit updated: %s", unit.getId())
            );
        });
    }

    @Cacheable(value = UNIT_COUNT_CACHE, key = "'count'")
    public long getAvailableUnitsCount() {
        return unitRepository.countAvailableUnits();
    }
}
