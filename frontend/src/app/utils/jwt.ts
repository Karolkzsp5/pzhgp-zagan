const TOKEN_KEY = 'jwt_token';
const AUTH_PERSISTENCE_KEY = 'auth_persistence';
const SESSION_MARKER_COOKIE = 'pzhgp_session';

type AuthPersistence = 'session' | 'persistent';

const hasSessionMarker = (): boolean => {
    if (typeof document === 'undefined') return false;

    return document.cookie.split('; ').some(cookie => cookie === `${SESSION_MARKER_COOKIE}=1`);
};

const setSessionMarker = () => {
    if (typeof document === 'undefined') return;

    const secure = window.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${SESSION_MARKER_COOKIE}=1; Path=/; SameSite=Strict${secure}`;
};

const clearSessionMarker = () => {
    if (typeof document === 'undefined') return;

    const secure = window.location.protocol === 'https:' ? '; Secure' : '';
    document.cookie = `${SESSION_MARKER_COOKIE}=; Path=/; Max-Age=0; SameSite=Strict${secure}`;
};

const clearAuthStorage = () => {
    if (typeof window === 'undefined') return;

    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(AUTH_PERSISTENCE_KEY);
    sessionStorage.removeItem(TOKEN_KEY);

    clearSessionMarker();
};

export const saveAuthToken = (token: string, rememberMe: boolean) => {
    if (typeof window === 'undefined') return;
    localStorage.setItem(TOKEN_KEY, token);

    if (rememberMe) {
        localStorage.setItem(AUTH_PERSISTENCE_KEY, 'persistent');
        clearSessionMarker();
    } else {
        localStorage.setItem(AUTH_PERSISTENCE_KEY, 'session');
        setSessionMarker();
    }

    sessionStorage.removeItem(TOKEN_KEY);
};

export const getAuthToken = (): string | null => {
    if (typeof window === 'undefined') return null;

    const token = localStorage.getItem(TOKEN_KEY);
    const persistence = localStorage.getItem(AUTH_PERSISTENCE_KEY) as AuthPersistence | null;

    if (!token || !persistence) {
        clearAuthStorage();
        return null;
    }

    if (persistence === 'session' && !hasSessionMarker()) {
        clearAuthStorage();
        return null;
    }

    return token;
};

export const decodeJwt = (token: string) => {
    try {
        const payloadBase64 = token.split('.')[1];
        const base64 = payloadBase64
            .replace(/-/g, '+')
            .replace(/_/g, '/');

        const decodedJson = decodeURIComponent(atob(base64).split('').map(c =>
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
        );

        return JSON.parse(decodedJson);
    } catch (error) {
        console.error('Błąd dekodowania tokenu JWT:', error);
        return null;
    }
};

export const isJwtValid = (token: string | null): boolean => {
    if (!token) return false;
    const payload = decodeJwt(token);

    if (!payload) return false;
    if (!payload.exp) return true;

    return payload.exp * 1000 > Date.now();
};

export const logout = () => {
    clearAuthStorage();
};

export const getUserRole = (token: string | null): string | null => {
    if (!isJwtValid(token)) return null;
    const payload = decodeJwt(token!);

    return payload?.role || null;
};