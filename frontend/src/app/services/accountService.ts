import { AccountDataExport, AccountDto, ContactUpdateRequest, PasswordChangeRequest } from '@/app/types/account';
import { API_URL, ensureOk, fetchWithAuth } from '@/app/utils/apiClient';

export const accountService = {
    getAccount: async (): Promise<AccountDto> => {
        const response = await fetchWithAuth(`${API_URL}/api/account`, { cache: 'no-store' });
        await ensureOk(response, 'Nie udało się pobrać danych konta.');
        return response.json();
    },

    updateContact: async (request: ContactUpdateRequest): Promise<AccountDto> => {
        const response = await fetchWithAuth(`${API_URL}/api/account/contact`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        await ensureOk(response, 'Nie udało się zapisać danych kontaktowych.');
        return response.json();
    },

    /** Po zmianie hasła backend kończy wszystkie sesje konta i usuwa ciasteczko bieżącej sesji. */
    changePassword: async (request: PasswordChangeRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/account/password`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        await ensureOk(response, 'Nie udało się zmienić hasła.');
    },

    /** Kończy wszystkie sesje konta, także bieżącą. */
    logoutAllDevices: async (): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/account/logout-all`, { method: 'POST' });
        await ensureOk(response, 'Nie udało się wylogować ze wszystkich urządzeń.');
    },

    exportData: async (): Promise<AccountDataExport> => {
        const response = await fetchWithAuth(`${API_URL}/api/account/export`, { cache: 'no-store' });
        await ensureOk(response, 'Nie udało się pobrać danych konta.');
        return response.json();
    }
};