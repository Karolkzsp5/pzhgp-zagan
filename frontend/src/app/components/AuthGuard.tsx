"use client";

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import { useAuth } from '@/app/components/AuthProvider';

export default function AuthGuard({children}: { children: React.ReactNode; }) {
    const { isLoggedIn, isAuthResolved, authCheckFailed, refreshAuth } = useAuth();
    const router = useRouter();

    useEffect(() => {
        if (isAuthResolved && !isLoggedIn && !authCheckFailed) {
            router.replace('/login');
        }
    }, [isAuthResolved, isLoggedIn, authCheckFailed, router]);

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

    if (!isAuthResolved || !isLoggedIn) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <LoadingState />
            </div>
        );
    }

    return <>{children}</>;
}