/** Język, w którym znalazca wypełnił formularz. */
export type ReportLanguage = 'PL' | 'EN' | 'DE';

/** Status obsługi zgłoszenia. */
export type FoundPigeonStatus = 'PENDING' | 'APPROVED' | 'RESOLVED' | 'REJECTED';

/**
 * Dozwolone przejścia między statusami — te same reguły egzekwuje backend.
 * Interfejs pokazuje wyłącznie dostępne akcje, ale to serwer decyduje o poprawności zmiany.
 */
export const ALLOWED_STATUS_TRANSITIONS: Record<FoundPigeonStatus, FoundPigeonStatus[]> = {
    PENDING: ['APPROVED', 'REJECTED'],
    APPROVED: ['RESOLVED', 'REJECTED'],
    RESOLVED: [],
    REJECTED: []
};

const REPORT_STATUS_LABELS: Record<FoundPigeonStatus, string> = {
    PENDING: 'Oczekujące',
    APPROVED: 'W obsłudze',
    RESOLVED: 'Zakończone',
    REJECTED: 'Odrzucone'
};

const REPORT_LANGUAGE_LABELS: Record<ReportLanguage, string> = {
    PL: 'polski',
    EN: 'angielski',
    DE: 'niemiecki'
};

/** Etykieta akcji zmieniającej status na docelowy. */
const STATUS_ACTION_LABELS: Record<FoundPigeonStatus, string> = {
    PENDING: 'Cofnij do oczekujących',
    APPROVED: 'Zatwierdź',
    RESOLVED: 'Oznacz jako zakończone',
    REJECTED: 'Odrzuć'
};

export const STATUS_BADGE_CLASSES: Record<FoundPigeonStatus, string> = {
    PENDING: 'bg-amber-100 text-amber-800 border-amber-200',
    APPROVED: 'bg-blue-100 text-blue-800 border-blue-200',
    RESOLVED: 'bg-green-100 text-green-800 border-green-200',
    REJECTED: 'bg-gray-100 text-gray-700 border-gray-200'
};

export const formatReportStatus = (status: FoundPigeonStatus) => REPORT_STATUS_LABELS[status] ?? status;
export const formatReportLanguage = (language: ReportLanguage) => REPORT_LANGUAGE_LABELS[language] ?? language;
export const formatStatusAction = (status: FoundPigeonStatus) => STATUS_ACTION_LABELS[status] ?? status;

/** Dane wysyłane przez publiczny formularz. */
export interface FoundPigeonRequest {
    ringNumber: string;
    contactPhone?: string | null;
    contactEmail?: string | null;
    foundLocation?: string | null;
    foundCountry?: string | null;
    description?: string | null;
    preferredLanguage: ReportLanguage;
}

/** Pełne dane zgłoszenia — dostępne wyłącznie w panelu administratora. */
export interface FoundPigeonDto {
    id: number;
    ringNumber: string;
    contactPhone: string | null;
    contactEmail: string | null;
    foundLocation: string | null;
    foundCountry: string | null;
    description: string | null;
    preferredLanguage: ReportLanguage;
    status: FoundPigeonStatus;
    adminNote: string | null;
    createdAt: string;
    updatedAt: string | null;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}