export interface NotificationDto {
    id: number;
    message: string;
    link: string | null;
    isRead: boolean;
    type: string;
    createdAt: string;
}
