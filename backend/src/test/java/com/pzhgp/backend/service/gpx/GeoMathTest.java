package com.pzhgp.backend.service.gpx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoMathTest {

    @Test
    @DisplayName("Distance between identical coordinates is zero")
    void returnsZeroForIdenticalCoordinates() {
        double distance = GeoMath.distance(51.0, 15.0, 51.0, 15.0);

        assertEquals(0.0, distance, 1e-9);
    }

    @Test
    @DisplayName("Haversine distance is symmetric")
    void distanceIsSymmetric() {
        double forward = GeoMath.distance(51.0, 15.0, 52.0, 16.0);
        double backward = GeoMath.distance(52.0, 16.0, 51.0, 15.0);

        assertEquals(forward, backward, 1e-9);
    }

    @Test
    @DisplayName("One degree of latitude is approximately one hundred eleven kilometers")
    void computesKnownLatitudeDistance() {
        double distance = GeoMath.distance(51.0, 15.0, 52.0, 15.0);

        assertEquals(111_195.0, distance, 200.0);
    }

    @Test
    @DisplayName("Point located directly on a segment has zero distance to that segment")
    void pointOnSegmentHasZeroDistance() {
        double distance = GeoMath.distanceToSegment(51.0, 15.005, 51.0, 15.0, 51.0, 15.01);

        assertEquals(0.0, distance, 1e-6);
    }

    @Test
    @DisplayName("Perpendicular point distance to a horizontal segment is calculated in meters")
    void computesPerpendicularDistanceToSegment() {
        double distance = GeoMath.distanceToSegment(51.001, 15.005, 51.0, 15.0, 51.0, 15.01);

        assertEquals(111.2, distance, 2.0);
    }

    @Test
    @DisplayName("Point outside the segment is measured from the nearest endpoint")
    void clampsProjectionToSegmentEndpoint() {
        double distance = GeoMath.distanceToSegment(51.0, 14.999, 51.0, 15.0, 51.0, 15.01);

        assertTrue(distance > 60.0);
        assertTrue(distance < 80.0);
    }

    @Test
    @DisplayName("Zero-length segment is handled as distance to a single point")
    void handlesZeroLengthSegment() {
        double distance = GeoMath.distanceToSegment(51.001, 15.0, 51.0, 15.0, 51.0, 15.0);

        assertEquals(111.2, distance, 2.0);
    }
}