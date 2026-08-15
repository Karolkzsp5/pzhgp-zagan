"use client";

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import AdminGuard from '../components/AdminGuard';
import Navbar from "@/app/components/Navbar";

interface BreederDto {
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
    const [pendingBreeders, setPendingBreeders] = useState<BreederDto[]>([]);
    const [activeBreeders, setActiveBreeders] = useState<BreederDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const router = useRouter();

    useEffect(() => {
        fetchAllAccounts();
    }, []);

    const getToken = () => {
        return localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
    };

    const fetchAllAccounts = async () => {
        const token = getToken();
        if (!token) {
            router.push('/login');
            return;
        }

        try {
            const [pendingRes, activeRes] = await Promise.all([
                fetch('http://localhost:8080/api/admin/pending', {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                }),
                fetch('http://localhost:8080/api/admin/active', {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                })
            ]);

            if (pendingRes.ok && activeRes.ok) {
                const pendingData = await pendingRes.json();
                const activeData = await activeRes.json();
                setPendingBreeders(pendingData);
                setActiveBreeders(activeData);
            } else if (pendingRes.status === 401 || pendingRes.status === 403) {
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

    const handleAction = async (id: number, action: 'approve' | 'reject' | 'block') => {
        const token = getToken();
        if (!token) return;

        let method = 'PUT';
        if (action === 'reject') method = 'DELETE';

        const url = `http://localhost:8080/api/admin/${action}/${id}`;

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                if (action === 'approve') {
                    const approvedBreeder = pendingBreeders.find(b => b.id === id);
                    if (approvedBreeder) {
                        setPendingBreeders(prev => prev.filter(b => b.id !== id));
                        setActiveBreeders(prev => [...prev, { ...approvedBreeder, status: 'ACTIVE' }]);
                    }
                } else if (action === 'reject') {
                    setPendingBreeders(prev => prev.filter(b => b.id !== id));
                } else if (action === 'block') {
                    setActiveBreeders(prev => prev.filter(b => b.id !== id));
                }
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
                <div className="max-w-7xl mx-auto space-y-12">

                    <div className="flex justify-between items-center">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Panel Administratora</h1>
                            <p className="mt-2 text-sm text-gray-600">Zarządzanie kontami hodowców.</p>
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
                        <>
                            <section>
                                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Konta czekające na akceptację</h2>
                                <div className="bg-white shadow overflow-hidden sm:rounded-lg border border-gray-200">
                                    {pendingBreeders.length === 0 ? (
                                        <div className="p-8 text-center text-gray-500">
                                            Brak kont oczekujących na akceptację.
                                        </div>
                                    ) : (
                                        <div className="overflow-x-auto">
                                            <table className="min-w-full divide-y divide-gray-200">
                                                <thead className="bg-blue-100">
                                                <tr>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Hodowca</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Kontakt</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Lokalizacja / Sekcja</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Data Rejestracji</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Akcje</th>
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
                            </section>

                            <section>
                                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Aktywne konta</h2>
                                <div className="bg-white shadow overflow-hidden sm:rounded-lg border border-gray-200">
                                    {activeBreeders.length === 0 ? (
                                        <div className="p-8 text-center text-gray-500">
                                            Brak aktywnych kont w systemie.
                                        </div>
                                    ) : (
                                        <div className="overflow-x-auto">
                                            <table className="min-w-full divide-y divide-gray-200">
                                                <thead className="bg-blue-100">
                                                <tr>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Hodowca</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Kontakt</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Lokalizacja / Sekcja</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Data Rejestracji</th>
                                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Akcje</th>
                                                </tr>
                                                </thead>
                                                <tbody className="bg-white divide-y divide-gray-200">
                                                {activeBreeders.map((breeder) => (
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
                                                                onClick={() => {
                                                                    if(window.confirm('Czy na pewno chcesz zablokować tego użytkownika? Straci on możliwość logowania do systemu.')) {
                                                                        handleAction(breeder.id, 'block');
                                                                    }
                                                                }}
                                                                className="text-orange-600 hover:text-orange-900 bg-orange-50 hover:bg-orange-100 px-3 py-1 rounded-md transition"
                                                            >
                                                                Zablokuj
                                                            </button>
                                                        </td>
                                                    </tr>
                                                ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </div>
                            </section>
                        </>
                    )}
                </div>
            </div>
        </AdminGuard>
    );
}