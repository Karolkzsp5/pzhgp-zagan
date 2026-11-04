export interface AnnouncementDto {
    id: number;
    title: string;
    content: string;
    authorName: string;
    isPinned: boolean;
    createdAt: string;
    updatedAt: string | null;
    canEdit: boolean;
    canDelete: boolean;
}
