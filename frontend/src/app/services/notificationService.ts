import { PageResponse } from '@/app/types/common';
import { NotificationDto } from '@/app/types/notification';
import { requestJson, requestVoid } from '@/app/utils/apiClient';

const NOTIFICATIONS_PAGE_SIZE = 50;

export const notificationService = {
    /** Powiadomienia zalogowanego użytkownika od najnowszego. */
    getNotifications: (page = 0): Promise<PageResponse<NotificationDto>> =>
        requestJson(`/api/notifications?page=${page}&size=${NOTIFICATIONS_PAGE_SIZE}`, 'Nie udało się pobrać powiadomień.'),

    getUnreadCount: (): Promise<number> =>
        requestJson('/api/notifications/unread-count', 'Nie udało się pobrać liczby nieprzeczytanych powiadomień.'),

    markAsRead: (id: number): Promise<void> =>
        requestVoid(`/api/notifications/${id}/read`, 'Nie udało się oznaczyć powiadomienia jako przeczytane.', { method: 'PUT' }),

    markAllAsRead: (): Promise<void> =>
        requestVoid('/api/notifications/read-all', 'Nie udało się oznaczyć wszystkich powiadomień jako przeczytane.', { method: 'PUT' }),

    deleteNotification: (id: number): Promise<void> =>
        requestVoid(`/api/notifications/${id}`, 'Nie udało się usunąć powiadomienia.', { method: 'DELETE' })
};