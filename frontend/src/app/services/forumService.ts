import { getAuthToken } from '@/utils/jwt';

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

const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
    const response = await fetch(url, options);

    if (response.status === 401) {
        if (typeof window !== 'undefined') {
            localStorage.removeItem('jwt_token');
            sessionStorage.removeItem('jwt_token');
            window.location.href = '/login?expired=true';
        }
        throw new Error('Sesja wygasła. Zaloguj się ponownie.');
    }

    return response;
};

export const fetchCategories = async (): Promise<ForumCategoryDto[]> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania kategorii');
    return response.json();
};

export const fetchCategoryById = async (categoryId: number): Promise<ForumCategoryDto> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${categoryId}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Nie udało się pobrać kategorii');
    return response.json();
};

export const createCategory = async (name: string, description: string, sortOrder: number): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name, description, sortOrder })
    });

    if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || 'Nie udało się utworzyć kategorii.');
    }
};

export const updateCategory = async (id: number, name: string, description: string, sortOrder: number): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${id}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name, description, sortOrder })
    });

    if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || 'Nie udało się zaktualizować kategorii.');
    }
};

export const deleteCategory = async (id: number): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${id}`, {
        method: 'DELETE',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Nie udało się usunąć kategorii. Upewnij się, że nie ma w niej wątków.');
};

export const fetchThreadsByCategory = async (categoryId: number, page = 0): Promise<PageResponse<ForumThreadDto>> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${categoryId}/threads?page=${page}&size=15`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania wątków');
    return response.json();
};

export const fetchThreadById = async (threadId: number): Promise<ForumThreadDto & { categoryName?: string }> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania wątku');
    return response.json();
};

export const updateThreadTitle = async (threadId: number, title: string): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}/title`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ title })
    });
    if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || 'Nie udało się zaktualizować tytułu wątku.');
    }
};

export const deleteThread = async (id: number): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${id}`, {
        method: 'DELETE',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Nie udało się usunąć wątku');
};

export const toggleThreadStatus = async (id: number, action: 'LOCK' | 'PIN'): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${id}/${action.toLowerCase()}`, {
        method: 'PUT',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error(`Nie udało się zmienić statusu wątku (${action})`);
};

export const fetchPostsByThread = async (threadId: number, page = 0): Promise<PageResponse<ForumPostDto>> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}/posts?page=${page}&size=20`, {
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Błąd pobierania postów');
    return response.json();
};

export const createPost = async (threadId: number, body: string): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/threads/${threadId}/posts`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ body })
    });
    if (!response.ok) throw new Error('Nie udało się dodać odpowiedzi');
};

export const updatePost = async (postId: number, body: string): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/posts/${postId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ body })
    });
    if (!response.ok) throw new Error('Nie udało się zaktualizować wpisu');
};

export const deletePost = async (id: number): Promise<void> => {
    const token = getAuthToken();
    const response = await fetchWithAuth(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/posts/${id}`, {
        method: 'DELETE',
        headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    if (!response.ok) throw new Error('Nie można usunąć wpisu.');
};
