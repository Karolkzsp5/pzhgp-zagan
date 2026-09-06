"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {getAuthToken, isJwtValid} from '@/utils/jwt';

export default function ForumGuard({ children }: { children: React.ReactNode }) {
    const [isAuthorized, setIsAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const token = getAuthToken();

        if (!isJwtValid(token)) {
            localStorage.removeItem('jwt_token');
            sessionStorage.removeItem('jwt_token');
            router.push('/');
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