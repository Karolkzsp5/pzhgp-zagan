'use client';

import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/app/components/AuthProvider';
import { authService } from '@/app/services/authService';
import { getErrorMessage, refreshCsrfToken } from '@/app/utils/apiClient';
import { invalidateAuthSession } from '@/app/utils/authSession';
import { safeReturnPath } from '@/app/utils/loginRedirect';

interface LoginFormData {
    email: string;
    password: string;
}

interface MessageState {
    text: string;
    type: 'success' | 'error' | 'info' | '';
}

/** Komunikaty dla parametru notice, z którym strona ustawień przekierowuje na logowanie. */
const LOGIN_NOTICES: Record<string, string> = {
    'password-changed': 'Hasło zostało zmienione. Zaloguj się nowym hasłem.',
    'logged-out-everywhere': 'Wylogowano Cię ze wszystkich urządzeń. Zaloguj się ponownie.'
};

export default function LoginPage() {
    const [isLoading, setIsLoading] = useState(false);
    const [sessionExpired, setSessionExpired] = useState(false);
    const [notice, setNotice] = useState<string | null>(null);
    const [formData, setFormData] = useState<LoginFormData>({ email: '', password: '' });
    const [rememberMe, setRememberMe] = useState(false);
    const [message, setMessage] = useState<MessageState>({ text: '', type: '' });
    const router = useRouter();
    const { refreshAuth } = useAuth();

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        setSessionExpired(params.get('expired') === 'true');
        setNotice(LOGIN_NOTICES[params.get('notice') ?? ''] ?? null);
    }, []);

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });

        if (formData.password.length > 72 || new TextEncoder().encode(formData.password).length > 72) {
            setMessage({ text: 'Hasło może mieć maksymalnie 72 bajty (UTF-8).', type: 'error' });
            return;
        }

        setIsLoading(true);

        try {
            await authService.login({ ...formData, rememberMe });

            // Backend ustawia JWT w HttpOnly cookie (odpowiedź 204, bez tokenu).
            invalidateAuthSession(false);
            const isAuthenticated = await refreshAuth();
            if (!isAuthenticated) {
                setMessage({ text: 'Nie udało się potwierdzić sesji. Sprawdź konfigurację ciasteczek.', type: 'error' });
                setIsLoading(false);
                return;
            }
            // Spring Security może wyczyścić wcześniejszy token CSRF po logowaniu.
            try {
                await refreshCsrfToken();
            } catch (csrfError) {
                console.error('Błąd odświeżenia CSRF po logowaniu:', csrfError);
            }
            router.push(safeReturnPath(new URLSearchParams(window.location.search).get('next')));
        } catch (error) {
            setMessage({ text: getErrorMessage(error, 'Wystąpił błąd podczas logowania.'), type: 'error' });
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Logowanie Hodowcy</h1>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label htmlFor="email" className="block text-sm font-medium text-gray-700">Adres E-mail</label>
                        <input
                            id="email"
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            autoComplete="email"
                            required
                            disabled={isLoading}
                            className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                        />
                    </div>

                    <div>
                        <label htmlFor="password" className="block text-sm font-medium text-gray-700">Hasło</label>
                        <input
                            id="password"
                            type="password"
                            name="password"
                            autoComplete="current-password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                            disabled={isLoading}
                            className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                        />
                    </div>

                    <div className="flex items-center justify-between mt-2">
                        <div className="flex items-center">
                            <input
                                id="remember-me"
                                name="remember-me"
                                type="checkbox"
                                checked={rememberMe}
                                onChange={(e) => setRememberMe(e.target.checked)}
                                disabled={isLoading}
                                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded cursor-pointer disabled:cursor-not-allowed"
                            />
                            <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700 cursor-pointer select-none">
                                Zapamiętaj mnie
                            </label>
                        </div>
                    </div>

                    {message.text && (
                        <div className={`p-4 my-4 rounded transition-all duration-300 text-sm ${
                            message.type === 'success' ? 'bg-green-100 text-green-700' :
                                message.type === 'error' ? 'bg-red-100 text-red-700' : 'bg-blue-100 text-blue-700'
                        }`}>
                            {message.text}
                        </div>
                    )}

                    {notice && !message.text && (
                        <div role="status" className="p-4 mb-4 rounded bg-green-100 text-green-700 text-sm">
                            {notice}
                        </div>
                    )}

                    {sessionExpired && !message.text && (
                        <div role="status" className="p-4 mb-4 rounded bg-red-100 text-red-700 text-sm">
                            Sesja wygasła. Zaloguj się ponownie.
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full mt-6 bg-blue-600 text-white font-bold py-2 px-4 rounded hover:bg-blue-700 transition duration-200 disabled:bg-blue-400 disabled:cursor-wait flex justify-center items-center"
                    >
                        {isLoading ? 'Logowanie...' : 'Zaloguj się'}
                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-600 border-t pt-4 space-y-2">
                    <p>
                        Nie pamiętasz hasła?{' '}
                        <Link href="/forgot-password" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                            Odzyskaj hasło
                        </Link>
                    </p>
                    <p>
                        Nie masz jeszcze konta?{' '}
                        <Link href="/register" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                            Zarejestruj się tutaj
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}