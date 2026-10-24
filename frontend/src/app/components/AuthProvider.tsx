"use client";

import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { API_URL, fetchApi, fetchWithAuth, readApiError, refreshCsrfToken } from '@/app/utils/apiClient';
import { AUTH_CHANGED_EVENT, AUTH_SYNC_KEY, clearLegacyAuthStorage, CurrentUser, getAuthGeneration, invalidateAuthSession } from '@/app/utils/authSession';

interface AuthContextValue {
    user: CurrentUser | null;
    isLoggedIn: boolean;
    isAuthResolved: boolean;
    authCheckFailed: boolean;
    refreshAuth: () => Promise<boolean>;
    signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export default function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<CurrentUser | null>(null);
    const [isAuthResolved, setIsAuthResolved] = useState(false);
    const [authCheckFailed, setAuthCheckFailed] = useState(false);
    const latestAuthRequestRef = useRef(0);

    const refreshAuth = useCallback(async (): Promise<boolean> => {
        const requestId = ++latestAuthRequestRef.current;
        const generation = getAuthGeneration();

        try {
            const response = await fetchApi(`${API_URL}/api/auth/me`, { cache: 'no-store' });
            if (requestId !== latestAuthRequestRef.current || generation !== getAuthGeneration()) return false;

            if (response.status === 401) {
                setUser(null);
                setAuthCheckFailed(false);
                setIsAuthResolved(true);
                return false;
            }
            if (!response.ok) {
                throw new Error(await readApiError(response, 'Nie udało się pobrać danych użytkownika.'));
            }

            const currentUser = await response.json() as CurrentUser;
            if (requestId !== latestAuthRequestRef.current || generation !== getAuthGeneration()) return false;
            setUser(currentUser);
            setAuthCheckFailed(false);
            setIsAuthResolved(true);
            return true;
        } catch (error) {
            if (requestId === latestAuthRequestRef.current && generation === getAuthGeneration()) {
                // Awaria sieci nie oznacza wylogowania — zachowujemy dotychczasowy stan.
                console.error('Błąd sprawdzania sesji:', error);
                setAuthCheckFailed(true);
                setIsAuthResolved(true);
            }
            return false;
        }
    }, []);

    const signOut = useCallback(async () => {
        const response = await fetchWithAuth(`${API_URL}/api/auth/logout`, { method: 'POST' });
        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się wylogować.'));
        }
        invalidateAuthSession(false);
        latestAuthRequestRef.current++;
        setUser(null);
        setAuthCheckFailed(false);
        setIsAuthResolved(true);

        try {
            await refreshCsrfToken();
        } catch (error) {
            console.error('Błąd odświeżenia tokenu CSRF po wylogowaniu:', error);
        }
    }, []);

    useEffect(() => {
        clearLegacyAuthStorage();
        void refreshAuth();

        const onStorage = (event: StorageEvent) => {
            if (event.storageArea === localStorage && (event.key === AUTH_SYNC_KEY || event.key === null)) {
                invalidateAuthSession(false, false);
                void refreshAuth();
            }
        };
        const onAuthChanged = () => { void refreshAuth(); };
        const onFocus = () => { void refreshAuth(); };

        window.addEventListener('storage', onStorage);
        window.addEventListener(AUTH_CHANGED_EVENT, onAuthChanged);
        window.addEventListener('focus', onFocus);
        return () => {
            latestAuthRequestRef.current++;
            window.removeEventListener('storage', onStorage);
            window.removeEventListener(AUTH_CHANGED_EVENT, onAuthChanged);
            window.removeEventListener('focus', onFocus);
        };
    }, [refreshAuth]);

    const value = useMemo<AuthContextValue>(() => ({
        user,
        isLoggedIn: user !== null,
        isAuthResolved,
        authCheckFailed,
        refreshAuth,
        signOut
    }), [user, isAuthResolved, authCheckFailed, refreshAuth, signOut]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = (): AuthContextValue => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth musi być używany wewnątrz AuthProvider.');
    return context;
};