import { ForgotPasswordRequest, LoginRequest, RegistrationRequest, ResetPasswordRequest } from '@/app/types/auth';
import { API_URL, ensureOk, fetchApi, fetchWithAuth } from '@/app/utils/apiClient';
import { CurrentUser } from '@/app/utils/authSession';

export const authService = {
    /** Dane zalogowanego użytkownika albo null, gdy nikt nie jest zalogowany. */
    getCurrentUser: async (): Promise<CurrentUser | null> => {
        const response = await fetchApi(`${API_URL}/api/auth/me`, { cache: 'no-store' });
        if (response.status === 401) return null;
        await ensureOk(response, 'Nie udało się pobrać danych użytkownika.');
        return response.json();
    },

    /** Backend odpowiada 204 i ustawia JWT w ciasteczku HttpOnly; token nie trafia do JavaScriptu. */
    login: async (request: LoginRequest): Promise<void> => {
        const response = await fetchApi(`${API_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        await ensureOk(response, 'Nie udało się zalogować.');
    },

    register: async (request: RegistrationRequest): Promise<void> => {
        const response = await fetchApi(`${API_URL}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        await ensureOk(response, 'Nie udało się zarejestrować konta.');
    },

    /** Prosi o wysłanie linku do zmiany hasła. Backend odpowiada tak samo, gdy konto z podanym adresem nie istnieje. */
    requestPasswordReset: async (request: ForgotPasswordRequest): Promise<void> => {
        const response = await fetchApi(`${API_URL}/api/auth/forgot-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        await ensureOk(response, 'Nie udało się wysłać prośby o zmianę hasła.');
    },

    /** Ustawia nowe hasło na podstawie jednorazowego tokenu z linku w wiadomości e-mail. */
    resetPassword: async (request: ResetPasswordRequest): Promise<void> => {
        const response = await fetchApi(`${API_URL}/api/auth/reset-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        await ensureOk(response, 'Nie udało się zmienić hasła.');
    },

    logout: async (): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/auth/logout`, { method: 'POST' });
        await ensureOk(response, 'Nie udało się wylogować.');
    }
};
