"use client";

import { use, useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import AuthGuard from '@/app/components/AuthGuard';
import ConfirmModal from '@/app/components/ConfirmModal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import EmptyState from '@/app/components/EmptyState';
import ActionMenu from '@/app/components/ActionMenu';
import FlightPlanEntryModal from '@/app/components/FlightPlanEntryModal';
import { flightPlanService } from '@/app/services/flightPlanService';
import { FlightPlanDetailsDto, FlightPlanEntryDto, FlightResultSummaryDto, PigeonAgeGroup } from '@/app/types/flightPlan';
import { decodeJwt, getAuthToken, isJwtValid } from '@/app/utils/jwt';
import { formatLocalDate } from '@/app/utils/formatters';

interface FlightPlanYearPageProps {
    params: Promise<{
        year: string;
    }>;
}

export default function FlightPlanYearPage({ params }: FlightPlanYearPageProps) {
    const resolvedParams = use(params);
    const year = Number(resolvedParams.year);

    const [plan, setPlan] = useState<FlightPlanDetailsDto | null>(null);
    const [userRole, setUserRole] = useState<string | null>(null);

    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [openingResultId, setOpeningResultId] = useState<number | null>(null);
    const [openActionsId, setOpenActionsId] = useState<number | null>(null);

    const [isEntryModalOpen, setIsEntryModalOpen] = useState(false);
    const [entryAgeGroup, setEntryAgeGroup] = useState<PigeonAgeGroup>('ADULT');
    const [editingEntry, setEditingEntry] = useState<FlightPlanEntryDto | null>(null);

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const isAdministrator = userRole === 'ADMINISTRATOR';

    const closeModal = () => {
        setModalConfig(previous => ({
            ...previous,
            isOpen: false
        }));
    };

    const showAlert = (title: string, message: string) => {
        setModalConfig({
            isOpen: true,
            title,
            message,
            isAlert: true,
            onConfirm: closeModal
        });
    };

    const loadPlan = useCallback(async () => {
        if (!Number.isInteger(year)) {
            setLoadError('Nieprawidłowy rok planu lotów.');
            setIsLoading(false);
            return;
        }

        setIsLoading(true);
        setLoadError('');

        try {
            setPlan(await flightPlanService.getPlanByYear(year));
        } catch (error) {
            setLoadError(error instanceof Error ? error.message : 'Nie udało się pobrać planu lotów.');
        } finally {
            setIsLoading(false);
        }
    }, [year]);

    useEffect(() => {
        const token = getAuthToken();

        if (isJwtValid(token)) {
            const payload = decodeJwt(token!);
            setUserRole(payload?.role ?? null);
        }

        void loadPlan();
    }, [loadPlan]);

    const handleOpenResult = async (resultId: number) => {
        const resultWindow = window.open('', '_blank');

        if (!resultWindow) {
            showAlert(
                'Nie można otworzyć wyników',
                'Przeglądarka zablokowała otwarcie nowej karty. Zezwól na wyskakujące okna dla tej strony i spróbuj ponownie.'
            );
            return;
        }

        setOpeningResultId(resultId);

        try {
            const blob = await flightPlanService.getResultFile(resultId);
            const objectUrl = URL.createObjectURL(blob);

            resultWindow.location.href = objectUrl;

            window.setTimeout(() => {
                URL.revokeObjectURL(objectUrl);
            }, 60_000);
        } catch (error) {
            resultWindow.close();

            showAlert(
                'Nie udało się otworzyć wyników',
                error instanceof Error ? error.message : 'Wystąpił błąd podczas pobierania wyników lotu.'
            );
        } finally {
            setOpeningResultId(null);
        }
    };

    const handleAddFlight = (ageGroup: PigeonAgeGroup) => {
        setEntryAgeGroup(ageGroup);
        setEditingEntry(null);
        setIsEntryModalOpen(true);
    };

    const handleEditFlight = (flight: FlightPlanEntryDto) => {
        setEntryAgeGroup(flight.pigeonAgeGroup);
        setEditingEntry(flight);
        setIsEntryModalOpen(true);
    };

    const handleDeleteFlight = (flight: FlightPlanEntryDto) => {
        setModalConfig({
            isOpen: true,
            title: 'Usuń lot',
            message: `Czy na pewno chcesz usunąć lot ${flight.location} z dnia ${formatLocalDate(flight.scheduledDate)}?`,
            isAlert: false,
            onConfirm: async () => {
                closeModal();

                try {
                    await flightPlanService.deleteEntry(flight.id);
                    await loadPlan();
                } catch (error) {
                    showAlert('Nie udało się usunąć lotu',
                        error instanceof Error ? error.message : 'Wystąpił błąd podczas usuwania lotu.'
                    );
                }
            }
        });
    };

    return (
        <AuthGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <div className="mb-8">
                        <Link
                            href="/flight-plans"
                            className="inline-flex items-center gap-1 text-sm font-medium text-blue-600 hover:text-blue-800 transition mb-4"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                                <path d="M384-96 0-480l384-384 68 68-316 316 316 316-68 68Z"></path>
                            </svg>
                            Wróć do planów
                        </Link>

                        <div className="border-b border-gray-200 pb-5">
                            <h1
                                className="text-3xl font-bold text-gray-900"
                                data-cy="flight-plan-year-title"
                            >
                                Plan lotów {year}
                            </h1>

                            <p className="mt-2 text-sm text-gray-600">
                                Plan oraz dostępne wyniki lotów oddziałowych i sekcyjnych.
                            </p>
                        </div>
                    </div>

                    {isLoading ? (
                        <LoadingState />
                    ) : loadError ? (
                        <ErrorState
                            message={loadError}
                            onRetry={() => void loadPlan()}
                        />
                    ) : plan ? (
                        <div className="space-y-12">
                            <FlightTableSection
                                title="Gołębie dorosłe"
                                ageGroup="ADULT"
                                flights={plan.adultFlights}
                                isAdministrator={isAdministrator}
                                openingResultId={openingResultId}
                                openActionsId={openActionsId}
                                onToggleActions={id => setOpenActionsId(previous => previous === id ? null : id)}
                                onCloseActions={() => setOpenActionsId(null)}
                                onOpenResult={handleOpenResult}
                                onAddFlight={handleAddFlight}
                                onEditFlight={handleEditFlight}
                                onDeleteFlight={handleDeleteFlight}
                            />

                            <FlightTableSection
                                title="Gołębie młode"
                                ageGroup="YOUNG"
                                flights={plan.youngFlights}
                                isAdministrator={isAdministrator}
                                openingResultId={openingResultId}
                                openActionsId={openActionsId}
                                onToggleActions={id => setOpenActionsId(previous => previous === id ? null : id)}
                                onCloseActions={() => setOpenActionsId(null)}
                                onOpenResult={handleOpenResult}
                                onAddFlight={handleAddFlight}
                                onEditFlight={handleEditFlight}
                                onDeleteFlight={handleDeleteFlight}
                            />
                        </div>
                    ) : null}
                </main>

                <Footer />

                <FlightPlanEntryModal
                    isOpen={isEntryModalOpen}
                    onClose={() => {
                        setIsEntryModalOpen(false);
                        setEditingEntry(null);
                    }}
                    onSaved={loadPlan}
                    year={year}
                    pigeonAgeGroup={entryAgeGroup}
                    entryToEdit={editingEntry}
                />

                <ConfirmModal
                    isOpen={modalConfig.isOpen}
                    title={modalConfig.title}
                    message={modalConfig.message}
                    isAlert={modalConfig.isAlert}
                    onConfirm={modalConfig.onConfirm}
                    onCancel={closeModal}
                />
            </div>
        </AuthGuard>
    );
}

interface FlightTableSectionProps {
    title: string;
    ageGroup: PigeonAgeGroup;
    flights: FlightPlanEntryDto[];
    isAdministrator: boolean;
    openingResultId: number | null;
    openActionsId: number | null;
    onToggleActions: (flightId: number) => void;
    onCloseActions: () => void;
    onOpenResult: (resultId: number) => Promise<void>;
    onAddFlight: (ageGroup: PigeonAgeGroup) => void;
    onEditFlight: (flight: FlightPlanEntryDto) => void;
    onDeleteFlight: (flight: FlightPlanEntryDto) => void;
}

function FlightTableSection({
                                title,
                                ageGroup,
                                flights,
                                isAdministrator,
                                openingResultId,
                                openActionsId,
                                onToggleActions,
                                onCloseActions,
                                onOpenResult,
                                onAddFlight,
                                onEditFlight,
                                onDeleteFlight
                            }: FlightTableSectionProps) {
    return (
        <section>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                <h2 className="text-2xl font-bold text-gray-900">
                    {title}
                </h2>

                {isAdministrator && (
                    <button
                        type="button"
                        onClick={() => onAddFlight(ageGroup)}
                        className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2 px-4 rounded-md shadow-sm transition shrink-0"
                    >
                        + Dodaj lot
                    </button>
                )}
            </div>

            {flights.length === 0 ? (
                <EmptyState>
                    Brak lotów w tej części planu.
                </EmptyState>
            ) : (
                <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-blue-100">
                        <tr>
                            <TableHeader className="text-right w-16">Lp.</TableHeader>
                            <TableHeader>Data</TableHeader>
                            <TableHeader>Miejscowość</TableHeader>
                            <TableHeader>Dystans</TableHeader>
                            <TableHeader>Kategoria</TableHeader>
                            <TableHeader>Rodzaj listy</TableHeader>
                            <TableHeader>Wyniki</TableHeader>

                            {isAdministrator && (
                                <TableHeader>Akcje</TableHeader>
                            )}
                        </tr>
                        </thead>

                        <tbody className="bg-white divide-y divide-gray-200">
                        {flights.map((flight, index) => (
                            <FlightTableRow
                                key={flight.id}
                                rowNumber={index + 1}
                                flight={flight}
                                isAdministrator={isAdministrator}
                                openingResultId={openingResultId}
                                openActionsId={openActionsId}
                                onToggleActions={onToggleActions}
                                onCloseActions={onCloseActions}
                                onOpenResult={onOpenResult}
                                onEditFlight={onEditFlight}
                                onDeleteFlight={onDeleteFlight}
                            />
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}

function TableHeader({children, className = ''}: {
    children: React.ReactNode;
    className?: string;
}) {
    return (
        <th
            scope="col"
            className={`px-6 py-3 text-center text-xs font-semibold text-gray-600 uppercase tracking-wider whitespace-nowrap ${className}`}
        >
            {children}
        </th>
    );
}

interface FlightTableRowProps {
    rowNumber: number;
    flight: FlightPlanEntryDto;
    isAdministrator: boolean;
    openingResultId: number | null;
    openActionsId: number | null;
    onToggleActions: (flightId: number) => void;
    onCloseActions: () => void;
    onOpenResult: (resultId: number) => Promise<void>;
    onEditFlight: (flight: FlightPlanEntryDto) => void;
    onDeleteFlight: (flight: FlightPlanEntryDto) => void;
}

function FlightTableRow({
                            rowNumber,
                            flight,
                            isAdministrator,
                            openingResultId,
                            openActionsId,
                            onToggleActions,
                            onCloseActions,
                            onOpenResult,
                            onEditFlight,
                            onDeleteFlight
                        }: FlightTableRowProps) {

    const handleResultChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
        const resultId = Number(event.target.value);

        if (!resultId) return;

        event.target.value = '';
        void onOpenResult(resultId);
    };

    return (
        <tr className="bg-white even:bg-slate-50 transition duration-150">
            <td className="px-6 py-3 text-sm text-gray-500 text-right whitespace-nowrap">
                {rowNumber}
            </td>

            <td className="px-6 py-3 text-sm text-gray-700 text-center whitespace-nowrap">
                {formatLocalDate(flight.scheduledDate)}
            </td>

            <td className="px-6 py-3 text-sm font-medium text-gray-900 text-center whitespace-nowrap">
                {flight.location}
            </td>

            <td className="px-6 py-3 text-sm text-gray-700 text-center whitespace-nowrap">
                {flight.distanceKm} km
            </td>

            <td className="px-6 py-3 text-sm text-gray-700 text-center whitespace-nowrap">
                {flight.category || '—'}
            </td>

            <td className="px-6 py-3 text-sm text-gray-700 text-center whitespace-nowrap">
                {flight.listType}
            </td>

            <td className="px-6 py-3 min-w-52 text-center">
                {flight.results.length === 0 ? (
                    <span className="text-sm text-gray-400">
                        Brak wyników
                    </span>
                ) : (
                    <select
                        defaultValue=""
                        onChange={handleResultChange}
                        disabled={openingResultId !== null}
                        aria-label={`Wyniki lotu ${flight.location}`}
                        className="w-full min-w-44 px-3 py-1.5 border border-gray-300 rounded-md text-sm text-center text-gray-700 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                    >
                        <option value="">
                            {openingResultId !== null
                                ? 'Otwieranie...'
                                : 'Wybierz wyniki'}
                        </option>

                        {flight.results.map(result => (
                            <option
                                key={result.id}
                                value={result.id}
                            >
                                {formatResultLabel(result)}
                            </option>
                        ))}
                    </select>
                )}
            </td>

            {isAdministrator && (
                <td className="px-6 py-3 whitespace-nowrap text-center text-sm font-medium">
                    <ActionMenu
                        isOpen={openActionsId === flight.id}
                        onToggle={() => onToggleActions(flight.id)}
                        onClose={onCloseActions}
                        ariaLabel={`Otwórz akcje dla lotu ${flight.location}`}
                        buttonDataCy={`flight-actions-button-${flight.id}`}
                        menuDataCy={`flight-actions-menu-${flight.id}`}
                    >
                        <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                                onCloseActions();
                                onEditFlight(flight);
                            }}
                            className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-200 transition"
                        >
                            Edytuj lot
                        </button>

                        <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                                onCloseActions();
                            }}
                            className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-200 transition"
                        >
                            Dodaj wyniki
                        </button>

                        <button
                            type="button"
                            role="menuitem"
                            onClick={() => {
                                onCloseActions();
                                onDeleteFlight(flight);
                            }}
                            className="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-200 transition border-t border-gray-50"
                        >
                            Usuń lot
                        </button>
                    </ActionMenu>
                </td>
            )}
        </tr>
    );
}

function formatResultLabel(result: FlightResultSummaryDto): string {
    if (result.scope === 'BRANCH') {
        return 'Oddział';
    }

    if (result.sectionSortOrder !== null && result.sectionName) {
        return `Sekcja ${result.sectionSortOrder} ${result.sectionName}`;
    }

    return result.sectionName ?? 'Sekcja';
}