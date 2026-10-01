package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightResult;
import com.pzhgp.backend.entity.FlightResultScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightResultRepository extends JpaRepository<FlightResult, Long> {

    boolean existsByFlightPlanEntryId(Long flightPlanEntryId);

    boolean existsByFlightPlanEntryIdAndScope(
            Long flightPlanEntryId,
            FlightResultScope scope
    );

    boolean existsByFlightPlanEntryIdAndScopeAndSectionId(
            Long flightPlanEntryId,
            FlightResultScope scope,
            Long sectionId
    );
}