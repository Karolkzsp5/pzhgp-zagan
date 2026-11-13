import { UserRole } from '@/app/utils/authSession';

/** Dane zalogowanego użytkownika na stronie ustawień (GET /api/account). */
export interface AccountDto {
    name: string;
    surname: string;
    dateOfBirth: string;
    email: string;
    phoneNumber: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
    sectionName: string;
    role: UserRole;
    createdAt: string;
    /** Funkcje w zarządzie, np. „Prezes – Zarząd Oddziału”. */
    boardFunctions: string[];
}

/** Dane kontaktowe, które użytkownik może zmienić sam (PUT /api/account/contact). */
export interface ContactUpdateRequest {
    phoneNumber: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
}

/** Zmiana hasła przez zalogowanego użytkownika (PUT /api/account/password). */
export interface PasswordChangeRequest {
    currentPassword: string;
    newPassword: string;
}

/** Kopia danych użytkownika (GET /api/account/export), zapisywana jako plik JSON. */
export interface AccountDataExport {
    exportedAt: string;
    account: AccountDto;
    forumThreads: { title: string; category: string; createdAt: string }[];
    forumPosts: { thread: string; body: string; createdAt: string; editedAt: string | null }[];
    announcements: { title: string; content: string; createdAt: string; updatedAt: string | null }[];
    flights: {
        name: string;
        ringNumber: string | null;
        releaseSite: string | null;
        fileName: string;
        startTime: string | null;
        endTime: string | null;
        trackDistanceMeters: number;
        durationSeconds: number;
        uploadedAt: string;
    }[];
}