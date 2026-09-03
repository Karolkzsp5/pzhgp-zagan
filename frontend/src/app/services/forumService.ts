import { getAuthToken } from '@/utils/jwt';

export interface ForumCategoryDto {
    id: number;
    name: string;
    description: string;
    sortOrder: number;
    createdAt: string;
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
}

export interface ForumPostDto {
    id: number;
    authorName: string;
    body: string;
    createdAt: string;
    editedAt: string | null;
    canEdit: boolean;
    canDelete: boolean;
}

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number;
}

export const fetchCategories = async (): Promise<ForumCategoryDto[]> => {
    const token = getAuthToken();
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania kategorii');
    return response.json();
};

export const fetchCategoryById = async (categoryId: number): Promise<ForumCategoryDto> => {
    const token = getAuthToken();
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${categoryId}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Nie udało się pobrać kategorii');
    return response.json();
};

export const fetchThreadsByCategory = async (categoryId: number, page = 0): Promise<PageResponse<ForumThreadDto>> => {
    const token = getAuthToken();
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${categoryId}/threads?page=${page}&size=15`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania wątków');
    return response.json();
};

export const fetchThreadById = async (threadId: number): Promise<ForumThreadDto & { categoryName?: string }> => {
    const token = getAuthToken();
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania wątku');
    return response.json();
};

export const fetchPostsByThread = async (threadId: number, page = 0): Promise<PageResponse<ForumPostDto>> => {
    const token = getAuthToken();
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}/posts?page=${page}&size=20`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania postów');
    return response.json();
};

export const createPost = async (threadId: number, body: string): Promise<void> => {
    const token = getAuthToken();
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}/posts`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ body })
    });
    if (!response.ok) throw new Error('Nie udało się dodać odpowiedzi');
};