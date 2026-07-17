package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity // mówi springowi że ta klasa to tabela w bazie danych
@Table(name = "breeders") //nazwa tabeli w postgresql
@Getter
@Setter
@NoArgsConstructor
public class Breeder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //Auto increment
    private long id;

    @Column(nullable = false, length = 32)
    private String name;

    @Column(nullable = false, length = 64)
    private String surname;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void OnCreate(){
        this.createdAt = LocalDateTime.now();
        this.status = AccountStatus.PENDING;
    }
}
