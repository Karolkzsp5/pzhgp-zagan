import { PageResponse } from '@/app/types/common';
import { FoundPigeonDto, FoundPigeonRequest, FoundPigeonStatus } from '@/app/types/foundPigeon';
import { requestCreatedId, requestJson, requestVoid } from '@/app/utils/apiClient';

export const foundPigeonService = {
    /**
     * Wysyła publiczne zgłoszenie odnalezienia gołębia.
     *
     * Jedyna operacja modułu działająca bez logowania, dlatego jest wysyłana jako zapytanie
     * publiczne — nie ma sesji, która mogłaby wygasnąć.
     */
    submitReport: (request: FoundPigeonRequest): Promise<number> =>
        requestCreatedId('/api/found-pigeons', 'Nie udało się wysłać zgłoszenia.', { method: 'POST', body: request, isPublic: true }),

    /** Lista zgłoszeń dla panelu administratora. */
    getReports: (status: FoundPigeonStatus | null, ringNumber: string, page = 0, size = 10): Promise<PageResponse<FoundPigeonDto>> => {
        const params = new URLSearchParams({ page: String(page), size: String(size) });
        if (status) params.set('status', status);
        if (ringNumber.trim()) params.set('ringNumber', ringNumber.trim());

        return requestJson(`/api/admin/found-pigeons?${params}`, 'Nie udało się pobrać zgłoszeń.');
    },

    updateStatus: (id: number, status: FoundPigeonStatus): Promise<FoundPigeonDto> =>
        requestJson(`/api/admin/found-pigeons/${id}/status`, 'Nie udało się zmienić statusu zgłoszenia.', { method: 'PATCH', body: { status } }),

    updateNote: (id: number, adminNote: string): Promise<FoundPigeonDto> =>
        requestJson(`/api/admin/found-pigeons/${id}/note`, 'Nie udało się zapisać notatki.', { method: 'PATCH', body: { adminNote } }),

    deleteReport: (id: number): Promise<void> =>
        requestVoid(`/api/admin/found-pigeons/${id}`, 'Nie udało się usunąć zgłoszenia.', { method: 'DELETE' })
};