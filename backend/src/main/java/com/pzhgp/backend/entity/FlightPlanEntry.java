package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "flight_plan_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_flight_plan_entry_order",
        columnNames = {"flight_plan_id", "pigeon_age_group", "sort_order"})
})
@Getter
@Setter
@NoArgsConstructor
public class FlightPlanEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_plan_id", nullable = false)
    private FlightPlan flightPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "pigeon_age_group", nullable = false, length = 20)
    private PigeonAgeGroup pigeonAgeGroup;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(name = "distance_km", nullable = false)
    private Integer distanceKm;

    @Column(length = 30)
    private String category;

    @Column(name = "list_type", nullable = false, length = 100)
    private String listType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @OneToMany(mappedBy = "flightPlanEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FlightResult> results = new ArrayList<>();
}