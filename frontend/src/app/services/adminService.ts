import { AccountAction, BreederDto } from '@/app/types/breeder';
import { API_URL, ensureOk, fetchWithAuth } from '@/app/utils/apiClient';
import { UserRole } from '@/app/utils/authSession';

export const adminService = {
    /** Konta czekające na zatwierdzenie przez administratora. */
    getPendingAccounts: async (): Promise<BreederDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/admin/accounts/pending`);
        await ensureOk(response, 'Wystąpił błąd podczas pobierania danych.');
        return response.json();
    },

    /** Zatwierdzone konta: aktywne i zablokowane. */
    getRegisteredAccounts: async (): Promise<BreederDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/admin/accounts/registered`);
        await ensureOk(response, 'Wystąpił błąd podczas pobierania danych.');
        return response.json();
    },

    /** Zatwierdza, odrzuca (usuwa), blokuje albo odblokowuje konto. */
    applyAccountAction: async (id: number, action: AccountAction): Promise<void> => {
        const response = action === 'reject'
            ? await fetchWithAuth(`${API_URL}/api/admin/accounts/${id}`, { method: 'DELETE' })
            : await fetchWithAuth(`${API_URL}/api/admin/accounts/${id}/${action}`, { method: 'PUT' });
        await ensureOk(response, 'Nie udało się wykonać operacji.');
    },

    changeRole: async (id: number, role: UserRole): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/admin/accounts/${id}/role`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role })
        });
        await ensureOk(response, 'Nie udało się zmienić roli.');
    }
};
