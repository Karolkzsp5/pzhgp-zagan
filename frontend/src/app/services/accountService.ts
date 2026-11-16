import { AccountDataExport, AccountDto, ContactUpdateRequest, PasswordChangeRequest } from '@/app/types/account';
import { requestJson, requestVoid } from '@/app/utils/apiClient';

export const accountService = {
    getAccount: (): Promise<AccountDto> =>
        requestJson('/api/account', 'Nie udało się pobrać danych konta.', { cache: 'no-store' }),

    updateContact: (request: ContactUpdateRequest): Promise<AccountDto> =>
        requestJson('/api/account/contact', 'Nie udało się zapisać danych kontaktowych.', { method: 'PUT', body: request }),

    /** Po zmianie hasła backend kończy wszystkie sesje konta i usuwa ciasteczko bieżącej sesji. */
    changePassword: (request: PasswordChangeRequest): Promise<void> =>
        requestVoid('/api/account/password', 'Nie udało się zmienić hasła.', { method: 'PUT', body: request }),

    /** Kończy wszystkie sesje konta, także bieżącą. */
    logoutAllDevices: (): Promise<void> =>
        requestVoid('/api/account/logout-all', 'Nie udało się wylogować ze wszystkich urządzeń.', { method: 'POST' }),

    exportData: (): Promise<AccountDataExport> =>
        requestJson('/api/account/export', 'Nie udało się pobrać danych konta.', { cache: 'no-store' })
};
