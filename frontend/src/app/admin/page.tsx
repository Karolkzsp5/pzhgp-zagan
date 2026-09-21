"use client";

import { useState, useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { getAuthToken, decodeJwt } from '@/app/utils/jwt';
import AdminGuard from '@/app/components/AdminGuard';
import Navbar from "@/app/components/Navbar";
import Footer from '@/app/components/Footer';
import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import ConfirmModal from '@/app/components/ConfirmModal';
import { formatAccountStatus, formatDate, formatGlobalDate, formatLocalDate, formatPhoneNumber, formatRole } from '@/app/utils/formatters';
import Modal from '@/app/components/Modal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import EmptyState from '@/app/components/EmptyState';

type SortOption = 'NEWEST' | 'OLDEST' | 'A_Z' | 'Z_A';

interface BreederDto {
    id: number;
    name: string;
    surname: string;
    email: string;
    phoneNumber: string;
    dateOfBirth: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
    sectionId: number;
    sectionName: string;
    status: 'PENDING' | 'ACTIVE' | 'BLOCKED';
    createdAt: string;
    role: string;
}

export default function AdminPanelPage() {
    const [pendingBreeders, setPendingBreeders] = useState<BreederDto[]>([]);
    const [registeredBreeders, setRegisteredBreeders] = useState<BreederDto[]>([]);
    const [sectionsList, setSectionsList] = useState<{ id: number; name: string }[]>([]);
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
    const [sortBy, setSortBy] = useState<SortOption>('NEWEST');

    const router = useRouter();

    useEffect(() => {
        fetchAllAccounts();

        const handleClickOutside = () => setOpenDropdownId(null);
        window.addEventListener('click', handleClickOutside);
        return () => window.removeEventListener('click', handleClickOutside);
    }, []);

    const fetchAllAccounts = async () => {
        setIsLoading(true);
        setError('');

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
                fetchWithAuth(`${API_URL}/api/admin/pending`),
                fetchWithAuth(`${API_URL}/api/admin/registered`),
                fetch(`${API_URL}/api/sections`)
            ]);

            if (!pendingRes.ok || !registeredRes.ok || !sectionsRes.ok) {
                if (pendingRes.status === 403 || registeredRes.status === 403) {
                    setError('Brak uprawnień dostępu. Zaloguj się jako administrator.');
                } else {
                    setError('Wystąpił błąd podczas pobierania danych.');
                }
                return;
            }

            const [pendingData, registeredData, sectionsData] = await Promise.all([
                pendingRes.json(),
                registeredRes.json(),
                sectionsRes.json()
            ]);

            setPendingBreeders(pendingData);
            setRegisteredBreeders(registeredData);
            setSectionsList(sectionsData);

        } catch (error) {
            setError('Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleAction = async (id: number, action: 'approve' | 'reject' | 'block' | 'unblock') => {
        const method = action === 'reject' ? 'DELETE' : 'PUT';
        const url = `${API_URL}/api/admin/${action}/${id}`;

        try {
            const response = await fetchWithAuth(url, { method });

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
                setModalMessage(`Błąd: ${await readApiError(response, 'Nie udało się wykonać operacji.')}`);
            }
        } catch (error) {
            setModalMessage('Błąd połączenia z serwerem podczas wykonywania akcji.');
        }
    };

    const submitRoleChange = async () => {
        if (!roleChangeBreeder) return;

        try {
            const response = await fetchWithAuth(`${API_URL}/api/admin/${roleChangeBreeder.id}/role`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ role: newRole })
            });

            if (response.ok) {
                setRegisteredBreeders(prev =>
                    prev.map(b => b.id === roleChangeBreeder.id ? { ...b, role: newRole } : b)
                );
                setRoleChangeBreeder(null);
            } else {
                setModalMessage(`Błąd: ${await readApiError(response, 'Nie udało się zmienić roli.')}`);
            }
        } catch (error) {
            setModalMessage('Błąd połączenia z serwerem podczas zmiany roli.');
        }
    };

    const processBreeders = (breeders: BreederDto[]) => {
        return breeders
            .filter((b) => {
                const searchLower = searchTerm.trim().toLowerCase();
                const normalizedSearch = searchLower.replace(/\s+/g, '');

                const matchesSearch =
                    b.name.toLowerCase().includes(searchLower) ||
                    b.surname.toLowerCase().includes(searchLower) ||
                    b.email.toLowerCase().includes(searchLower) ||
                    b.phoneNumber.replace(/\s+/g, '').includes(normalizedSearch);

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

    const closeConfirmDialog = () => setConfirmDialog({ isOpen: false, message: '', action: null, breederId: null });

    const confirmAdminAction = () => {
        if (confirmDialog.breederId !== null && confirmDialog.action) {
            void handleAction(confirmDialog.breederId, confirmDialog.action);
        }
        closeConfirmDialog();
    };

    const hasActiveFilters = searchTerm.trim().length > 0 || filterSection !== 'ALL';

    return (
        <AdminGuard>
            <div className="flex flex-col min-h-screen">
                <Navbar />
                <main className="grow bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
                    <div className="max-w-7xl mx-auto space-y-12">

                        <div>
                            <h1 className="text-3xl font-bold text-gray-900" data-cy="admin-panel-title">Panel Administratora</h1>
                            <p className="mt-2 text-sm text-gray-600">Zarządzanie kontami hodowców.</p>
                        </div>

                        {isLoading ? (
                            <div data-cy="loading-spinner">
                                <LoadingState />
                            </div>
                        ) : error ? (
                            <div data-cy="error-message">
                                <ErrorState message={error} onRetry={() => void fetchAllAccounts()} />
                            </div>
                        ) : (
                            <>
                                {/* Searching and filtering */}
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
                                            onChange={(e) => setSortBy(e.target.value as SortOption)}
                                        >
                                            <option value="NEWEST">Od najnowszego</option>
                                            <option value="OLDEST">Od najstarszego</option>
                                            <option value="A_Z">Alfabetycznie (A-Z)</option>
                                            <option value="Z_A">Alfabetycznie (Z-A)</option>
                                        </select>
                                    </div>
                                </div>

                                {/* Pending accounts */}
                                <section>
                                    <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Konta czekające na akceptację</h2>
                                        {processedPending.length === 0 ? (
                                            <div data-cy="no-pending-accounts">
                                                <EmptyState>
                                                    {hasActiveFilters
                                                        ? 'Brak kont oczekujących spełniających podane kryteria.'
                                                        : 'Brak kont oczekujących na akceptację.'}
                                                </EmptyState>
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
                                                            <div className="text-sm text-gray-500">Tel: {formatPhoneNumber(breeder.phoneNumber)}</div>
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap">
                                                            <div className="text-sm text-gray-500">{breeder.sectionName}</div>
                                                        </td>
                                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                            {formatGlobalDate(breeder.createdAt)}
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
                                </section>

                                {/* Breeders accounts */}
                                <section>
                                    <h2 className="text-xl font-semibold text-gray-800 mb-4 border-b pb-2">Konta hodowców</h2>
                                        {processedRegistered.length === 0 ? (
                                            <div data-cy="no-registered-accounts">
                                                <EmptyState>
                                                    {hasActiveFilters
                                                        ? 'Brak kont hodowców spełniających podane kryteria.'
                                                        : 'Brak zarejestrowanych kont w systemie.'}
                                                </EmptyState>
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
                                                            <div className="text-sm text-gray-500">Tel: {formatPhoneNumber(breeder.phoneNumber)}</div>
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
                                                            {formatGlobalDate(breeder.createdAt)}
                                                        </td>

                                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium relative">
                                                            <button
                                                                type="button"
                                                                data-cy={`kebab-menu-btn-${breeder.id}`}
                                                                aria-label={`Otwórz akcje dla ${breeder.name} ${breeder.surname}`}
                                                                aria-expanded={openDropdownId === breeder.id}
                                                                aria-haspopup="menu"
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
                                                                                            message: 'Czy na pewno chcesz zablokować to konto?',
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
                                </section>
                            </>
                        )}
                    </div>
                </main>

                <Footer />
            </div>

            {/* Breeder details modal */}
            {selectedBreeder && (
                <Modal isOpen title="Dane hodowcy" onClose={() => setSelectedBreeder(null)} maxWidthClass="max-w-md">
                    <div data-cy="details-modal" className="p-6">
                        <div className="space-y-4">
                            <div>
                                <p className="text-xs text-gray-500 uppercase tracking-wider">Imię i nazwisko</p>
                                <p className="text-lg font-medium text-gray-900">{selectedBreeder.name} {selectedBreeder.surname}</p>
                            </div>

                            <div className="grid grid-cols-1 gap-4">
                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Email</p>
                                    <p className="text-sm font-medium text-gray-800 break-all">{selectedBreeder.email}</p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Telefon</p>
                                    <p className="text-sm font-medium text-gray-800">{formatPhoneNumber(selectedBreeder.phoneNumber)}</p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Data urodzenia</p>
                                    <p className="text-sm font-medium text-gray-800">{formatLocalDate(selectedBreeder.dateOfBirth)}</p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Adres zamieszkania</p>
                                    <p className="text-sm font-medium text-gray-800">
                                        ul. {selectedBreeder.street} {selectedBreeder.houseNumber}<br />
                                        {selectedBreeder.postalCode} {selectedBreeder.city}
                                    </p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Sekcja</p>
                                    <p className="text-sm font-medium text-gray-800">{selectedBreeder.sectionName}</p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Data rejestracji</p>
                                    <p className="text-sm font-medium text-gray-800">{formatDate(selectedBreeder.createdAt)}</p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Rola w systemie</p>
                                    <p className="text-sm font-bold text-gray-700">{formatRole(selectedBreeder.role)}</p>
                                </div>

                                <div>
                                    <p className="text-xs text-gray-500 uppercase tracking-wider">Obecny status</p>
                                    <p className={`text-sm font-bold ${selectedBreeder.status === 'ACTIVE' ? 'text-green-600' : selectedBreeder.status === 'BLOCKED' ? 'text-red-600' : 'text-yellow-600'}`}>
                                        {formatAccountStatus(selectedBreeder.status)}
                                    </p>
                                </div>
                            </div>
                        </div>

                        <div className="mt-8 flex justify-end">
                            <button type="button" onClick={() => setSelectedBreeder(null)} className="bg-blue-600 hover:bg-blue-700 text-white font-medium px-6 py-2 rounded-md transition shadow-sm">
                                Zamknij
                            </button>
                        </div>
                    </div>
                </Modal>
            )}

            {/* role change modal */}
            {roleChangeBreeder && (
                <div data-cy="role-change-modal">
                    <Modal isOpen title="Zmień rolę" onClose={() => setRoleChangeBreeder(null)} maxWidthClass="max-w-sm">
                        <div className="p-6">
                            <p className="text-sm text-gray-600 mb-6">
                                Wybierz nowe uprawnienia dla użytkownika:
                                <span className="block text-base font-semibold text-gray-900 mt-1">
                                    {roleChangeBreeder.name} {roleChangeBreeder.surname}
                                </span>
                            </p>

                            <div className="mb-6">
                                <label htmlFor="role-select" className="block text-sm font-medium text-gray-700 mb-2">Nowa rola w systemie</label>
                                <select
                                    id="role-select"
                                    data-cy="role-select"
                                    value={newRole}
                                    onChange={(e) => setNewRole(e.target.value)}
                                    className="w-full bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block p-2.5 shadow-sm"
                                >
                                    <option value="BREEDER">Hodowca</option>
                                    <option value="MODERATOR">Moderator</option>
                                </select>
                            </div>

                            <div className="flex justify-end space-x-3">
                                <button data-cy="cancel-role-btn" type="button" onClick={() => setRoleChangeBreeder(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm">
                                    Anuluj
                                </button>
                                <button data-cy="save-role-btn" type="button" onClick={submitRoleChange} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition shadow-sm">
                                    Zapisz zmiany
                                </button>
                            </div>
                        </div>
                    </Modal>
                </div>
            )}

            {/* Error modal */}
            <ConfirmModal
                isOpen={modalMessage !== null}
                title="Komunikat systemu"
                message={modalMessage ?? ''}
                isAlert
                variant="primary"
                confirmLabel="OK"
                onConfirm={() => setModalMessage(null)}
                onCancel={() => setModalMessage(null)}
            />

            {/* Block/unblock confirmation modal */}
            <ConfirmModal
                isOpen={confirmDialog.isOpen}
                title="Potwierdzenie akcji"
                message={confirmDialog.message}
                variant={confirmDialog.action === 'reject' || confirmDialog.action === 'block' ? 'danger' : 'primary'}
                confirmLabel="Potwierdź"
                confirmButtonDataCy="confirm-dialog-btn"
                onConfirm={confirmAdminAction}
                onCancel={closeConfirmDialog}
            />

        </AdminGuard>
    );
}