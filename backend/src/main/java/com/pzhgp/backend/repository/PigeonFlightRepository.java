package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.PigeonFlight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PigeonFlightRepository extends JpaRepository<PigeonFlight, Long> {

    /** Loty wgrane przez użytkownika, od najstarszego — do eksportu jego danych (bez punktów trasy). */
    List<PigeonFlight> findByOwnerIdOrderByUploadedAtAscIdAsc(Long ownerId);

    @EntityGraph(attributePaths = {"owner"})
    Page<PigeonFlight> findByOwnerOrderByUploadedAtDescIdDesc(Breeder owner, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    Optional<PigeonFlight> findWithOwnerById(Long id);

    long countByOwner(Breeder owner);
}
