"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, decodeJwt, isJwtValid } from '@/utils/jwt';

export default function AdminGuard({ children }: { children: React.ReactNode }) {
    const [isAuthorized, setIsAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const token = getAuthToken();

        if (!token) {
            router.push('/login');
            return;
        }

        if (isJwtValid(token)) {
            const payload = decodeJwt(token);
            if (payload?.role === 'ADMINISTRATOR') {
                setIsAuthorized(true);
            } else {
                console.error('Brak uprawnień administratora');
                router.push('/');
            }
        } else {
            console.error('Wygasł token sesji');
            localStorage.removeItem('jwt_token');
            sessionStorage.removeItem('jwt_token');
            router.push('/login');
        }
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