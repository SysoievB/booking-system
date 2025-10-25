package com.bookingsystem.repository;

import com.bookingsystem.model.AccommodationType;
import com.bookingsystem.model.Unit;
import com.bookingsystem.model.Unit_;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

import static com.bookingsystem.model.BookingStatus.AVAILABLE;
import static org.springframework.data.jpa.domain.Specification.where;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long>, JpaSpecificationExecutor<Unit> {

    @SuppressWarnings("deprecations")
    default Page<Unit> searchUnits(
            @Nullable @Param("rooms") Integer numberOfRooms,
            @Nullable @Param("type") AccommodationType type,
            @Nullable @Param("minCost") Double minCost,
            @Nullable @Param("maxCost") Double maxCost,
            @Nullable @Param("from") LocalDate from,
            @Nullable @Param("to") LocalDate to,
            @Nullable Pageable pageable
    ) {
        Specification<Unit> spec = where(hasRooms(numberOfRooms))
                .and(hasType(type))
                .and(costBetween(minCost, maxCost))
                .and(bookingDateBetweenOrIsNull(from, to))
                .and(isAvailable());

        return findAll(spec, Optional.ofNullable(pageable).orElseGet(Pageable::unpaged));
    }

    @Query("SELECT COUNT(distinct u) FROM Unit u WHERE u.status = 'AVAILABLE'")
    long countAvailableUnits();

    private static Specification<Unit> hasRooms(@Nullable Integer rooms) {
        return (root, query, cb) -> rooms == null
                ? null
                : cb.equal(root.get(Unit_.numberOfRooms), rooms);
    }

    private static Specification<Unit> hasType(@Nullable AccommodationType type) {
        return (root, query, cb) -> type == null
                ? null
                : cb.equal(root.get(Unit_.type), type);
    }

    @Nullable
    private static Specification<Unit> costBetween(@Nullable Double minCost, @Nullable Double maxCost) {
        if (minCost == null && maxCost == null) {
            return null;
        } else if (minCost != null && maxCost == null) {
            return (root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(Unit_.totalCost), minCost);
        } else if (minCost == null) {
            return (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(Unit_.totalCost), maxCost);
        } else {
            return (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get(Unit_.totalCost), minCost),
                    cb.lessThanOrEqualTo(root.get(Unit_.totalCost), maxCost)
            );
        }
    }

    @Nullable
    private static Specification<Unit> bookingDateBetweenOrIsNull(@Nullable LocalDate from, @Nullable LocalDate to) {
        if (from == null && to == null) {
            return null;
        } else if (from != null && to == null) {
            return (root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(Unit_.bookingDate), from);
        } else if (from == null) {
            return (root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(Unit_.bookingDate), to);
        } else {
            return (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get(Unit_.bookingDate), from),
                    cb.lessThanOrEqualTo(root.get(Unit_.bookingDate), to)
            );
        }
    }

    private static Specification<Unit> isAvailable() {
        return (root, query, cb) -> cb.equal(root.get(Unit_.status), AVAILABLE);
    }
}
