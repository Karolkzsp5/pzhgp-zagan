import { AnnouncementDto, AnnouncementRequest } from '@/app/types/announcement';
import { PageResponse } from '@/app/types/common';
import { requestJson, requestVoid } from '@/app/utils/apiClient';

const ANNOUNCEMENTS_PAGE_SIZE = 10;

export const announcementService = {
    /** Ogłoszenia od przypiętych i najnowszych (endpoint publiczny). */
    getAnnouncements: (page = 0): Promise<PageResponse<AnnouncementDto>> =>
        requestJson(`/api/announcements?page=${page}&size=${ANNOUNCEMENTS_PAGE_SIZE}`, 'Nie udało się pobrać ogłoszeń.', { isPublic: true }),

    createAnnouncement: (data: AnnouncementRequest): Promise<void> =>
        requestVoid('/api/announcements', 'Wystąpił błąd podczas zapisywania ogłoszenia.', { method: 'POST', body: data }),

    updateAnnouncement: (id: number, data: AnnouncementRequest): Promise<void> =>
        requestVoid(`/api/announcements/${id}`, 'Wystąpił błąd podczas zapisywania ogłoszenia.', { method: 'PUT', body: data }),

    deleteAnnouncement: (id: number): Promise<void> =>
        requestVoid(`/api/announcements/${id}`, 'Nie udało się usunąć ogłoszenia.', { method: 'DELETE' })
};