import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';

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

export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number;
}

const ensureOk = async (response: Response, fallback: string): Promise<void> => {
    if (!response.ok) throw new Error(await readApiError(response, fallback));
};

export const fetchCategories = async (): Promise<ForumCategoryDto[]> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/categories`);
    await ensureOk(response, 'Nie udało się pobrać kategorii.');
    return response.json();
};

export const fetchCategoryById = async (categoryId: number): Promise<ForumCategoryDto> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${categoryId}`);
    await ensureOk(response, 'Nie udało się pobrać kategorii.');
    return response.json();
};

export const createCategory = async (name: string, description: string, sortOrder: number): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/categories`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description, sortOrder })
    });
    await ensureOk(response, 'Nie udało się utworzyć kategorii.');
};

export const updateCategory = async (id: number, name: string, description: string, sortOrder: number): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description, sortOrder })
    });
    await ensureOk(response, 'Nie udało się zaktualizować kategorii.');
};

export const deleteCategory = async (id: number): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${id}`, {
        method: 'DELETE'
    });
    await ensureOk(response, 'Nie udało się usunąć kategorii. Upewnij się, że nie ma w niej wątków.');
};

export const fetchThreadsByCategory = async (categoryId: number, page = 0): Promise<PageResponse<ForumThreadDto>> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${categoryId}/threads?page=${page}&size=15`);
    await ensureOk(response, 'Nie udało się pobrać wątków.');
    return response.json();
};

export const fetchThreadById = async (threadId: number): Promise<ForumThreadDto & { categoryName?: string }> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}`);
    await ensureOk(response, 'Nie udało się pobrać wątku.');
    return response.json();
};

export const updateThreadTitle = async (threadId: number, title: string): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}/title`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title })
    });
    await ensureOk(response, 'Nie udało się zaktualizować tytułu wątku.');
};

export const deleteThread = async (id: number): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${id}`, {
        method: 'DELETE'
    });
    await ensureOk(response, 'Nie udało się usunąć wątku.');
};

export const toggleThreadStatus = async (id: number, action: 'LOCK' | 'PIN'): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${id}/${action.toLowerCase()}`, {
        method: 'PUT'
    });
    await ensureOk(response, `Nie udało się zmienić statusu wątku (${action}).`);
};

export const fetchPostsByThread = async (threadId: number, page = 0): Promise<PageResponse<ForumPostDto>> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}/posts?page=${page}&size=20`);
    await ensureOk(response, 'Nie udało się pobrać postów.');
    return response.json();
};

export const createPost = async (threadId: number, body: string): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}/posts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ body })
    });
    await ensureOk(response, 'Nie udało się dodać odpowiedzi.');
};

export const updatePost = async (postId: number, body: string): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/posts/${postId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ body })
    });
    await ensureOk(response, 'Nie udało się zaktualizować wpisu.');
};

export const deletePost = async (id: number): Promise<void> => {
    const response = await fetchWithAuth(`${API_URL}/api/forum/posts/${id}`, {
        method: 'DELETE'
    });
    await ensureOk(response, 'Nie można usunąć wpisu.');
};
