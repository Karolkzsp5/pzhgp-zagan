import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { PageResponse } from '@/app/types/flight';
import { FoundPigeonDto, FoundPigeonRequest, FoundPigeonStatus } from '@/app/types/foundPigeon';

export const foundPigeonService = {
    /**
     * Wysyła publiczne zgłoszenie odnalezienia gołębia.
     *
     * Jedyna operacja modułu działająca bez logowania, dlatego korzysta ze zwykłego
     * fetch zamiast fetchWithAuth — nie ma tokenu do dołączenia ani sesji do odświeżenia.
     */
    submitReport: async (request: FoundPigeonRequest): Promise<number> => {
        const response = await fetch(`${API_URL}/api/found-pigeons`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się wysłać zgłoszenia.'));
        }

        const body = await response.json();
        return body.id as number;
    },

    /** Lista zgłoszeń dla panelu administratora. */
    getReports: async (
        status: FoundPigeonStatus | null,
        ringNumber: string,
        page = 0,
        size = 10
    ): Promise<PageResponse<FoundPigeonDto>> => {
        const params = new URLSearchParams({ page: String(page), size: String(size) });
        if (status) params.set('status', status);
        if (ringNumber.trim()) params.set('ringNumber', ringNumber.trim());

        const response = await fetchWithAuth(`${API_URL}/api/admin/found-pigeons?${params}`);
        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się pobrać zgłoszeń.'));
        }
        return response.json();
    },

    updateStatus: async (id: number, status: FoundPigeonStatus): Promise<FoundPigeonDto> => {
        const response = await fetchWithAuth(`${API_URL}/api/admin/found-pigeons/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        });

        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się zmienić statusu zgłoszenia.'));
        }
        return response.json();
    },

    updateNote: async (id: number, adminNote: string): Promise<FoundPigeonDto> => {
        const response = await fetchWithAuth(`${API_URL}/api/admin/found-pigeons/${id}/note`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ adminNote })
        });

        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się zapisać notatki.'));
        }
        return response.json();
    },

    deleteReport: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/admin/found-pigeons/${id}`, { method: 'DELETE' });

        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się usunąć zgłoszenia.'));
        }
    }
};
