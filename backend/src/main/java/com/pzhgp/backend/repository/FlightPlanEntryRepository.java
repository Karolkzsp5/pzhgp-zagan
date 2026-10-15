package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightPlanEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightPlanEntryRepository extends JpaRepository<FlightPlanEntry, Long> {
    List<FlightPlanEntry> findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(Long flightPlanId);
    boolean existsByFlightPlanId(Long flightPlanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM FlightPlanEntry e WHERE e.id = :id")
    Optional<FlightPlanEntry> findByIdForUpdate(@Param("id") Long id);
}