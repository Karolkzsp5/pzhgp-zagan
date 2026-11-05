import { API_URL, ensureOk, fetchWithAuth } from '@/app/utils/apiClient';
import { PageResponse } from '@/app/types/common';
import { ForumCategoryDto, ForumPostDto, ForumThreadDto } from '@/app/types/forum';

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

export const fetchThreadById = async (threadId: number, countView = true): Promise<ForumThreadDto> => {
    const query = countView ? '' : '?countView=false';
    const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}${query}`);
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