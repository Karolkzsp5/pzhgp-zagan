const TOKEN_KEY = 'jwt_token';
const AUTH_PERSISTENCE_KEY = 'auth_persistence';
const SESSION_MARKER_COOKIE = 'pzhgp_session';

export const AUTH_CHANGED_EVENT = 'pzhgp-auth-changed';

export type AuthPersistence = 'session' | 'persistent';
export type UserRole = 'BREEDER' | 'MODERATOR' | 'ADMINISTRATOR';

export interface JwtPayload {
    id?: number;
    sub?: string;
    name?: string;
    role?: UserRole;
    status?: string;
    exp?: number;
    iat?: number;
}

const notifyAuthChanged = () => {
    if (typeof window === 'undefined') return;

    window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
};

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

const clearAuthStorage = (notify = false) => {
    if (typeof window === 'undefined') return;

    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(AUTH_PERSISTENCE_KEY);
    sessionStorage.removeItem(TOKEN_KEY);

    clearSessionMarker();

    if (notify) {
        notifyAuthChanged();
    }
};

export const saveAuthToken = (token: string, rememberMe: boolean) => {
    if (typeof window === 'undefined') return;

    if (rememberMe) {
        clearSessionMarker();
        localStorage.setItem(AUTH_PERSISTENCE_KEY, 'persistent');
    } else {
        setSessionMarker();
        localStorage.setItem(AUTH_PERSISTENCE_KEY, 'session');
    }

    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.setItem(TOKEN_KEY, token);

    notifyAuthChanged();
};

export const getAuthToken = (): string | null => {
    if (typeof window === 'undefined') return null;

    const token = localStorage.getItem(TOKEN_KEY);
    const persistence = localStorage.getItem(AUTH_PERSISTENCE_KEY);

    if (!token || (persistence !== 'session' && persistence !== 'persistent')) {
        clearAuthStorage();
        return null;
    }

    if (persistence === 'session' && !hasSessionMarker()) {
        clearAuthStorage();
        return null;
    }

    return token;
};

export const decodeJwt = (token: string): JwtPayload | null => {
    try {
        const payloadBase64 = token.split('.')[1];

        if (!payloadBase64) {
            return null;
        }

        const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
        const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');

        const decodedJson = decodeURIComponent(
            atob(paddedBase64).split('').map(char => '%' + ('00' + char.charCodeAt(0).toString(16)).slice(-2)).join('')
        );

        const payload: unknown = JSON.parse(decodedJson);

        if (typeof payload !== 'object' || payload === null) {
            return null;
        }

        return payload as JwtPayload;
    } catch (error) {
        console.error('Błąd dekodowania tokenu JWT:', error);
        return null;
    }
};

export const isJwtValid = (token: string | null): boolean => {
    if (!token) return false;

    const payload = decodeJwt(token);

    if (!payload) return false;

    if (typeof payload.exp !== 'number' || !Number.isFinite(payload.exp)) {
        return false;
    }

    return payload.exp * 1000 > Date.now();
};

export const logout = () => {
    clearAuthStorage(true);
};