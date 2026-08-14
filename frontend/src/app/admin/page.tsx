"use client";

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import AdminGuard from '../components/AdminGuard';
import Navbar from '../components/Navbar';

interface PendingBreeder {
    id: number;
    name: string;
    surname: string;
    email: string;
    phoneNumber: string;
    dateOfBirth: string;
    city: string;
    sectionId: number;
    status: string;
    createdAt: string;
}

const sectionNames: Record<number, string> = {
    1: 'Żagań',
    2: 'Wymiarki',
    3: 'Chotków',
    4: 'Kożuchów'
};

export default function AdminPanelPage() {
    const [pendingBreeders, setPendingBreeders] = useState<PendingBreeder[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const router = useRouter();

    useEffect(() => {
        fetchPendingAccounts();
    }, []);

    const getToken = () => {
        return localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
    };

    const fetchPendingAccounts = async () => {
        const token = getToken();
        if (!token) {
            router.push('/login');
            return;
        }

        try {
            const response = await fetch('http://localhost:8080/api/admin/pending', {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.ok) {
                const data = await response.json();
                setPendingBreeders(data);
            } else if (response.status === 401 || response.status === 403) {
                setError('Brak uprawnień dostępu. Zaloguj się jako administrator.');
            } else {
                setError('Wystąpił błąd podczas pobierania danych.');
            }
        } catch (err) {
            setError('Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleAction = async (id: number, action: 'approve' | 'reject') => {
        const token = getToken();
        if (!token) return;

        const method = action === 'approve' ? 'PUT' : 'DELETE';
        const url = `http://localhost:8080/api/admin/${action}/${id}`;

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                setPendingBreeders(prev => prev.filter(breeder => breeder.id !== id));
            } else {
                const errorData = await response.text();
                alert(`Błąd: ${errorData}`);
            }
        } catch (err) {
            alert('Błąd połączenia z serwerem podczas wykonywania akcji.');
        }
    };

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
            </div>
        );
    }

    return (
        <AdminGuard>
            <Navbar />
            <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
                <div className="max-w-7xl mx-auto">
                    <div className="flex justify-between items-center mb-8">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Panel Administratora</h1>
                            <p className="mt-2 text-sm text-gray-600">Zarządzanie kontami użytkowników.</p>
                        </div>
                    </div>

                    {error ? (
                        <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded mb-6">
                            <div className="flex">
                                <div className="flex-shrink-0">
                                    <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                                    </svg>
                                </div>
                                <div className="ml-3">
                                    <p className="text-sm text-red-700">{error}</p>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <div className="bg-white shadow overflow-hidden sm:rounded-lg border border-gray-200">
                            {pendingBreeders.length === 0 ? (
                                <div className="p-8 text-center text-gray-500">
                                    <svg className="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                    Brak kont oczekujących na akceptację.
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-blue-100">
                                        <tr>
                                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Hodowca</th>
                                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Kontakt</th>
                                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Lokalizacja / Sekcja</th>
                                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Data Rejestracji</th>
                                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Akcje</th>
                                        </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                        {pendingBreeders.map((breeder) => (
                                            <tr key={breeder.id} className="bg-white even:bg-slate-50 transition duration-150">
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    <div className="text-sm font-medium text-gray-900">{breeder.name} {breeder.surname}</div>
                                                    <div className="text-sm text-gray-500">Data ur: {breeder.dateOfBirth}</div>
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    <div className="text-sm text-gray-900">{breeder.email}</div>
                                                    <div className="text-sm text-gray-500">Tel: {breeder.phoneNumber}</div>
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap">
                                                    <div className="text-sm text-gray-900">{breeder.city}</div>
                                                    <div className="text-sm text-gray-500">Sekcja: {sectionNames[breeder.sectionId] || 'Nieznana'}</div>
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                    {new Date(breeder.createdAt).toLocaleDateString('pl-PL')}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-left text-sm font-medium">
                                                    <button
                                                        onClick={() => handleAction(breeder.id, 'approve')}
                                                        className="text-green-600 hover:text-green-900 bg-green-50 hover:bg-green-100 px-3 py-1 rounded-md transition mr-3"
                                                    >
                                                        Akceptuj
                                                    </button>
                                                    <button
                                                        onClick={() => {
                                                            if(window.confirm('Czy na pewno chcesz odrzucić i usunąć to konto?')) {
                                                                handleAction(breeder.id, 'reject');
                                                            }
                                                        }}
                                                        className="text-red-600 hover:text-red-900 bg-red-50 hover:bg-red-100 px-3 py-1 rounded-md transition"
                                                    >
                                                        Odrzuć
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </AdminGuard>
    );
}