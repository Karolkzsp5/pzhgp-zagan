"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, getUserRole, isJwtValid, logout } from '@/app/utils/jwt';

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
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
            </div>
        );
    }

    return <>{children}</>;
}