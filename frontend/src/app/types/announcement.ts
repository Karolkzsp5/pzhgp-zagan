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

/** Dane formularza ogłoszenia wysyłane przy dodawaniu i edycji. */
export interface AnnouncementRequest {
    title: string;
    content: string;
    isPinned: boolean;
}
