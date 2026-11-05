import { PageResponse } from '@/app/types/common';
import { NotificationDto } from '@/app/types/notification';
import { API_URL, ensureOk, fetchWithAuth } from '@/app/utils/apiClient';

const NOTIFICATIONS_PAGE_SIZE = 50;

export const notificationService = {
    /** Powiadomienia zalogowanego użytkownika od najnowszego. */
    getNotifications: async (page = 0): Promise<PageResponse<NotificationDto>> => {
        const response = await fetchWithAuth(`${API_URL}/api/notifications?page=${page}&size=${NOTIFICATIONS_PAGE_SIZE}`);
        await ensureOk(response, 'Nie udało się pobrać powiadomień.');
        return response.json();
    },

    getUnreadCount: async (): Promise<number> => {
        const response = await fetchWithAuth(`${API_URL}/api/notifications/unread-count`);
        await ensureOk(response, 'Nie udało się pobrać liczby nieprzeczytanych powiadomień.');
        return response.json();
    },

    markAsRead: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/notifications/${id}/read`, { method: 'PUT' });
        await ensureOk(response, 'Nie udało się oznaczyć powiadomienia jako przeczytane.');
    },

    markAllAsRead: async (): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/notifications/read-all`, { method: 'PUT' });
        await ensureOk(response, 'Nie udało się oznaczyć wszystkich powiadomień jako przeczytane.');
    },

    deleteNotification: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/notifications/${id}`, { method: 'DELETE' });
        await ensureOk(response, 'Nie udało się usunąć powiadomienia.');
    }
};
