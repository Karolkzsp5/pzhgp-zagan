package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.AccountStatus;
import com.pzhgp.backend.entity.Breeder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreederRepository extends JpaRepository<Breeder, Long> {

    Optional<Breeder> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);

    List<Breeder> findByStatus(AccountStatus status);
}
