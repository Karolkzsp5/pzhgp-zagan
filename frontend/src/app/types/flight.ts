export interface FlightTrackPointDto {
    latitude: number;
    longitude: number;
    elevation: number | null;
    /** Znacznik czasu w UTC (ISO-8601) — konwersja na czas lokalny odbywa się w przeglądarce. */
    time: string | null;
    /** Prędkość chwilowa w metrach na minutę. */
    speedMetersPerMinute: number | null;
}

export interface FlightStatisticsDto {
    straightLineDistanceKm: number;
    trackDistanceKm: number;
    durationSeconds: number;
    /** Prędkość średnia po trasie, w m/min — jednostka używana na listach konkursowych PZHGP. */
    averageSpeedMetersPerMinute: number;
    /** Prędkość liczona po linii prostej, w m/min. */
    straightLineSpeedMetersPerMinute: number;
    maxSpeedMetersPerMinute: number;
    minElevationMeters: number | null;
    maxElevationMeters: number | null;
    elevationGainMeters: number | null;
    totalPoints: number;
    timestampsAvailable: boolean;
}

export interface FlightSummaryDto {
    id: number;
    name: string;
    ringNumber: string | null;
    releaseSite: string | null;
    ownerName: string;
    startTime: string | null;
    endTime: string | null;
    straightLineDistanceKm: number;
    durationSeconds: number;
    averageSpeedMetersPerMinute: number;
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
    startTime: string | null;
    endTime: string | null;
    startLatitude: number;
    startLongitude: number;
    endLatitude: number;
    endLongitude: number;
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

/** Formatuje prędkość w metrach na minutę, np. "1 378 m/min". */
export const formatSpeed = (metersPerMinute: number | null): string => {
    if (metersPerMinute === null || !Number.isFinite(metersPerMinute) || metersPerMinute <= 0) return '—';

    return `${Math.round(metersPerMinute).toLocaleString('pl-PL')} m/min`;
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