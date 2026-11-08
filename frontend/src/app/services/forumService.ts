import { PageResponse } from '@/app/types/common';
import { ForumCategoryDto, ForumPostDto, ForumThreadDto, ThreadStatusChange } from '@/app/types/forum';
import { API_URL, ensureOk, fetchWithAuth } from '@/app/utils/apiClient';

export const forumService = {
    getCategories: async (): Promise<ForumCategoryDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/categories`);
        await ensureOk(response, 'Nie udało się pobrać kategorii.');
        return response.json();
    },

    getCategoryById: async (categoryId: number): Promise<ForumCategoryDto> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${categoryId}`);
        await ensureOk(response, 'Nie udało się pobrać kategorii.');
        return response.json();
    },

    createCategory: async (name: string, description: string, sortOrder: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/categories`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description, sortOrder })
        });
        await ensureOk(response, 'Nie udało się utworzyć kategorii.');
    },

    updateCategory: async (id: number, name: string, description: string, sortOrder: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description, sortOrder })
        });
        await ensureOk(response, 'Nie udało się zaktualizować kategorii.');
    },

    deleteCategory: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${id}`, {
            method: 'DELETE'
        });
        await ensureOk(response, 'Nie udało się usunąć kategorii. Upewnij się, że nie ma w niej wątków.');
    },

    getThreadsByCategory: async (categoryId: number, page = 0): Promise<PageResponse<ForumThreadDto>> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/categories/${categoryId}/threads?page=${page}&size=15`);
        await ensureOk(response, 'Nie udało się pobrać wątków.');
        return response.json();
    },

    createThread: async (categoryId: number, title: string, initialPostContent: string): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ categoryId, title, initialPostContent })
        });
        await ensureOk(response, 'Wystąpił błąd podczas tworzenia wątku.');
    },

    getThreadById: async (threadId: number, countView = true): Promise<ForumThreadDto> => {
        const query = countView ? '' : '?countView=false';
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}${query}`);
        await ensureOk(response, 'Nie udało się pobrać wątku.');
        return response.json();
    },

    updateThreadTitle: async (threadId: number, title: string): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}/title`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title })
        });
        await ensureOk(response, 'Nie udało się zaktualizować tytułu wątku.');
    },

    deleteThread: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${id}`, {
            method: 'DELETE'
        });
        await ensureOk(response, 'Nie udało się usunąć wątku.');
    },

    /** Ustawia blokadę albo przypięcie wątku na podaną wartość (ponowne wysłanie niczego nie przełącza). */
    updateThreadStatus: async (id: number, status: ThreadStatusChange): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${id}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(status)
        });
        await ensureOk(response, 'isLocked' in status ? 'Nie udało się zamknąć ani otworzyć wątku.' : 'Nie udało się przypiąć ani odpiąć wątku.');
    },

    getPostsByThread: async (threadId: number, page = 0): Promise<PageResponse<ForumPostDto>> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}/posts?page=${page}&size=20`);
        await ensureOk(response, 'Nie udało się pobrać postów.');
        return response.json();
    },

    createPost: async (threadId: number, body: string): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/threads/${threadId}/posts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ body })
        });
        await ensureOk(response, 'Nie udało się dodać odpowiedzi.');
    },

    updatePost: async (postId: number, body: string): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/posts/${postId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ body })
        });
        await ensureOk(response, 'Nie udało się zaktualizować wpisu.');
    },

    deletePost: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/forum/posts/${id}`, {
            method: 'DELETE'
        });
        await ensureOk(response, 'Nie można usunąć wpisu.');
    }
};