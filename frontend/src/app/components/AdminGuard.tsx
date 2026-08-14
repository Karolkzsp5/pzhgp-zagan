"use client";

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function AdminGuard({ children }: { children: React.ReactNode }) {
    const [isAuthorized, setIsAuthorized] = useState(false);
    const router = useRouter();

    useEffect(() => {
        const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');

        if (!token) {
            router.push('/login');
            return;
        }

        try {
            const payloadBase64 = token.split('.')[1];
            const decodedJson = atob(payloadBase64);
            const payload = JSON.parse(decodedJson);

            if (payload.role === 'ADMINISTRATOR') {
                setIsAuthorized(true);
            } else {
                router.push('/');
            }
        } catch (error) {
            console.error('Błąd weryfikacji uprawnień:', error);
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