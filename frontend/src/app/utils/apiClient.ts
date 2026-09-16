import { getAuthToken, logout } from './jwt';

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? '';

export const readApiError = async (response: Response, fallback: string): Promise<string> => {
    const message = await response.text();
    return message.trim().length > 0 ? message : fallback;
};

export const fetchWithAuth = async (url: string, options: RequestInit = {}): Promise<Response> => {
    const token = getAuthToken();
    const headers = new Headers(options.headers);

    if (token) headers.set('Authorization', `Bearer ${token}`);

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
        logout();
        if (typeof window !== 'undefined') window.location.href = '/login?expired=true';
        throw new Error('Sesja wygasła. Zaloguj się ponownie.');
    }

    return response;
};
