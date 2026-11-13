'use client';

import { FormEvent, useRef, useState } from 'react';
import Link from 'next/link';
import TurnstileWidget, { TurnstileWidgetHandle } from '@/app/components/TurnstileWidget';
import { authService } from '@/app/services/authService';
import { getErrorMessage } from '@/app/utils/apiClient';

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [isRequestSent, setIsRequestSent] = useState(false);
    const [captchaToken, setCaptchaToken] = useState<string | null>(null);
    const captchaRef = useRef<TurnstileWidgetHandle>(null);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        if (!captchaToken) {
            setError('Dokończ weryfikację CAPTCHA przed wysłaniem formularza.');
            return;
        }

        setIsLoading(true);
        try {
            await authService.requestPasswordReset({ email: email.trim(), captchaToken });
            setIsRequestSent(true);
        } catch (error) {
            setError(getErrorMessage(error, 'Nie udało się wysłać prośby o zmianę hasła.'));
            // Token CAPTCHA jest jednorazowy, więc kolejna próba wymaga nowego.
            captchaRef.current?.reset();
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Odzyskiwanie hasła</h1>

                {isRequestSent ? (
                    // Komunikat jest taki sam dla każdego adresu, żeby nie zdradzać, które adresy są zarejestrowane.
                    <div role="status" className="p-4 rounded bg-green-100 text-green-700 text-sm leading-relaxed">
                        Jeśli konto z podanym adresem istnieje, wysłaliśmy na nie wiadomość z linkiem do zmiany hasła.
                        Link jest ważny 30 minut. Jeśli nie widzisz wiadomości, sprawdź folder ze spamem.
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <p className="text-sm text-gray-600 leading-relaxed">
                            Podaj adres e-mail przypisany do konta. Wyślemy na niego link do ustawienia nowego hasła.
                        </p>

                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-700">Adres e-mail</label>
                            <input
                                id="email"
                                type="email"
                                name="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                autoComplete="email"
                                required
                                maxLength={320}
                                disabled={isLoading}
                                className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                            />
                        </div>

                        <TurnstileWidget ref={captchaRef} onTokenChange={setCaptchaToken} />

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
                            {isLoading ? 'Wysyłanie...' : 'Wyślij link'}
                        </button>
                    </form>
                )}

                <div className="mt-6 text-center text-sm text-gray-600 border-t pt-4">
                    Pamiętasz hasło?{' '}
                    <Link href="/login" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                        Zaloguj się tutaj
                    </Link>
                </div>
            </div>
        </div>
    );
}