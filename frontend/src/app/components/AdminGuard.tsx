"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, getUserRole, isJwtValid, logout } from '@/app/utils/jwt';
import LoadingState from '@/app/components/LoadingState';

export default function AdminGuard({ children }: { children: React.ReactNode }) {
    const [isAuthorized, setIsAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const token = getAuthToken();

        if (!isJwtValid(token)) {
            logout();
            router.replace('/login');
            return;
        }

        if (getUserRole(token) !== 'ADMINISTRATOR') {
            router.replace('/');
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