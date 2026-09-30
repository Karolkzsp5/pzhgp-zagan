package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightResult;
import com.pzhgp.backend.entity.FlightResultScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightResultRepository extends JpaRepository<FlightResult, Long> {

    List<FlightResult> findAllByFlightPlanEntryIdOrderByUploadedAtAsc(Long flightPlanEntryId);

    Optional<FlightResult> findByFlightPlanEntryIdAndScopeAndSectionId(
            Long flightPlanEntryId,
            FlightResultScope scope,
            Long sectionId
    );

    boolean existsByFlightPlanEntryId(Long flightPlanEntryId);
}