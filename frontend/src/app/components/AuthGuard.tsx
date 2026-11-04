'use client';

import { ReactNode, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import { useAuth } from '@/app/components/AuthProvider';
import { UserRole } from '@/app/utils/authSession';

interface AuthGuardProps {
    children: ReactNode;
    /** Rola wymagana na podstronie. Zalogowany użytkownik bez tej roli trafia na stronę główną. */
    requiredRole?: UserRole;
}

export default function AuthGuard({ children, requiredRole }: AuthGuardProps) {
    const { user, isLoggedIn, isAuthResolved, authCheckFailed, refreshAuth } = useAuth();
    const router = useRouter();

    const hasRequiredRole = !requiredRole || user?.role === requiredRole;

    useEffect(() => {
        if (!isAuthResolved) return;

        if (!isLoggedIn) {
            if (!authCheckFailed) router.replace('/login');
            return;
        }

        if (!hasRequiredRole) {
            router.replace('/');
        }
    }, [hasRequiredRole, isAuthResolved, isLoggedIn, authCheckFailed, router]);

    if (isAuthResolved && !isLoggedIn && authCheckFailed) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
                <div className="w-full max-w-lg">
                    <ErrorState
                        message="Nie udało się sprawdzić sesji. Sprawdź połączenie z serwerem i spróbuj ponownie."
                        onRetry={() => {
                            void refreshAuth();
                        }}
                    />
                </div>
            </div>
        );
    }

    if (!isAuthResolved || !isLoggedIn || !hasRequiredRole) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <LoadingState />
            </div>
        );
    }

    return <>{children}</>;
}
