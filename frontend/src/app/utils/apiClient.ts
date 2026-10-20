import { getAuthToken, isJwtValid, logout } from './jwt';

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? '';
export const CONNECTION_ERROR_MESSAGE = 'Nie udało się połączyć z serwerem.';

// Wspólna obsługa błędów połączenia, również dla endpointów publicznych.
export const fetchApi = async (url: string, options: RequestInit = {}): Promise<Response> => {
    try {
        return await fetch(url, options);
    } catch (error) {
        if (error instanceof TypeError) {
            throw new TypeError(CONNECTION_ERROR_MESSAGE);
        }

        throw error;
    }
};

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

    const response = await fetchApi(url, {
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