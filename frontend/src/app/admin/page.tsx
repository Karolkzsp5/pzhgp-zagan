"use client";

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, decodeJwt } from '@/utils/jwt';
import AdminGuard from '../components/AdminGuard';
import Navbar from "@/app/components/Navbar";
import BreederDetailsModal, { BreederDto } from '../components/BreederDetailsModal';
import Footer from '../components/Footer';

export default function AdminPanelPage() {
    const [pendingBreeders, setPendingBreeders] = useState<BreederDto[]>([]);
    const [registeredBreeders, setRegisteredBreeders] = useState<BreederDto[]>([]);
    const [sectionsList, setSectionsList] = useState<{id: number, name: string}[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [currentUserEmail, setCurrentUserEmail] = useState('');
    const [modalMessage, setModalMessage] = useState<string | null>(null);
    const [confirmDialog, setConfirmDialog] = useState<{
        isOpen: boolean;
        message: string;
        action: 'approve' | 'reject' | 'block' | 'unblock' | null;
        breederId: number | null;
    }>({ isOpen: false, message: '', action: null, breederId: null });

    const [openDropdownId, setOpenDropdownId] = useState<number | null>(null);
    const [selectedBreeder, setSelectedBreeder] = useState<BreederDto | null>(null);
    const [roleChangeBreeder, setRoleChangeBreeder] = useState<BreederDto | null>(null);
    const [newRole, setNewRole] = useState<string>('');

    const [searchTerm, setSearchTerm] = useState('');
    const [filterSection, setFilterSection] = useState<string>('ALL');
    const [sortBy, setSortBy] = useState<'NEWEST' | 'OLDEST' | 'A_Z' | 'Z_A'>('NEWEST');

    const router = useRouter();

    useEffect(() => {
        fetchAllAccounts();

        const handleClickOutside = () => setOpenDropdownId(null);
        window.addEventListener('click', handleClickOutside);
        return () => window.removeEventListener('click', handleClickOutside);
    }, []);

    const fetchAllAccounts = async () => {
        const token = getAuthToken();
        if (!token) {
            router.push('/login');
            return;
        }

        const payload = decodeJwt(token);
        if (payload) {
            setCurrentUserEmail(payload.sub);
        }

        try {
            const [pendingRes, registeredRes, sectionsRes] = await Promise.all([
                fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/admin/pending`, {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                }),
                fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/admin/registered`, {
                    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
                }),
                fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/sections`)
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

            if (sectionsRes.ok) {
                const sectionsData = await sectionsRes.json();
                setSectionsList(sectionsData);
            }

        } catch (err) {
            setError('Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleAction = async (id: number, action: 'approve' | 'reject' | 'block' | 'unblock') => {
        const token = getAuthToken();
        if (!token) return;

        let method = 'PUT';
        if (action === 'reject') method = 'DELETE';

        const url = `${process.env.NEXT_PUBLIC_API_URL}/api/admin/${action}/${id}`;

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
                setModalMessage(`Błąd: ${errorData}`);
            }
        } catch (err) {
            setModalMessage('Błąd połączenia z serwerem podczas wykonywania akcji.');
        }
    };

    const submitRoleChange = async () => {
        if (!roleChangeBreeder) return;
        const token = getAuthToken();
        if (!token) return;

        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/admin/${roleChangeBreeder.id}/role`, {
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
                setModalMessage(`Błąd: ${errorData}`);
            }
        } catch (err) {
            setModalMessage('Błąd połączenia z serwerem podczas zmiany roli.');
        }
    };

    const processBreeders = (breeders: BreederDto[]) => {
        return breeders
            .filter((b) => {
                const searchLower = searchTerm.toLowerCase();
                const matchesSearch =
                    b.name.toLowerCase().includes(searchLower) ||
                    b.surname.toLowerCase().includes(searchLower) ||
                    b.email.toLowerCase().includes(searchLower) ||
                    b.phoneNumber.includes(searchLower);

                const matchesSection = filterSection === 'ALL' || b.sectionId.toString() === filterSection;

                return matchesSearch && matchesSection;
            })
            .sort((a, b) => {
                if (sortBy === 'NEWEST') return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
                if (sortBy === 'OLDEST') return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
                if (sortBy === 'A_Z') return a.name.localeCompare(b.name);
                if (sortBy === 'Z_A') return b.name.localeCompare(a.name);
                return 0;
            });
    };

    const processedPending = useMemo(() => {
        return processBreeders(pendingBreeders);
    }, [pendingBreeders, searchTerm, filterSection, sortBy]);

    const processedRegistered = useMemo(() => {
        return processBreeders(registeredBreeders);
    }, [registeredBreeders, searchTerm, filterSection, sortBy]);

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-50" data-cy="loading-spinner">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
            </div>
        );
    }

    return (
        <AdminGuard>
            <div className="flex flex-col min-h-screen">
                <Navbar />

                <main className="flex-grow bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
                    <div className="max-w-7xl mx-auto space-y-12">

                        <div>
                            <h1 className="text-3xl font-bold text-gray-900" data-cy="admin-panel-title">Panel Administratora</h1>
                            <p className="mt-2 text-sm text-gray-600">Zarządzanie kontami hodowców.</p>
                        </div>

                        {/* Wyszukiwanie i filtrowanie */}
                        <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col md:flex-row md:items-center justify-between gap-4">
                            <div className="relative flex-1">
                                <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
                                    <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                    </svg>
                                </div>
                                <input
                                    data-cy="search-input"
                                    type="text"
                                    className="block w-full p-2.5 pl-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-gray-50 focus:ring-blue-500 focus:border-blue-500 transition"
                                    placeholder="Szukaj po imieniu, nazwisku, emailu lub telefonie..."
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                            </div>

                            <div className="flex flex-col sm:flex-row gap-3">
                                <select
                                    data-cy="section-filter"
                                    className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5"
                                    value={filterSection}
                                    onChange={(e) => setFilterSection(e.target.value)}
                                >
                                    <option value="ALL">Wszystkie sekcje</option>
                                    {sectionsList.map((section) => (
                                        <option key={section.id} value={section.id.toString()}>
                                            {section.name}
                                        </option>
                                    ))}
                                </select>

                                <select
                                    data-cy="sort-filter"
                                    className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5"
                                    value={sortBy}
                                    onChange={(e) => setSortBy(e.target.value as any)}
                                >
                                    <option value="NEWEST">Od najnowszego</option>
                                    <option value="OLDEST">Od najstarszego</option>
                                    <option value="A_Z">Alfabetycznie (A-Z)</option>
                                    <option value="Z_A">Alfabetycznie (Z-A)</option>
                                </select>
                            </div>
                        </div>

                        {error ? (
                            <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded mb-6" data-cy="error-message">
                                <p className="text-sm text-red-700">{error}</p>
                            </div>
                        ) : (
                            <>
                                {/* Oczekujące konta */}
                                <section>
                                    <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Konta czekające na akceptację</h2>
                                    <div className="bg-white shadow overflow-x-auto sm:rounded-lg border border-gray-200">
                                        {processedPending.length === 0 ? (
                                            <div className="p-8 text-center text-gray-500" data-cy="no-pending-accounts">
                                                Brak kont oczekujących na akceptację.
                                            </div>
                                        ) : (
                                            <table className="min-w-full divide-y divide-gray-200" data-cy="pending-table">
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
                                                {processedPending.map((breeder) => (
                                                    <tr key={breeder.id} className="bg-white even:bg-slate-50 transition duration-150" data-cy={`pending-row-${breeder.id}`}>
                                                        <td className="px-6 py-4 whitespace-nowrap">
                                                            <div className="text-sm font-medium text-gray-900">{breeder.name} {breeder.surname}</div>
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap">
                                                            <div className="text-sm text-gray-900">{breeder.email}</div>
                                                            <div className="text-sm text-gray-500">Tel: {breeder.phoneNumber}</div>
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap">
                                                            <div className="text-sm text-gray-500">{breeder.sectionName}</div>
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                            {new Date(breeder.createdAt).toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' })}
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap text-left text-sm font-medium">
                                                            <button
                                                                data-cy={`approve-btn-${breeder.id}`}
                                                                onClick={() => {
                                                                    setConfirmDialog({
                                                                        isOpen: true,
                                                                        message: 'Czy na pewno chcesz zatwierdzić rejestrację tego konta?',
                                                                        action: 'approve',
                                                                        breederId: breeder.id
                                                                    });
                                                                    setOpenDropdownId(null);
                                                                }}
                                                                className="text-green-600 hover:text-green-900 bg-green-50 hover:bg-green-100 px-3 py-1 rounded-md transition mr-3"
                                                            >
                                                                Akceptuj
                                                            </button>
                                                            <button
                                                                data-cy={`reject-btn-${breeder.id}`}
                                                                onClick={() => {
                                                                    setConfirmDialog({
                                                                        isOpen: true,
                                                                        message: 'Czy na pewno chcesz odrzucić i usunąć to konto?',
                                                                        action: 'reject',
                                                                        breederId: breeder.id
                                                                    });
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
                                    <div className="bg-white shadow overflow-x-auto sm:rounded-lg border border-gray-200">
                                        {processedRegistered.length === 0 ? (
                                            <div className="p-8 text-center text-gray-500" data-cy="no-registered-accounts">
                                                Brak zarejestrowanych kont w systemie.
                                            </div>
                                        ) : (
                                            <table className="min-w-full divide-y divide-gray-200" data-cy="registered-table">
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
                                                {processedRegistered.map((breeder) => (
                                                    <tr key={breeder.id} className="bg-white even:bg-slate-50 transition duration-150" data-cy={`registered-row-${breeder.id}`}>
                                                        <td className="px-6 py-4 whitespace-nowrap">
                                                            <div className="text-sm font-medium text-gray-900 flex items-center">
                                                                {breeder.name} {breeder.surname}
                                                                {breeder.role === 'ADMINISTRATOR' && (
                                                                    <span data-cy="badge-admin" className="ml-2 px-2 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800 border border-purple-200">
                                                                        ADMIN
                                                                    </span>
                                                                )}
                                                                {breeder.role === 'MODERATOR' && (
                                                                    <span data-cy="badge-moderator" className="ml-2 px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
                                                                        MODERATOR
                                                                    </span>
                                                                )}
                                                                {breeder.email === currentUserEmail && (
                                                                    <span data-cy="badge-you" className="ml-2 px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">
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
                                                                <span data-cy="status-active" className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                                                                        Aktywny
                                                                    </span>
                                                            ) : (
                                                                <span data-cy="status-blocked" className="px-2.5 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">
                                                                        Zablokowany
                                                                    </span>
                                                            )}
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap">
                                                            <div className="text-sm text-gray-500">{breeder.sectionName}</div>
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                            {new Date(breeder.createdAt).toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' })}
                                                        </td>

                                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium relative">
                                                            <button
                                                                data-cy={`kebab-menu-btn-${breeder.id}`}
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
                                                                    data-cy={`dropdown-menu-${breeder.id}`}
                                                                    className="absolute right-4 top-12 w-40 bg-white rounded-md shadow-xl py-1 z-50 border border-gray-300 flex flex-col"
                                                                    onClick={(e) => e.stopPropagation()}
                                                                >
                                                                    <button
                                                                        data-cy="details-option"
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
                                                                                data-cy="change-role-option"
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
                                                                                    data-cy="block-option"
                                                                                    onClick={() => {
                                                                                        setConfirmDialog({
                                                                                            isOpen: true,
                                                                                            message: 'Czy na pewno chcesz zablokować tego użytkownika?',
                                                                                            action: 'block',
                                                                                            breederId: breeder.id
                                                                                        });
                                                                                        setOpenDropdownId(null);
                                                                                    }}
                                                                                    className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-200 transition border-t border-gray-50"
                                                                                >
                                                                                    Zablokuj
                                                                                </button>
                                                                            ) : (
                                                                                <button
                                                                                    data-cy="unblock-option"
                                                                                    onClick={() => {
                                                                                        setConfirmDialog({
                                                                                            isOpen: true,
                                                                                            message: 'Czy na pewno chcesz odblokować to konto?',
                                                                                            action: 'unblock',
                                                                                            breederId: breeder.id
                                                                                        });
                                                                                        setOpenDropdownId(null);
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
                </main>

                <Footer />
            </div>

            {/* Kompoment: dane hodowcy */}
            {selectedBreeder && (
                <BreederDetailsModal
                    breeder={selectedBreeder}
                    onClose={() => setSelectedBreeder(null)}
                />
            )}

            {/* Modal: zmiana roli */}
            {roleChangeBreeder && (
                <div data-cy="role-change-modal" className="fixed inset-0 bg-gray-50/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity">
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
                                data-cy="role-select"
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
                                data-cy="cancel-role-btn"
                                onClick={() => setRoleChangeBreeder(null)}
                                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm"
                            >
                                Anuluj
                            </button>
                            <button
                                data-cy="save-role-btn"
                                onClick={submitRoleChange}
                                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition shadow-sm"
                            >
                                Zapisz zmiany
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal powiadomień i błędów */}
            {modalMessage && (
                <div className="fixed inset-0 bg-gray-50/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity">
                    <div className="bg-white rounded-lg shadow-2xl max-w-sm w-full p-6 relative">
                        <div className="flex items-center space-x-3 mb-4">
                            <div className="flex-shrink-0 flex items-center justify-center h-10 w-10 rounded-full bg-red-100 text-red-600">
                                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-bold text-gray-900">Komunikat systemu</h3>
                        </div>

                        <p className="text-sm text-gray-600 mb-6 leading-relaxed">
                            {modalMessage}
                        </p>

                        <div className="flex justify-end">
                            <button
                                onClick={() => setModalMessage(null)}
                                className="px-5 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition shadow-sm"
                            >
                                OK
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Modal potwierdzenia akcji */}
            {confirmDialog.isOpen && (
                <div className="fixed inset-0 bg-gray-50/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity">
                    <div className="bg-white rounded-lg shadow-2xl max-w-sm w-full p-6 relative">
                        <h3 className="text-xl font-bold text-gray-900 mb-4">Potwierdzenie akcji</h3>

                        <p className="text-sm text-gray-600 mb-8 leading-relaxed">
                            {confirmDialog.message}
                        </p>

                        <div className="flex justify-end space-x-3">
                            <button
                                onClick={() => setConfirmDialog({ isOpen: false, message: '', action: null, breederId: null })}
                                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm"
                            >
                                Anuluj
                            </button>
                            <button
                                data-cy="confirm-dialog-btn"
                                onClick={() => {
                                    if (confirmDialog.breederId && confirmDialog.action) {
                                        handleAction(confirmDialog.breederId, confirmDialog.action);
                                    }
                                    setConfirmDialog({ isOpen: false, message: '', action: null, breederId: null });
                                }}
                                className={`px-4 py-2 text-sm font-medium text-white rounded-md transition shadow-sm ${
                                    confirmDialog.action === 'reject' || confirmDialog.action === 'block'
                                        ? 'bg-red-600 hover:bg-red-700'
                                        : 'bg-blue-600 hover:bg-blue-700'
                                }`}
                            >
                                Potwierdź
                            </button>
                        </div>
                    </div>
                </div>
            )}

        </AdminGuard>
    );
}