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
 * przeliczania tysięcy punktów trasy przy każdym wyświetleniu.
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

    @Column(name = "track_start_time")
    private Instant trackStartTime;

    @Column(name = "track_end_time")
    private Instant trackEndTime;

    @Column(name = "release_time")
    private Instant releaseTime;

    @Column(name = "arrival_time")
    private Instant arrivalTime;

    @Column(name = "release_latitude", nullable = false)
    private double releaseLatitude;

    @Column(name = "release_longitude", nullable = false)
    private double releaseLongitude;

    @Column(name = "arrival_latitude", nullable = false)
    private double arrivalLatitude;

    @Column(name = "arrival_longitude", nullable = false)
    private double arrivalLongitude;

    /** Odległość w linii prostej z miejsca wypuszczenia do gołębnika, w metrach. */
    @Column(name = "straight_line_distance_meters", nullable = false)
    private double straightLineDistanceMeters;

    /** Droga faktycznie pokonana wzdłuż trasy w fazie lotu, w metrach. */
    @Column(name = "track_distance_meters", nullable = false)
    private double trackDistanceMeters;

    /** Suma odcinków całego pliku — razem z szumem GPS w spoczynku, w metrach. */
    @Column(name = "raw_track_distance_meters", nullable = false)
    private double rawTrackDistanceMeters;

    @Column(name = "flight_duration_seconds", nullable = false)
    private long flightDurationSeconds;

    @Column(name = "total_duration_seconds", nullable = false)
    private long totalDurationSeconds;

    @Column(name = "average_speed_kmh", nullable = false)
    private double averageSpeedKmh;

    /** Prędkość konkursowa w m/min — jednostka używana w regulaminach lotowych PZHGP. */
    @Column(name = "racing_velocity_m_per_min", nullable = false)
    private double racingVelocityMetersPerMinute;

    @Column(name = "max_speed_kmh", nullable = false)
    private double maxSpeedKmh;

    /** Iloraz drogi po trasie i linii prostej: 1,0 oznacza lot idealnie prosty. */
    @Column(name = "straightness_ratio", nullable = false)
    private double straightnessRatio;

    @Column(name = "course_degrees", nullable = false)
    private double courseDegrees;

    @Column(name = "min_elevation_meters")
    private Double minElevationMeters;

    @Column(name = "max_elevation_meters")
    private Double maxElevationMeters;

    @Column(name = "elevation_gain_meters")
    private Double elevationGainMeters;

    /** Droga "przebyta" wyłącznie przez dryf GPS przed startem i po przylocie, w metrach. */
    @Column(name = "stationary_noise_meters", nullable = false)
    private double stationaryNoiseMeters;

    @Column(name = "pre_flight_duration_seconds", nullable = false)
    private long preFlightDurationSeconds;

    @Column(name = "post_flight_duration_seconds", nullable = false)
    private long postFlightDurationSeconds;

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
