export type PigeonAgeGroup = 'ADULT' | 'YOUNG';

export type FlightResultScope = 'BRANCH' | 'SECTION';

export interface FlightPlanSummaryDto {
    id: number;
    year: number;
}

export interface FlightResultSummaryDto {
    id: number;
    scope: FlightResultScope;
    sectionId: number | null;
    sectionName: string | null;
    sectionSortOrder: number | null;
    originalFileName: string;
}

export interface FlightPlanEntryDto {
    id: number;
    pigeonAgeGroup: PigeonAgeGroup;
    scheduledDate: string;
    location: string;
    distanceKm: number;
    category: string | null;
    listType: string;
    sortOrder: number;
    results: FlightResultSummaryDto[];
}

export interface FlightPlanDetailsDto {
    id: number;
    year: number;
    adultNotes: string | null;
    youngNotes: string | null;
    adultFlights: FlightPlanEntryDto[];
    youngFlights: FlightPlanEntryDto[];
}

export interface FlightPlanEntryRequest {
    pigeonAgeGroup: PigeonAgeGroup;
    scheduledDate: string;
    location: string;
    distanceKm: number;
    category: string | null;
    listType: string;
    sortOrder: number;
}

export interface FlightPlanNotesRequest {
    adultNotes: string | null;
    youngNotes: string | null;
}

export interface FlightResultUploadRequest {
    scope: FlightResultScope;
    sectionId: number | null;
}