"use client";

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import AdminGuard from '../components/AdminGuard';
import Navbar from "@/app/components/Navbar";
import BreederDetailsModal, { BreederDto } from '../components/BreederDetailsModal';

const sectionNames: Record<number, string> = {
    1: 'Żagań',
    2: 'Wymiarki',
    3: 'Chotków',
    4: 'Kożuchów'
};

export default function AdminPanelPage() {
    const [pendingBreeders, setPendingBreeders] = useState<BreederDto[]>([]);
    const [registeredBreeders, setRegisteredBreeders] = useState<BreederDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [currentUserEmail, setCurrentUserEmail] = useState('');

    const [openDropdownId, setOpenDropdownId] = useState<number | null>(null);
    const [selectedBreeder, setSelectedBreeder] = useState<BreederDto | null>(null);
    const [roleChangeBreeder, setRoleChangeBreeder] = useState<BreederDto | null>(null);
    const [newRole, setNewRole] = useState<string>('');

    const router = useRouter();

    useEffect(() => {
        fetchAllAccounts();

        const handleClickOutside = () => setOpenDropdownId(null);
        window.addEventListener('click', handleClickOutside);
        return () => window.removeEventListener('click', handleClickOutside);
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
            const payloadBase64 = token.split('.')[1];
            const decodedJson = atob(payloadBase64);
            const payload = JSON.parse(decodedJson);
            setCurrentUserEmail(payload.sub);
        } catch (e) {
            console.error("Błąd dekodowania tokenu", e);
        }

        try {
            const [pendingRes, registeredRes] = await Promise.all([
                fetch('http://localhost:8080/api/admin/pending', {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                }),
                fetch('http://localhost:8080/api/admin/registered', {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                })
            ]);

            if (pendingRes.ok && registeredRes.ok) {
                const pendingData = await pendingRes.json();
                const registeredData = await registeredRes.json();
                setPendingBreeders(pendingData);
                setRegisteredBreeders(registeredData);
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

    const handleAction = async (id: number, action: 'approve' | 'reject' | 'block' | 'unblock') => {
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
                    const approved = pendingBreeders.find(b => b.id === id);
                    if (approved) {
                        setPendingBreeders(prev => prev.filter(b => b.id !== id));
                        setRegisteredBreeders(prev => [...prev, { ...approved, status: 'ACTIVE' }]);
                    }
                } else if (action === 'reject') {
                    setPendingBreeders(prev => prev.filter(b => b.id !== id));
                } else if (action === 'block') {
                    setRegisteredBreeders(prev =>
                        prev.map(b => b.id === id ? { ...b, status: 'BLOCKED' } : b)
                    );
                } else if (action === 'unblock') {
                    setRegisteredBreeders(prev =>
                        prev.map(b => b.id === id ? { ...b, status: 'ACTIVE' } : b)
                    );
                }
            } else {
                const errorData = await response.text();
                alert(`Błąd: ${errorData}`);
            }
        } catch (err) {
            alert('Błąd połączenia z serwerem podczas wykonywania akcji.');
        }
    };

    const submitRoleChange = async () => {
        if (!roleChangeBreeder) return;
        const token = getToken();
        if (!token) return;

        try {
            const response = await fetch(`http://localhost:8080/api/admin/${roleChangeBreeder.id}/role`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ role: newRole })
            });

            if (response.ok) {
                setRegisteredBreeders(prev =>
                    prev.map(b => b.id === roleChangeBreeder.id ? { ...b, role: newRole } : b)
                );
                setRoleChangeBreeder(null);
            } else {
                const errorData = await response.text();
                alert(`Błąd: ${errorData}`);
            }
        } catch (err) {
            alert('Błąd połączenia z serwerem podczas zmiany roli.');
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

                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Panel Administratora</h1>
                        <p className="mt-2 text-sm text-gray-600">Zarządzanie kontami hodowców.</p>
                    </div>

                    {error ? (
                        <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded mb-6">
                            <p className="text-sm text-red-700">{error}</p>
                        </div>
                    ) : (
                        <>
                            {/* Oczekujące konta */}
                            <section>
                                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Konta czekające na akceptację</h2>
                                <div className="bg-white shadow overflow-x-auto sm:rounded-lg border border-gray-200">
                                    {pendingBreeders.length === 0 ? (
                                        <div className="p-8 text-center text-gray-500">
                                            Brak kont oczekujących na akceptację.
                                        </div>
                                    ) : (
                                        <table className="min-w-full divide-y divide-gray-200">
                                            <thead className="bg-blue-100">
                                            <tr>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Hodowca</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Kontakt</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Sekcja</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Data Rejestracji</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Akcje</th>
                                            </tr>
                                            </thead>
                                            <tbody className="bg-white divide-y divide-gray-200">
                                            {pendingBreeders.map((breeder) => (
                                                <tr key={breeder.id} className="bg-white even:bg-slate-50 transition duration-150">
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        <div className="text-sm font-medium text-gray-900">{breeder.name} {breeder.surname}</div>
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        <div className="text-sm text-gray-900">{breeder.email}</div>
                                                        <div className="text-sm text-gray-500">Tel: {breeder.phoneNumber}</div>
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        <div className="text-sm text-gray-500">{sectionNames[breeder.sectionId] || 'Nieznana'}</div>
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
                                    )}
                                </div>
                            </section>

                            {/* Konta hodowców */}
                            <section>
                                <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Konta hodowców</h2>
                                <div className="bg-white shadow overflow-visible sm:rounded-lg border border-gray-200 min-h-[250px]">
                                    {registeredBreeders.length === 0 ? (
                                        <div className="p-8 text-center text-gray-500">
                                            Brak zarejestrowanych kont w systemie.
                                        </div>
                                    ) : (
                                        <table className="min-w-full divide-y divide-gray-200">
                                            <thead className="bg-blue-100">
                                            <tr>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Hodowca</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Kontakt</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Status</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Sekcja</th>
                                                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-600 uppercase tracking-wider">Data Rejestracji</th>
                                                <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-600 uppercase tracking-wider">Akcje</th>
                                            </tr>
                                            </thead>
                                            <tbody className="bg-white divide-y divide-gray-200">
                                            {registeredBreeders.map((breeder) => (
                                                <tr key={breeder.id} className="bg-white even:bg-slate-50 transition duration-150">
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        <div className="text-sm font-medium text-gray-900 flex items-center">
                                                            {breeder.name} {breeder.surname}
                                                            {breeder.role === 'ADMINISTRATOR' && (
                                                                <span className="ml-2 px-2 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800 border border-purple-200">
                                                                    ADMIN
                                                                </span>
                                                            )}
                                                            {breeder.role === 'MODERATOR' && (
                                                                <span className="ml-2 px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
                                                                    MODERATOR
                                                                </span>
                                                            )}
                                                            {breeder.email === currentUserEmail && (
                                                                <span className="ml-2 px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">
                                                                    TO TY
                                                                </span>
                                                            )}
                                                        </div>
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        <div className="text-sm text-gray-900">{breeder.email}</div>
                                                        <div className="text-sm text-gray-500">Tel: {breeder.phoneNumber}</div>
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        {breeder.status === 'ACTIVE' ? (
                                                            <span className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                                                                    Aktywny
                                                                </span>
                                                        ) : (
                                                            <span className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">
                                                                    Zablokowany
                                                                </span>
                                                        )}
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap">
                                                        <div className="text-sm text-gray-500">{sectionNames[breeder.sectionId] || 'Nieznana'}</div>
                                                    </td>
                                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                        {new Date(breeder.createdAt).toLocaleDateString('pl-PL')}
                                                    </td>

                                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium relative">
                                                        <button
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                setOpenDropdownId(openDropdownId === breeder.id ? null : breeder.id);
                                                            }}
                                                            className="text-gray-400 hover:text-gray-600 p-2 rounded-full hover:bg-gray-100 transition focus:outline-none"
                                                        >
                                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                                                            </svg>
                                                        </button>

                                                        {openDropdownId === breeder.id && (
                                                            <div
                                                                className="absolute right-4 top-12 w-40 bg-white rounded-md shadow-xl py-1 z-50 border border-gray-300 flex flex-col"
                                                                onClick={(e) => e.stopPropagation()}
                                                            >
                                                                <button
                                                                    onClick={() => {
                                                                        setSelectedBreeder(breeder);
                                                                        setOpenDropdownId(null);
                                                                    }}
                                                                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-200 transition"
                                                                >
                                                                    Szczegóły
                                                                </button>

                                                                {breeder.role !== 'ADMINISTRATOR' && (
                                                                    <>
                                                                        <button
                                                                            onClick={() => {
                                                                                setRoleChangeBreeder(breeder);
                                                                                setNewRole(breeder.role);
                                                                                setOpenDropdownId(null);
                                                                            }}
                                                                            className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-200 transition"
                                                                        >
                                                                            Zmień rolę
                                                                        </button>

                                                                        {breeder.status === 'ACTIVE' ? (
                                                                            <button
                                                                                onClick={() => {
                                                                                    if(window.confirm('Czy zablokować tego użytkownika?')) {
                                                                                        handleAction(breeder.id, 'block');
                                                                                        setOpenDropdownId(null);
                                                                                    }
                                                                                }}
                                                                                className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-200 transition border-t border-gray-50"
                                                                            >
                                                                                Zablokuj
                                                                            </button>
                                                                        ) : (
                                                                            <button
                                                                                onClick={() => {
                                                                                    if(window.confirm('Czy odblokować to konto?')) {
                                                                                        handleAction(breeder.id, 'unblock');
                                                                                        setOpenDropdownId(null);
                                                                                    }
                                                                                }}
                                                                                className="block w-full text-left px-4 py-2 text-sm text-blue-600 hover:bg-gray-100 transition border-t border-gray-50"
                                                                            >
                                                                                Odblokuj
                                                                            </button>
                                                                        )}
                                                                    </>
                                                                )}
                                                            </div>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    )}
                                </div>
                            </section>
                        </>
                    )}
                </div>
            </div>

            {/* Kompoment: dane hodowcy */}
            {selectedBreeder && (
                <BreederDetailsModal
                    breeder={selectedBreeder}
                    onClose={() => setSelectedBreeder(null)}
                    sectionNames={sectionNames}
                />
            )}

            {/* Modal: zmiana roli */}
            {roleChangeBreeder && (
                <div className="fixed inset-0 bg-gray-50/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity">
                    <div className="bg-white rounded-lg shadow-2xl max-w-sm w-full p-6 relative">
                        <h3 className="text-2xl font-bold text-gray-900 mb-4">Zmień rolę</h3>
                        <p className="text-sm text-gray-600 mb-6">
                            Wybierz nowe uprawnienia dla użytkownika:
                            <span className="block text-base font-semibold text-gray-900 mt-1">
                                {roleChangeBreeder.name} {roleChangeBreeder.surname}
                            </span>
                        </p>

                        <div className="mb-6">
                            <label className="block text-sm font-medium text-gray-700 mb-2">Nowa rola w systemie</label>
                            <select
                                value={newRole}
                                onChange={(e) => setNewRole(e.target.value)}
                                className="w-full bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 shadow-sm"
                            >
                                <option value="BREEDER">Hodowca (Podstawowy dostęp)</option>
                                <option value="MODERATOR">Moderator (Zarządzanie lotami)</option>
                            </select>
                        </div>

                        <div className="flex justify-end space-x-3">
                            <button
                                onClick={() => setRoleChangeBreeder(null)}
                                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm"
                            >
                                Anuluj
                            </button>
                            <button
                                onClick={submitRoleChange}
                                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition shadow-sm"
                            >
                                Zapisz zmiany
                            </button>
                        </div>
                    </div>
                </div>
            )}

        </AdminGuard>
    );
}