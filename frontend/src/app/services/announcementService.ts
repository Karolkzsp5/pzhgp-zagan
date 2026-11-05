import { AnnouncementDto, AnnouncementRequest } from '@/app/types/announcement';
import { PageResponse } from '@/app/types/common';
import { API_URL, ensureOk, fetchWithAuth } from '@/app/utils/apiClient';

const ANNOUNCEMENTS_PAGE_SIZE = 10;

export const announcementService = {
    /** Ogłoszenia od przypiętych i najnowszych (endpoint publiczny). */
    getAnnouncements: async (page = 0): Promise<PageResponse<AnnouncementDto>> => {
        const response = await fetchWithAuth(`${API_URL}/api/announcements?page=${page}&size=${ANNOUNCEMENTS_PAGE_SIZE}`);
        await ensureOk(response, 'Nie udało się pobrać ogłoszeń.');
        return response.json();
    },

    createAnnouncement: async (data: AnnouncementRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/announcements`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        await ensureOk(response, 'Wystąpił błąd podczas zapisywania ogłoszenia.');
    },

    updateAnnouncement: async (id: number, data: AnnouncementRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/announcements/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        await ensureOk(response, 'Wystąpił błąd podczas zapisywania ogłoszenia.');
    },

    deleteAnnouncement: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/announcements/${id}`, { method: 'DELETE' });
        await ensureOk(response, 'Nie udało się usunąć ogłoszenia.');
    }
};
