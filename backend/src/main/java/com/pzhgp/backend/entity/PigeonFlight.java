package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lot gołębia wgrany przez hodowcę w postaci pliku GPX wraz z wyliczonymi statystykami.
 * <p>
 * Statystyki są utrwalane razem z lotem, dzięki czemu lista lotów nie wymaga ponownego
 * przeliczania tysięcy punktów trasy przy każdym wyświetleniu. Prędkości zapisywane są
 * w metrach na minutę — w jednostce używanej na listach konkursowych PZHGP.
 */
@Entity
@Table(name = "pigeon_flights")
@Getter
@Setter
@NoArgsConstructor
public class PigeonFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Breeder owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "ring_number", length = 32)
    private String ringNumber;

    @Column(name = "release_site", length = 150)
    private String releaseSite;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "start_latitude", nullable = false)
    private double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private double startLongitude;

    @Column(name = "end_latitude", nullable = false)
    private double endLatitude;

    @Column(name = "end_longitude", nullable = false)
    private double endLongitude;

    /** Odległość w linii prostej z początku na koniec trasy, w metrach. */
    @Column(name = "straight_line_distance_meters", nullable = false)
    private double straightLineDistanceMeters;

    /** Droga pokonana wzdłuż całej zarejestrowanej trasy, w metrach. */
    @Column(name = "track_distance_meters", nullable = false)
    private double trackDistanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    /** Prędkość średnia liczona po trasie, w metrach na minutę. */
    @Column(name = "average_speed_m_per_min", nullable = false)
    private double averageSpeedMetersPerMinute;

    /** Prędkość liczona po linii prostej, w metrach na minutę. */
    @Column(name = "straight_line_speed_m_per_min", nullable = false)
    private double straightLineSpeedMetersPerMinute;

    /** Prędkość maksymalna utrzymana w oknie czasowym, w metrach na minutę. */
    @Column(name = "max_speed_m_per_min", nullable = false)
    private double maxSpeedMetersPerMinute;

    @Column(name = "min_elevation_meters")
    private Double minElevationMeters;

    @Column(name = "max_elevation_meters")
    private Double maxElevationMeters;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    @Column(name = "timestamps_available", nullable = false)
    private boolean timestampsAvailable;

    @Column(name = "total_points", nullable = false)
    private int totalPoints;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pointIndex ASC")
    private List<FlightTrackPoint> trackPoints = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = Instant.now();
        }
    }

    public void addTrackPoint(FlightTrackPoint point) {
        point.setFlight(this);
        this.trackPoints.add(point);
    }
}
