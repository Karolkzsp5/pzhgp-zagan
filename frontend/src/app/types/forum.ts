export interface ForumCategoryDto {
    id: number;
    name: string;
    description: string;
    sortOrder: number;
    createdAt: string;
    canEdit: boolean;
    canDelete: boolean;
}

export interface ForumThreadDto {
    id: number;
    categoryId: number;
    title: string;
    authorName: string;
    repliesCount: number;
    views: number;
    createdAt: string;
    lastPostAt: string;
    isPinned: boolean;
    isLocked: boolean;
    canEdit: boolean;
    canDelete: boolean;
    canModerate: boolean;
}

export interface ForumPostDto {
    id: number;
    authorName: string;
    authorRole: string;
    body: string;
    createdAt: string;
    editedAt: string | null;
    canEdit: boolean;
    canDelete: boolean;
}
