import { ForgotPasswordRequest, LoginRequest, RegistrationRequest, ResetPasswordRequest } from '@/app/types/auth';
import { API_URL, ensureOk, fetchApi, requestVoid } from '@/app/utils/apiClient';
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
    login: (request: LoginRequest): Promise<void> =>
        requestVoid('/api/auth/login', 'Nie udało się zalogować.', { method: 'POST', body: request, isPublic: true }),

    register: (request: RegistrationRequest): Promise<void> =>
        requestVoid('/api/auth/register', 'Nie udało się zarejestrować konta.', { method: 'POST', body: request, isPublic: true }),

    /** Prosi o wysłanie linku do zmiany hasła. Backend odpowiada tak samo, gdy konto z podanym adresem nie istnieje. */
    requestPasswordReset: (request: ForgotPasswordRequest): Promise<void> =>
        requestVoid('/api/auth/forgot-password', 'Nie udało się wysłać prośby o zmianę hasła.', { method: 'POST', body: request, isPublic: true }),

    /** Ustawia nowe hasło na podstawie jednorazowego tokenu z linku w wiadomości e-mail. */
    resetPassword: (request: ResetPasswordRequest): Promise<void> =>
        requestVoid('/api/auth/reset-password', 'Nie udało się zmienić hasła.', { method: 'POST', body: request, isPublic: true }),

    logout: (): Promise<void> =>
        requestVoid('/api/auth/logout', 'Nie udało się wylogować.', { method: 'POST' })
};
