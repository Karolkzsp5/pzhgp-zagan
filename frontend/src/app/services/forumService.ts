import { PageResponse } from '@/app/types/common';
import { ForumCategoryDto, ForumPostDto, ForumThreadDto, ThreadStatusChange } from '@/app/types/forum';
import { requestJson, requestVoid } from '@/app/utils/apiClient';

export const forumService = {
    getCategories: (): Promise<ForumCategoryDto[]> =>
        requestJson('/api/forum/categories', 'Nie udało się pobrać kategorii.'),

    getCategoryById: (categoryId: number): Promise<ForumCategoryDto> =>
        requestJson(`/api/forum/categories/${categoryId}`, 'Nie udało się pobrać kategorii.'),

    createCategory: (name: string, description: string, sortOrder: number): Promise<void> =>
        requestVoid('/api/forum/categories', 'Nie udało się utworzyć kategorii.', {
            method: 'POST',
            body: { name, description, sortOrder }
        }),

    updateCategory: (id: number, name: string, description: string, sortOrder: number): Promise<void> =>
        requestVoid(`/api/forum/categories/${id}`, 'Nie udało się zaktualizować kategorii.', {
            method: 'PUT',
            body: { name, description, sortOrder }
        }),

    deleteCategory: (id: number): Promise<void> =>
        requestVoid(`/api/forum/categories/${id}`, 'Nie udało się usunąć kategorii. Upewnij się, że nie ma w niej wątków.', { method: 'DELETE' }),

    getThreadsByCategory: (categoryId: number, page = 0): Promise<PageResponse<ForumThreadDto>> =>
        requestJson(`/api/forum/categories/${categoryId}/threads?page=${page}&size=15`, 'Nie udało się pobrać wątków.'),

    createThread: (categoryId: number, title: string, initialPostContent: string): Promise<void> =>
        requestVoid('/api/forum/threads', 'Wystąpił błąd podczas tworzenia wątku.', {
            method: 'POST',
            body: { categoryId, title, initialPostContent }
        }),

    getThreadById: (threadId: number, countView = true): Promise<ForumThreadDto> => {
        const query = countView ? '' : '?countView=false';
        return requestJson(`/api/forum/threads/${threadId}${query}`, 'Nie udało się pobrać wątku.');
    },

    updateThreadTitle: (threadId: number, title: string): Promise<void> =>
        requestVoid(`/api/forum/threads/${threadId}/title`, 'Nie udało się zaktualizować tytułu wątku.', { method: 'PATCH', body: { title } }),

    deleteThread: (id: number): Promise<void> =>
        requestVoid(`/api/forum/threads/${id}`, 'Nie udało się usunąć wątku.', { method: 'DELETE' }),

    /** Ustawia blokadę albo przypięcie wątku na podaną wartość (ponowne wysłanie niczego nie przełącza). */
    updateThreadStatus: (id: number, status: ThreadStatusChange): Promise<void> => {
        const fallback = 'isLocked' in status ? 'Nie udało się zamknąć ani otworzyć wątku.' : 'Nie udało się przypiąć ani odpiąć wątku.';
        return requestVoid(`/api/forum/threads/${id}/status`, fallback, { method: 'PATCH', body: status });
    },

    getPostsByThread: (threadId: number, page = 0): Promise<PageResponse<ForumPostDto>> =>
        requestJson(`/api/forum/threads/${threadId}/posts?page=${page}&size=20`, 'Nie udało się pobrać postów.'),

    createPost: (threadId: number, body: string): Promise<void> =>
        requestVoid(`/api/forum/threads/${threadId}/posts`, 'Nie udało się dodać odpowiedzi.', { method: 'POST', body: { body } }),

    updatePost: (postId: number, body: string): Promise<void> =>
        requestVoid(`/api/forum/posts/${postId}`, 'Nie udało się zaktualizować wpisu.', { method: 'PUT', body: { body } }),

    deletePost: (id: number): Promise<void> =>
        requestVoid(`/api/forum/posts/${id}`, 'Nie można usunąć wpisu.', { method: 'DELETE' })
};