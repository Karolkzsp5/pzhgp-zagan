package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreederRepository extends JpaRepository<Breeder, Long> {
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Breeder> findByEmail(String email);
    List<Breeder> findByStatus(AccountStatus status);
    List<Breeder> findByStatusIn(List<AccountStatus> statuses);
}
