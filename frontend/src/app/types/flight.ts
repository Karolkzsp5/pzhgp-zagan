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
