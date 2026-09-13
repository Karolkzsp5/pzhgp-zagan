package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Pojedynczy punkt trasy lotu odczytany z pliku GPX.
 */
@Entity
@Table(
        name = "flight_track_points",
        indexes = @Index(name = "idx_track_point_flight", columnList = "flight_id, point_index")
)
@Getter
@Setter
@NoArgsConstructor
public class FlightTrackPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flight_id", nullable = false)
    private PigeonFlight flight;

    /** Kolejność punktu w trasie — zachowuje porządek odczytu z pliku. */
    @Column(name = "point_index", nullable = false)
    private int pointIndex;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "elevation_meters")
    private Double elevationMeters;

    /** Znacznik czasu w UTC — konwersja na czas lokalny odbywa się w przeglądarce. */
    @Column(name = "recorded_at")
    private Instant recordedAt;

    /** Prędkość chwilowa wygładzona oknem czasowym, w m/min — służy do kolorowania trasy na mapie. */
    @Column(name = "speed_m_per_min")
    private Double speedMetersPerMinute;

    public FlightTrackPoint(int pointIndex, double latitude, double longitude,
                            Double elevationMeters, Instant recordedAt, Double speedMetersPerMinute) {
        this.pointIndex = pointIndex;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevationMeters = elevationMeters;
        this.recordedAt = recordedAt;
        this.speedMetersPerMinute = speedMetersPerMinute;
    }
}
