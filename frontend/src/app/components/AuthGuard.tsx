"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, isJwtValid, logout } from '@/app/utils/jwt';
import LoadingState from '@/app/components/LoadingState';

/**
 * Osłona tras dostępnych wyłącznie dla zalogowanych użytkowników.
 * Przy nieważnym tokenie wylogowuje użytkownika i przekierowuje na stronę logowania.
 */
export default function AuthGuard({ children }: { children: React.ReactNode }) {
    const [isAuthorized, setIsAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const token = getAuthToken();

        if (!isJwtValid(token)) {
            logout();
            router.replace('/login');
            return;
        }

        setIsAuthorized(true);
    }, [router]);

    if (!isAuthorized) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <LoadingState />
            </div>
        );
    }

    return <>{children}</>;
}