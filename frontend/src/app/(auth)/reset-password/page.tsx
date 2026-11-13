'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import PasswordStrengthMeter from '@/app/components/PasswordStrengthMeter';
import { authService } from '@/app/services/authService';
import { getErrorMessage } from '@/app/utils/apiClient';
import { validateStrongPassword } from '@/app/utils/password';

export default function ResetPasswordPage() {
    // Token z linku w wiadomości e-mail. undefined: adres strony nie został jeszcze odczytany, null: link nie ma tokenu.
    const [token, setToken] = useState<string | null | undefined>(undefined);
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isPasswordChanged, setIsPasswordChanged] = useState(false);

    useEffect(() => {
        setToken(new URLSearchParams(window.location.search).get('token') || null);
    }, []);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        if (!token) return;

        const passwordError = validateStrongPassword(password);
        if (passwordError) {
            setError(passwordError);
            return;
        }

        if (password !== confirmPassword) {
            setError('Podane hasła nie są identyczne.');
            return;
        }

        setIsLoading(true);
        try {
            await authService.resetPassword({ token, password });
            setIsPasswordChanged(true);
        } catch (error) {
            setError(getErrorMessage(error, 'Nie udało się zmienić hasła.'));
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Ustaw nowe hasło</h1>

                {isPasswordChanged && (
                    <div role="status" className="p-4 rounded bg-green-100 text-green-700 text-sm">
                        Hasło zostało zmienione. Możesz się teraz zalogować.
                    </div>
                )}

                {token === null && (
                    <div role="alert" className="p-4 rounded bg-red-100 text-red-700 text-sm">
                        Link do zmiany hasła jest nieprawidłowy.{' '}
                        <Link href="/forgot-password" className="font-semibold underline">
                            Poproś o nowy link
                        </Link>
                        .
                    </div>
                )}

                {token && !isPasswordChanged && (
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-700">Nowe hasło</label>
                            <input
                                id="password"
                                type="password"
                                name="password"
                                autoComplete="new-password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                                className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                            />

                            <PasswordStrengthMeter password={password} />
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">Powtórz hasło</label>
                            <input
                                id="confirmPassword"
                                type="password"
                                name="confirmPassword"
                                autoComplete="new-password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                                disabled={isLoading}
                                className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                            />
                        </div>

                        {error && (
                            <div className="p-4 my-4 rounded bg-red-100 text-red-700 text-sm" role="alert">
                                {error}
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full mt-6 bg-blue-600 text-white font-bold py-2 px-4 rounded hover:bg-blue-700 transition duration-200 disabled:bg-blue-400 disabled:cursor-wait flex justify-center items-center"
                        >
                            {isLoading ? 'Zapisywanie...' : 'Zmień hasło'}
                        </button>
                    </form>
                )}

                <div className="mt-6 text-center text-sm text-gray-600 border-t pt-4">
                    <Link href="/login" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                        Przejdź do logowania
                    </Link>
                </div>
            </div>
        </div>
    );
}