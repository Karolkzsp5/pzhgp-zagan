package com.pzhgp.backend.service.gpx;

/**
 * Obliczenia geodezyjne na sferze wykorzystywane przy analizie tras lotu.
 * <p>
 * Dla dystansów spotykanych w lotach gołębi (do ok. 1000 km) wzór haversine
 * na sferze o promieniu średnim IUGG daje błąd rzędu 0,3%, co jest w zupełności
 * wystarczające, a jednocześnie nie wymaga zewnętrznej biblioteki geodezyjnej.
 */
public final class GeoMath {

    /** Średni promień Ziemi wg IUGG w metrach. */
    private static final double EARTH_RADIUS_METERS = 6_371_008.8;

    private GeoMath() {
    }

    /**
     * Odległość ortodromiczna (po najkrótszym łuku) między dwoma punktami, w metrach.
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = phi2 - phi1;
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);

        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /**
     * Azymut początkowy z punktu 1 do punktu 2, w stopniach (0 = północ, rosnąco zgodnie
     * z ruchem wskazówek zegara).
     */
    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    /**
     * Odległość punktu od odcinka łączącego dwa inne punkty, w metrach.
     * <p>
     * Wykorzystywana przez algorytm Ramera–Douglasa–Peuckera. Współrzędne rzutowane są
     * lokalnie na płaszczyznę (odwzorowanie równoprostokątne), co przy odległościach
     * rzędu kilometrów nie wprowadza istotnego błędu.
     */
    public static double distanceToSegment(double lat, double lon,
                                           double lat1, double lon1,
                                           double lat2, double lon2) {
        double metersPerDegreeLat = Math.PI * EARTH_RADIUS_METERS / 180.0;
        double metersPerDegreeLon = metersPerDegreeLat * Math.cos(Math.toRadians(lat1));

        double px = (lon - lon1) * metersPerDegreeLon;
        double py = (lat - lat1) * metersPerDegreeLat;
        double sx = (lon2 - lon1) * metersPerDegreeLon;
        double sy = (lat2 - lat1) * metersPerDegreeLat;

        double segmentLengthSquared = sx * sx + sy * sy;
        if (segmentLengthSquared == 0) {
            return Math.hypot(px, py);
        }

        double projection = Math.max(0, Math.min(1, (px * sx + py * sy) / segmentLengthSquared));
        return Math.hypot(px - projection * sx, py - projection * sy);
    }
}
