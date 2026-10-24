"use client";

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import { useAuth } from '@/app/components/AuthProvider';

export default function AdminGuard({children}: { children: React.ReactNode; }) {
    const { user, isLoggedIn, isAuthResolved, authCheckFailed, refreshAuth } = useAuth();
    const router = useRouter();

    const isAdministrator = user?.role === 'ADMINISTRATOR';

    useEffect(() => {
        if (!isAuthResolved) return;

        if (!isLoggedIn) {
            if (!authCheckFailed) router.replace('/login');
            return;
        }

        if (!isAdministrator) {
            router.replace('/');
        }
    }, [isAdministrator, isAuthResolved, isLoggedIn, authCheckFailed, router]);

    if (isAuthResolved && !isLoggedIn && authCheckFailed) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
                <div className="w-full max-w-lg">
                    <ErrorState
                        message="Nie udało się sprawdzić sesji. Sprawdź połączenie z serwerem i spróbuj ponownie."
                        onRetry={() => { void refreshAuth(); }}
                    />
                </div>
            </div>
        );
    }

    if (!isAuthResolved || !isLoggedIn || !isAdministrator) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <LoadingState />
            </div>
        );
    }

    return <>{children}</>;
}