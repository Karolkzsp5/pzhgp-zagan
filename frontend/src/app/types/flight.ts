export interface FlightTrackPointDto {
    latitude: number;
    longitude: number;
    elevation: number | null;
    /** Znacznik czasu w UTC (ISO-8601) — konwersja na czas lokalny odbywa się w przeglądarce. */
    time: string | null;
    speedKmh: number | null;
}

export interface FlightStatisticsDto {
    straightLineDistanceKm: number;
    trackDistanceKm: number;
    rawTrackDistanceKm: number;
    flightDurationSeconds: number;
    totalDurationSeconds: number;
    averageSpeedKmh: number;
    /** Prędkość konkursowa w m/min — jednostka używana w regulaminach lotowych PZHGP. */
    racingVelocityMetersPerMinute: number;
    maxSpeedKmh: number;
    straightnessRatio: number;
    detourPercent: number;
    courseDegrees: number;
    minElevationMeters: number | null;
    maxElevationMeters: number | null;
    elevationGainMeters: number | null;
    /** Droga "przebyta" wyłącznie przez dryf GPS w spoczynku. */
    stationaryNoiseKm: number;
    preFlightDurationSeconds: number;
    postFlightDurationSeconds: number;
    totalPoints: number;
    timestampsAvailable: boolean;
}

export interface FlightSummaryDto {
    id: number;
    name: string;
    ringNumber: string | null;
    releaseSite: string | null;
    ownerName: string;
    releaseTime: string | null;
    arrivalTime: string | null;
    straightLineDistanceKm: number;
    flightDurationSeconds: number;
    averageSpeedKmh: number;
    racingVelocityMetersPerMinute: number;
    totalPoints: number;
    timestampsAvailable: boolean;
    uploadedAt: string;
    canDelete: boolean;
}

export interface FlightDetailsDto {
    id: number;
    name: string;
    ringNumber: string | null;
    releaseSite: string | null;
    ownerName: string;
    originalFileName: string;
    releaseTime: string | null;
    arrivalTime: string | null;
    trackStartTime: string | null;
    trackEndTime: string | null;
    releaseLatitude: number;
    releaseLongitude: number;
    arrivalLatitude: number;
    arrivalLongitude: number;
    statistics: FlightStatisticsDto;
    trackPoints: FlightTrackPointDto[];
    returnedPoints: number;
    uploadedAt: string;
    canDelete: boolean;
}

export interface FlightUploadRequest {
    name?: string | null;
    ringNumber?: string | null;
    releaseSite?: string | null;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

/** Formatuje czas trwania w sekundach jako "2 godz. 13 min" lub "13 min 38 s". */
export const formatDuration = (seconds: number): string => {
    if (!seconds || seconds <= 0) return '—';

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const rest = Math.floor(seconds % 60);

    if (hours > 0) return `${hours} godz. ${minutes} min`;
    if (minutes > 0) return `${minutes} min ${rest} s`;
    return `${rest} s`;
};

/**
 * Formatuje znacznik czasu UTC jako czas lokalny hodowcy.
 * Pliki z obrączek zapisują czas w UTC, a hodowca oczekuje godziny "z zegarka".
 */
export const formatDateTime = (isoTime: string | null): string => {
    if (!isoTime) return '—';

    return new Date(isoTime).toLocaleString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
};

export const formatTime = (isoTime: string | null): string => {
    if (!isoTime) return '—';

    return new Date(isoTime).toLocaleTimeString('pl-PL', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
};

/** Zamienia azymut w stopniach na nazwę kierunku świata. */
export const formatCourse = (degrees: number): string => {
    const directions = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
    const names: Record<string, string> = {
        N: 'północ', NE: 'północny wschód', E: 'wschód', SE: 'południowy wschód',
        S: 'południe', SW: 'południowy zachód', W: 'zachód', NW: 'północny zachód'
    };

    const index = Math.round(degrees / 45) % 8;
    return `${Math.round(degrees)}° (${names[directions[index]]})`;
};
