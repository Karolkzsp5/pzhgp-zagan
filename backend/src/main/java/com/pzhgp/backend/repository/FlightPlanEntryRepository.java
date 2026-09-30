package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightPlanEntry;
import com.pzhgp.backend.entity.PigeonAgeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightPlanEntryRepository extends JpaRepository<FlightPlanEntry, Long> {

    List<FlightPlanEntry> findAllByFlightPlanIdAndPigeonAgeGroupOrderBySortOrderAsc(
            Long flightPlanId,
            PigeonAgeGroup pigeonAgeGroup
    );
}