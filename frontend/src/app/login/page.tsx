"use client";

import { useState, ChangeEvent, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

interface LoginFormData {
    email: string;
    password: string;
}

interface MessageState {
    text: string;
    type: 'success' | 'error' | 'info' | '';
}

export default function LoginPage() {
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const router = useRouter();

    const [formData, setFormData] = useState<LoginFormData>({
        email: '',
        password: ''
    });

    const [rememberMe, setRememberMe] = useState<boolean>(false);
    const [message, setMessage] = useState<MessageState>({ text: '', type: '' });

    const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setMessage({ text: '', type: '' });
        setIsLoading(true);

        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });

            const data = await response.text();

            if (response.ok) {
                if (rememberMe) {
                    localStorage.setItem('jwt_token', data);
                    sessionStorage.removeItem('jwt_token');
                } else {
                    sessionStorage.setItem('jwt_token', data);
                    localStorage.removeItem('jwt_token');
                }

                router.push('/');

            } else {
                setMessage({ text: data, type: 'error' });
                setIsLoading(false);
            }
        } catch (error) {
            setMessage({ text: 'Błąd połączenia z serwerem.', type: 'error' });
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Logowanie Hodowcy</h1>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700">Adres E-mail</label>
                        <input
                            type="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                            className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700">Hasło</label>
                        <input
                            type="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                            className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
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
                                message.type === 'error' ? 'bg-red-100 text-red-700' :
                                    'bg-blue-100 text-blue-700'
                        }`}>
                            {message.text}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full mt-6 bg-blue-600 text-white font-bold py-2 px-4 rounded hover:bg-blue-700 transition duration-200 disabled:bg-blue-400 disabled:cursor-wait flex justify-center items-center"
                    >
                        Zaloguj się
                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-600 border-t pt-4">
                    Nie masz jeszcze konta?{' '}
                    <Link href="/register" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                        Zarejestruj się tutaj
                    </Link>
                </div>
            </div>
        </div>
    );
}