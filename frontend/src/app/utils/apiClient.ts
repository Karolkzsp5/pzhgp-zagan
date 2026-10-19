import { getAuthToken, isJwtValid, logout } from './jwt';

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? '';

export const readApiError = async (response: Response, fallback: string): Promise<string> => {
    const message = await response.text();
    return message.trim().length > 0 ? message : fallback;
};

export const fetchWithAuth = async (url: string, options: RequestInit = {}): Promise<Response> => {
    const token = getAuthToken();
    const hadToken = token !== null;
    const headers = new Headers(options.headers);
    let requestToken: string | null = null;

    if (token && isJwtValid(token)) {
        headers.set('Authorization', `Bearer ${token}`);
        requestToken = token;
    } else if (token) {
        logout();
    }

    const response = await fetch(url, {
        ...options, headers
    });

    if (response.status === 401) {
        if (getAuthToken() !== requestToken) {
            return response;
        }

        logout();

        if (typeof window !== 'undefined') {
            window.location.href = hadToken ? '/login?expired=true' : '/login';
        }

        throw new Error(hadToken ? 'Sesja wygasła. Zaloguj się ponownie.' : 'Musisz się zalogować, aby wykonać tę operację.');
    }

    return response;
};
