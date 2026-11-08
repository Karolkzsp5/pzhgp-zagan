import { getAuthGeneration, invalidateAuthSession } from './authSession';
import { loginUrlReturningTo } from './loginRedirect';

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? '';
const CONNECTION_ERROR_MESSAGE = 'Nie udało się połączyć z serwerem.';
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';
let csrfRefreshPromise: Promise<string> | null = null;

const getCsrfToken = (): string | null => {
    if (typeof document === 'undefined') return null;
    const csrfCookie = document.cookie.split('; ').find(cookie => cookie.startsWith(`${CSRF_COOKIE_NAME}=`));
    return csrfCookie ? decodeURIComponent(csrfCookie.slice(CSRF_COOKIE_NAME.length + 1)) : null;
};

export const refreshCsrfToken = (): Promise<string> => {
    if (csrfRefreshPromise) return csrfRefreshPromise;
    csrfRefreshPromise = (async () => {
        let response: Response;
        try {
            response = await fetch(`${API_URL}/api/auth/csrf`, {
                method: 'GET',
                credentials: 'include',
                cache: 'no-store'
            });
        } catch (error) {
            if (error instanceof TypeError) throw new TypeError(CONNECTION_ERROR_MESSAGE);
            throw error;
        }
        if (!response.ok) {
            throw new Error('Nie udało się przygotować zabezpieczenia CSRF.');
        }
        const token = getCsrfToken();
        if (!token) {
            throw new Error('Nie można odczytać tokenu CSRF. Sprawdź adresy frontendu i backendu.');
        }
        return token;
    })();
    void csrfRefreshPromise.finally(() => { csrfRefreshPromise = null; }).catch(() => {});
    return csrfRefreshPromise;
};

// Klient dla endpointów publicznych; dla chronionych służy fetchWithAuth.
// JWT przesyła przeglądarka w HttpOnly cookie; nie odczytujemy go w JavaScript.
export const fetchApi = async (url: string, options: RequestInit = {}): Promise<Response> => {
    const method = (options.method ?? 'GET').toUpperCase();
    const headers = new Headers(options.headers);

    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
        const csrfToken = getCsrfToken() ?? await refreshCsrfToken();
        headers.set('X-XSRF-TOKEN', csrfToken);
    }

    try {
        return await fetch(url, { ...options, credentials: 'include', headers });
    } catch (error) {
        if (error instanceof TypeError) throw new TypeError(CONNECTION_ERROR_MESSAGE);
        throw error;
    }
};

/** Odpowiedź API z kodem błędu. Komunikat pochodzi z serwera albo jest zastępczy. */
export class ApiError extends Error {
    constructor(public readonly status: number, message: string) {
        super(message);
        this.name = 'ApiError';
    }
}

/**
 * Komunikat błędu z odpowiedzi serwera. Backend zwraca błędy jako zwykły tekst; pusta treść
 * albo strona HTML (np. od serwera proxy) daje podany tekst zastępczy.
 */
const readApiError = async (response: Response, fallback: string): Promise<string> => {
    const message = (await response.text()).trim();
    return message.length > 0 && !message.startsWith('<') ? message : fallback;
};

/**
 * Komunikat dla użytkownika po nieudanej operacji: tekst z serwera, informacja o braku połączenia
 * albo podany tekst zastępczy. Wszystkie strony pokazują błędy w ten sam sposób.
 */
export const getErrorMessage = (error: unknown, fallback: string): string => {
    if (error instanceof TypeError) return CONNECTION_ERROR_MESSAGE;
    if (error instanceof Error && error.message.trim().length > 0) return error.message;
    return fallback;
};

/** Zgłasza ApiError z komunikatem serwera (albo podanym zastępczym), gdy odpowiedź nie jest poprawna. */
export const ensureOk = async (response: Response, fallback: string): Promise<void> => {
    if (!response.ok) {
        throw new ApiError(response.status, await readApiError(response, fallback));
    }
};

/** Klient dla endpointów wymagających logowania: po odpowiedzi 401 przenosi na stronę logowania. */
export const fetchWithAuth = async (url: string, options: RequestInit = {}): Promise<Response> => {
    const requestGeneration = getAuthGeneration();
    const response = await fetchApi(url, options);

    if (response.status === 401 && requestGeneration === getAuthGeneration()) {
        invalidateAuthSession();
        if (typeof window !== 'undefined') {
            window.location.href = loginUrlReturningTo(window.location.pathname + window.location.search, { expired: 'true' });
        }
        throw new Error('Sesja wygasła. Zaloguj się ponownie.');
    }

    return response;
};