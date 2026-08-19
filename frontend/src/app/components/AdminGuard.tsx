"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, decodeJwt } from '@/utils/jwt';

export default function AdminGuard({ children }: { children: React.ReactNode }) {
    const [isAuthorized, setIsAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const token = getAuthToken();

        if (!token) {
            router.push('/login');
            return;
        }

        const payload = decodeJwt(token);

        if (payload && payload.role === 'ADMINISTRATOR') {
            setIsAuthorized(true);
        } else {
            console.error('Brak uprawnień lub błąd weryfikacji tokenu');
            router.push('/');
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