package com.bookingsystem.repository;

import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    @Query(value = """
                SELECT u FROM Unit u
                WHERE u.numberOfRooms = :rooms
                  AND u.type = :type
                  AND u.totalCost >= :minCost
                  AND u.totalCost <= :maxCost
                  AND u.bookingDate BETWEEN :from AND :to
                  AND u.status = 'AVAILABLE'
            """)
    Page<Unit> searchUnits(
            @Param("rooms") Integer numberOfRooms,
            @Param("type") AccommodationType type,
            @Param("minCost") Double minCost,
            @Param("maxCost") Double maxCost,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.type = 'AVAILABLE'")
    long countAvailableUnits();
}


