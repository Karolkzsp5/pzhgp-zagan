package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity // mówi springowi że ta klasa to tabela w bazie danych
@Table(name = "breeders") //nazwa tabeli w postgresql
@Getter
@Setter
@NoArgsConstructor
public class Breeder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, length = 64)
    private String surname;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 9)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "section_id", nullable = false)
    private Integer sectionId;

    @Column(nullable = false, length = 20)
    private String role; // BREEDER, MODERATOR, ADMINISTRATOR

    @Column(name = "postal_code", length = 6)
    private String postalCode;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String street;

    @Column(name = "house_number", length = 10)
    private String houseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = AccountStatus.PENDING;
        this.role = "BREEDER";
    }
}