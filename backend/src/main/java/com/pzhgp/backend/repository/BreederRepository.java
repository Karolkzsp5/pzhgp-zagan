package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreederRepository extends JpaRepository<Breeder, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Breeder> findByEmail(String email);
    Optional<Breeder> findByEmailIgnoreCase(String email);

    /**
     * Konto o podanym adresie e-mail — zwykle zalogowanego użytkownika.
     *
     * @throws EntityNotFoundException gdy konto nie istnieje
     */
    default Breeder requireByEmail(String email) {
        return findByEmail(email).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));
    }

    @EntityGraph(attributePaths = {"section"})
    List<Breeder> findByStatus(AccountStatus status);

    @EntityGraph(attributePaths = {"section"})
    List<Breeder> findByStatusIn(List<AccountStatus> statuses);

    /** Konta o zadanej roli i statusie — wykorzystywane przy powiadamianiu administratorów. */
    List<Breeder> findByRoleAndStatus(Role role, AccountStatus status);
}