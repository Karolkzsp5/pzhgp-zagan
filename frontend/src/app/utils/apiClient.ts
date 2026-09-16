import { getAuthToken, logout } from './jwt';

export const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
    const token = getAuthToken();
    const headers = new Headers(options.headers);

    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
        logout();
        if (typeof window !== 'undefined') {
            window.location.href = '/login?expired=true';
        }
        throw new Error('Sesja wygasła. Zaloguj się ponownie.');
    }

    return response;
};