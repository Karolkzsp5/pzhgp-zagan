package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.FoundPigeonReport;
import com.pzhgp.backend.entity.FoundPigeonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FoundPigeonRepository extends JpaRepository<FoundPigeonReport, Long> {

    /**
     * Zgłoszenia filtrowane po statusie i fragmencie numeru obrączki.
     * <p>
     * Filtrowanie odbywa się w zapytaniu, a nie po pobraniu rekordów do pamięci, dzięki czemu
     * stronicowanie działa na poziomie bazy danych.
     * <p>
     * Status jest opcjonalny — {@code null} wyłącza ten warunek. Fragment numeru obrączki musi
     * być tekstem; pusty tekst dopasowuje wszystkie zgłoszenia. Parametr celowo nie bywa
     * {@code null}, ponieważ PostgreSQL nie potrafi wtedy ustalić jego typu w wyrażeniu LIKE
     * i odrzuca zapytanie błędem "operator does not exist: character varying ~~ bytea".
     */
    @Query("""
            SELECT r FROM FoundPigeonReport r
            WHERE (:status IS NULL OR r.status = :status)
              AND r.ringNumberNormalized LIKE CONCAT('%', :ringNumber, '%')
            """)
    Page<FoundPigeonReport> search(@Param("status") FoundPigeonStatus status,
                                   @Param("ringNumber") String ringNumber,
                                   Pageable pageable);

    long countByStatus(FoundPigeonStatus status);
}
