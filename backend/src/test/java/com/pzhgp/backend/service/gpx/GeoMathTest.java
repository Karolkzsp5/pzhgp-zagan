package com.pzhgp.backend.service.gpx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoMathTest {

    @Test
    @DisplayName("Should return zero distance for identical coordinates")
    void returnsZeroForIdenticalCoordinates() {
        double distance = GeoMath.distance(51.0, 15.0, 51.0, 15.0);

        assertEquals(0.0, distance, 1e-9);
    }

    @Test
    @DisplayName("Should calculate symmetric Haversine distances")
    void distanceIsSymmetric() {
        double forward = GeoMath.distance(51.0, 15.0, 52.0, 16.0);
        double backward = GeoMath.distance(52.0, 16.0, 51.0, 15.0);

        assertEquals(forward, backward, 1e-9);
    }

    @Test
    @DisplayName("Should calculate approximately 111 km for one degree of latitude")
    void computesKnownLatitudeDistance() {
        double distance = GeoMath.distance(51.0, 15.0, 52.0, 15.0);

        assertEquals(111_195.0, distance, 200.0);
    }

    @Test
    @DisplayName("Should return zero distance for a point lying on a segment")
    void pointOnSegmentHasZeroDistance() {
        double distance = GeoMath.distanceToSegment(51.0, 15.005, 51.0, 15.0, 51.0, 15.01);

        assertEquals(0.0, distance, 1e-6);
    }

    @Test
    @DisplayName("Should calculate perpendicular distance to a horizontal segment in meters")
    void computesPerpendicularDistanceToSegment() {
        double distance = GeoMath.distanceToSegment(51.001, 15.005, 51.0, 15.0, 51.0, 15.01);

        assertEquals(111.2, distance, 2.0);
    }

    @Test
    @DisplayName("Should measure distance to the nearest endpoint when projection falls outside a segment")
    void clampsProjectionToSegmentEndpoint() {
        double distance = GeoMath.distanceToSegment(51.0, 14.999, 51.0, 15.0, 51.0, 15.01);

        assertTrue(distance > 60.0);
        assertTrue(distance < 80.0);
    }

    @Test
    @DisplayName("Should measure distance to a single point when the segment has zero length")
    void handlesZeroLengthSegment() {
        double distance = GeoMath.distanceToSegment(51.001, 15.0, 51.0, 15.0, 51.0, 15.0);

        assertEquals(111.2, distance, 2.0);
    }
}