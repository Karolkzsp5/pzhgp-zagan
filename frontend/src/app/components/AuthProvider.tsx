"use client";

import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { AUTH_CHANGED_EVENT, decodeJwt, getAuthToken, isJwtValid, JwtPayload, logout } from '@/app/utils/jwt';

interface AuthContextValue {
    user: JwtPayload | null;
    isLoggedIn: boolean;
    isAuthResolved: boolean;
    refreshAuth: () => void;
    signOut: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export default function AuthProvider({children}: { children: ReactNode; }) {
    const [user, setUser] = useState<JwtPayload | null>(null);
    const [isAuthResolved, setIsAuthResolved] = useState(false);

    const refreshAuth = useCallback(() => {
        const token = getAuthToken();

        if (!isJwtValid(token)) {
            if (token) {
                logout();
            }

            setUser(null);
            setIsAuthResolved(true);
            return;
        }

        setUser(decodeJwt(token!));
        setIsAuthResolved(true);
    }, []);

    const signOut = useCallback(() => {
        logout();
        setUser(null);
        setIsAuthResolved(true);
    }, []);

    useEffect(() => {
        refreshAuth();

        const handleStorage = (event: StorageEvent) => {

            if (event.storageArea === localStorage && (event.key === 'jwt_token' || event.key === null)) {
                refreshAuth();
            }
        };

        const handleAuthChanged = () => {
            refreshAuth();
        };

        const handleWindowFocus = () => {
            refreshAuth();
        };

        window.addEventListener('storage', handleStorage);
        window.addEventListener(AUTH_CHANGED_EVENT, handleAuthChanged);
        window.addEventListener('focus', handleWindowFocus);

        return () => {
            window.removeEventListener('storage', handleStorage);
            window.removeEventListener(AUTH_CHANGED_EVENT, handleAuthChanged);
            window.removeEventListener('focus', handleWindowFocus);
        };
    }, [refreshAuth]);

    const value = useMemo<AuthContextValue>(() => ({
        user,
        isLoggedIn: user !== null,
        isAuthResolved,
        refreshAuth,
        signOut
    }), [user, isAuthResolved, refreshAuth, signOut]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = (): AuthContextValue => {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error('useAuth musi być używany wewnątrz AuthProvider.');
    }

    return context;
};