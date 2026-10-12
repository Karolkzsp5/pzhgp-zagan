"use client";

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import LoadingState from '@/app/components/LoadingState';
import { useAuth } from '@/app/components/AuthProvider';

export default function AdminGuard({children}: { children: React.ReactNode; }) {
    const { user, isLoggedIn, isAuthResolved } = useAuth();
    const router = useRouter();

    const isAdministrator = user?.role === 'ADMINISTRATOR';

    useEffect(() => {
        if (!isAuthResolved) return;

        if (!isLoggedIn) {
            router.replace('/login');
            return;
        }

        if (!isAdministrator) {
            router.replace('/');
        }
    }, [isAdministrator, isAuthResolved, isLoggedIn, router]);

    if (!isAuthResolved || !isLoggedIn || !isAdministrator) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <LoadingState />
            </div>
        );
    }

    return <>{children}</>;
}