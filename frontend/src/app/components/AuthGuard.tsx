"use client";

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import LoadingState from '@/app/components/LoadingState';
import { useAuth } from '@/app/components/AuthProvider';

export default function AuthGuard({children}: { children: React.ReactNode; }) {
    const { isLoggedIn, isAuthResolved } = useAuth();
    const router = useRouter();

    useEffect(() => {
        if (isAuthResolved && !isLoggedIn) {
            router.replace('/login');
        }
    }, [isAuthResolved, isLoggedIn, router]);

    if (!isAuthResolved || !isLoggedIn) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <LoadingState />
            </div>
        );
    }

    return <>{children}</>;
}