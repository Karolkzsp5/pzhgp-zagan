package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightTrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightTrackPointRepository extends JpaRepository<FlightTrackPoint, Long> {

    /**
     * Punkty trasy pobierane osobnym zapytaniem zamiast przez {@code JOIN FETCH}, aby
     * uniknąć iloczynu kartezjańskiego przy kilku tysiącach punktów na lot.
     */
    List<FlightTrackPoint> findByFlightIdOrderByPointIndexAsc(Long flightId);
}
