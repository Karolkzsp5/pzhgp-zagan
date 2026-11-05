package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FlightPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlightPlanRepository extends JpaRepository<FlightPlan, Long> {

    List<FlightPlan> findAllByOrderByYearDesc();

    Optional<FlightPlan> findByYear(Integer year);

    boolean existsByYear(Integer year);
}