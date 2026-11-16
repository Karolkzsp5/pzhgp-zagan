import { AccountAction, BreederDto } from '@/app/types/breeder';
import { requestJson, requestVoid } from '@/app/utils/apiClient';
import { UserRole } from '@/app/utils/authSession';

export const adminService = {
    /** Konta czekające na zatwierdzenie przez administratora. */
    getPendingAccounts: (): Promise<BreederDto[]> =>
        requestJson('/api/admin/accounts/pending', 'Wystąpił błąd podczas pobierania danych.'),

    /** Zatwierdzone konta: aktywne i zablokowane. */
    getRegisteredAccounts: (): Promise<BreederDto[]> =>
        requestJson('/api/admin/accounts/registered', 'Wystąpił błąd podczas pobierania danych.'),

    /** Zatwierdza, odrzuca (usuwa), blokuje albo odblokowuje konto. */
    applyAccountAction: (id: number, action: AccountAction): Promise<void> =>
        action === 'reject'
            ? requestVoid(`/api/admin/accounts/${id}`, 'Nie udało się wykonać operacji.', { method: 'DELETE' })
            : requestVoid(`/api/admin/accounts/${id}/${action}`, 'Nie udało się wykonać operacji.', { method: 'PUT' }),

    changeRole: (id: number, role: UserRole): Promise<void> =>
        requestVoid(`/api/admin/accounts/${id}/role`, 'Nie udało się zmienić roli.', { method: 'PUT', body: { role } })
};