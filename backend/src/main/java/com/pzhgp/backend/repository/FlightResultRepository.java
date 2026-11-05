package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightResult;
import com.pzhgp.backend.entity.FlightResultScope;
import com.pzhgp.backend.repository.projection.FlightResultSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightResultRepository extends JpaRepository<FlightResult, Long> {

    @Query("""
            SELECT
                r.id AS id,
                r.flightPlanEntry.id AS flightPlanEntryId,
                r.scope AS scope,
                s.id AS sectionId,
                s.name AS sectionName,
                s.sortOrder AS sectionSortOrder,
                r.originalFileName AS originalFileName
            FROM FlightResult r
            LEFT JOIN r.section s
            WHERE r.flightPlanEntry.flightPlan.id = :flightPlanId
            """)
    List<FlightResultSummaryProjection> findSummariesByFlightPlanId(
            @Param("flightPlanId") Long flightPlanId
    );

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