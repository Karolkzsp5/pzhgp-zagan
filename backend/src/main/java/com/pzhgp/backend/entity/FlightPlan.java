package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flight_plans", uniqueConstraints = {@UniqueConstraint(name = "uk_flight_plans_year", columnNames = "plan_year")})
@Getter
@Setter
@NoArgsConstructor
public class FlightPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_year", nullable = false)
    private Integer year;

    @Column(name = "adult_notes", columnDefinition = "TEXT")
    private String adultNotes;

    @Column(name = "young_notes", columnDefinition = "TEXT")
    private String youngNotes;

    @OneToMany(mappedBy = "flightPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightPlanEntry> entries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}