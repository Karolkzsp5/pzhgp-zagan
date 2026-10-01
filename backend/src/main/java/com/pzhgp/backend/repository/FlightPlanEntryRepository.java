package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightPlanEntry;
import com.pzhgp.backend.entity.PigeonAgeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightPlanEntryRepository extends JpaRepository<FlightPlanEntry, Long> {

    List<FlightPlanEntry> findAllByFlightPlanIdOrderBySortOrderAsc(Long flightPlanId);

    boolean existsByFlightPlanId(Long flightPlanId);

    boolean existsByFlightPlanIdAndPigeonAgeGroupAndSortOrder(
            Long flightPlanId,
            PigeonAgeGroup pigeonAgeGroup,
            Integer sortOrder
    );

    boolean existsByFlightPlanIdAndPigeonAgeGroupAndSortOrderAndIdNot(
            Long flightPlanId,
            PigeonAgeGroup pigeonAgeGroup,
            Integer sortOrder,
            Long id
    );
}