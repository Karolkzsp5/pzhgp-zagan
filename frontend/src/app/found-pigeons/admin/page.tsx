"use client";

import {useCallback, useEffect, useRef, useState} from 'react';
import Link from 'next/link';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import AdminGuard from '@/app/components/AdminGuard';
import ConfirmModal from '@/app/components/ConfirmModal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import EmptyState from '@/app/components/EmptyState';
import { foundPigeonService } from '@/app/services/foundPigeonService';
import { formatGlobalDate } from '@/app/utils/formatters';
import {
    ALLOWED_STATUS_TRANSITIONS,
    FoundPigeonDto,
    FoundPigeonStatus,
    STATUS_BADGE_CLASSES,
    formatReportLanguage,
    formatReportStatus,
    formatStatusAction
} from '@/app/types/foundPigeon';

const PAGE_SIZE = 10;

const STATUS_FILTERS: { value: FoundPigeonStatus | null; label: string }[] = [
    { value: null, label: 'Wszystkie' },
    { value: 'PENDING', label: formatReportStatus('PENDING') },
    { value: 'APPROVED', label: formatReportStatus('APPROVED') },
    { value: 'RESOLVED', label: formatReportStatus('RESOLVED') },
    { value: 'REJECTED', label: formatReportStatus('REJECTED') }
];

function StatusBadge({ status }: { status: FoundPigeonStatus }) {
    return (
        <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-semibold border whitespace-nowrap ${STATUS_BADGE_CLASSES[status]}`}>
            {formatReportStatus(status)}
        </span>
    );
}

export default function FoundPigeonsAdminPage() {
    const [reports, setReports] = useState<FoundPigeonDto[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [page, setPage] = useState(0);
    const [statusFilter, setStatusFilter] = useState<FoundPigeonStatus | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [appliedSearch, setAppliedSearch] = useState('');

    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const requestControllerRef = useRef<AbortController | null>(null);

    const [selected, setSelected] = useState<FoundPigeonDto | null>(null);
    const [noteDraft, setNoteDraft] = useState('');
    const [isSavingNote, setIsSavingNote] = useState(false);
    const [isChangingStatus, setIsChangingStatus] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const [detailsError, setDetailsError] = useState('');

    const isMutating = isSavingNote || isChangingStatus || isDeleting;

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const closeModal = () => setModalConfig(previous => ({ ...previous, isOpen: false }));

    const showAlert = (title: string, message: string) =>
        setModalConfig({ isOpen: true, title, message, isAlert: true, onConfirm: closeModal });

    const fetchReports = useCallback(
        async (targetPage: number, status: FoundPigeonStatus | null, ringNumber: string) => {
            requestControllerRef.current?.abort();

            const controller = new AbortController();
            requestControllerRef.current = controller;

            setIsLoading(true);
            setLoadError('');

            try {
                const result = await foundPigeonService.getReports(
                    status,
                    ringNumber,
                    targetPage,
                    PAGE_SIZE,
                    controller.signal
                );

                if (controller.signal.aborted) {
                    return;
                }

                setReports(result.content);
                setTotalPages(result.totalPages);
                setTotalElements(result.totalElements);
                setPage(result.number);
            } catch (error) {
                if (controller.signal.aborted) {
                    return;
                }

                setLoadError(error instanceof Error ? error.message : 'Nie udało się pobrać zgłoszeń.');
            } finally {
                if (requestControllerRef.current === controller) {
                    requestControllerRef.current = null;

                    if (!controller.signal.aborted) {
                        setIsLoading(false);
                    }
                }
            }
        }, []
    );

    useEffect(() => {
        void fetchReports(0, null, '');

        return () => {
            requestControllerRef.current?.abort();
        };
    }, [fetchReports]);

    const applyFilters = (status: FoundPigeonStatus | null, ringNumber: string) => {
        setStatusFilter(status);
        setAppliedSearch(ringNumber);

        setSelected(null);
        setNoteDraft('');
        setDetailsError('');

        void fetchReports(0, status, ringNumber);
    };

    const changePage = (targetPage: number) => {
        setSelected(null);
        setNoteDraft('');
        setDetailsError('');

        void fetchReports(
            targetPage,
            statusFilter,
            appliedSearch
        );
    };

    const openDetails = (report: FoundPigeonDto) => {
        setSelected(report);
        setNoteDraft(report.adminNote ?? '');
        setDetailsError('');
    };

    const applyUpdate = (updated: FoundPigeonDto) => {
        setSelected(updated);
        setReports(previous => previous.map(item => (item.id === updated.id ? updated : item)));
    };

    const handleStatusChange = async (report: FoundPigeonDto, status: FoundPigeonStatus) => {
        if (isMutating) {
            return;
        }

        setIsChangingStatus(true);
        setDetailsError('');

        try {
            const updated = await foundPigeonService.updateStatus(report.id, status);
            applyUpdate(updated);

            if (
                statusFilter !== null &&
                statusFilter !== updated.status
            ) {
                setSelected(null);
                setNoteDraft('');
            }

            await fetchReports(
                page,
                statusFilter,
                appliedSearch
            );
        } catch (error) {
            setDetailsError(error instanceof Error ? error.message : 'Nie udało się zmienić statusu.');
        } finally {
            setIsChangingStatus(false);
        }
    };

    const handleSaveNote = async () => {
        if (!selected || isMutating) return;

        setIsSavingNote(true);
        setDetailsError('');
        try {
            applyUpdate(await foundPigeonService.updateNote(selected.id, noteDraft));
        } catch (error) {
            setDetailsError(error instanceof Error ? error.message : 'Nie udało się zapisać notatki.');
        } finally {
            setIsSavingNote(false);
        }
    };

    const handleDelete = (report: FoundPigeonDto) => {
        if (isMutating) {
            return;
        }
        setModalConfig({
            isOpen: true,
            title: 'Usuń zgłoszenie',
            message: `Czy na pewno chcesz trwale usunąć zgłoszenie obrączki ${report.ringNumber} `
                + 'razem z danymi kontaktowymi znalazcy? Tej operacji nie można cofnąć.',
            isAlert: false,
            onConfirm: async () => {
                closeModal();
                setIsDeleting(true);

                try {
                    await foundPigeonService.deleteReport(report.id);
                    setSelected(null);
                    const isLastOnPage = reports.length === 1 && page > 0;
                    await fetchReports(isLastOnPage ? page - 1 : page, statusFilter, appliedSearch);
                } catch (error) {
                    showAlert('Błąd', error instanceof Error ? error.message : 'Nie udało się usunąć zgłoszenia.');
                } finally {
                    setIsDeleting(false);
                }
            }
        });
    };

    return (
        <AdminGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <Link
                        href="/found-pigeons"
                        className="inline-flex items-center gap-1 text-sm text-blue-700 hover:text-blue-900 hover:underline mb-4"
                    >
                        <svg className="w-4 h-4" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                            <path d="M400-80 0-480l400-400 71 71-329 329 329 329-71 71Z" />
                        </svg>
                        Formularz publiczny
                    </Link>

                    <div className="border-b border-gray-200 pb-5 mb-6">
                        <h1 className="text-3xl font-bold text-gray-900">Zgłoszenia znalezionych gołębi</h1>
                        <p className="mt-2 text-sm text-gray-600">
                            Dane kontaktowe znalazców są widoczne wyłącznie na tej stronie. Usuń zgłoszenie,
                            gdy przestanie być potrzebne.
                        </p>
                    </div>

                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
                        <div className="flex flex-col lg:flex-row lg:items-end gap-4">
                            <div className="grow">
                                <span className="block text-sm font-medium text-gray-700 mb-2">Status</span>
                                <div className="flex flex-wrap gap-2">
                                    {STATUS_FILTERS.map(filter => (
                                        <button
                                            key={filter.label}
                                            type="button"
                                            disabled={isMutating}
                                            onClick={() => applyFilters(filter.value, appliedSearch)}
                                            aria-pressed={statusFilter === filter.value}
                                            className={`px-3 py-1.5 rounded-md text-sm font-medium border transition whitespace-nowrap disabled:opacity-50 disabled:cursor-not-allowed ${
                                                statusFilter === filter.value
                                                    ? 'bg-blue-600 text-white border-blue-600'
                                                    : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-100'
                                            }`}
                                        >
                                            {filter.label}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <form
                                className="flex gap-2 lg:w-96"
                                onSubmit={event => {
                                    event.preventDefault();
                                    applyFilters(statusFilter, searchTerm);
                                }}
                            >
                                <div className="grow">
                                    <label htmlFor="ring-search" className="block text-sm font-medium text-gray-700 mb-2">
                                        Numer obrączki
                                    </label>
                                    <input
                                        id="ring-search"
                                        type="search"
                                        value={searchTerm}
                                        onChange={event => setSearchTerm(event.target.value)}
                                        placeholder="np. PL-0369-24 lub 0369241234"
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                    />
                                </div>
                                <button
                                    type="submit"
                                    className="self-end px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-semibold hover:bg-blue-700 transition whitespace-nowrap"
                                >
                                    Szukaj
                                </button>
                            </form>
                        </div>
                    </div>

                    {isLoading ? (
                        <LoadingState />
                    ) : loadError ? (
                        <ErrorState message={loadError} onRetry={() => void fetchReports(page, statusFilter, appliedSearch)} />
                    ) : reports.length === 0 ? (
                        <EmptyState>Brak zgłoszeń spełniających wybrane kryteria.</EmptyState>
                    ) : (
                        <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
                            <div className="xl:col-span-3">
                                <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200 text-sm">
                                        <thead className="bg-gray-50">
                                            <tr>
                                                <th scope="col" className="px-4 py-3 text-left font-semibold text-gray-700">Obrączka</th>
                                                <th scope="col" className="px-4 py-3 text-left font-semibold text-gray-700">Status</th>
                                                <th scope="col" className="px-4 py-3 text-left font-semibold text-gray-700">Miejsce</th>
                                                <th scope="col" className="px-4 py-3 text-left font-semibold text-gray-700">Zgłoszono</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-100">
                                            {reports.map(report => (
                                                <tr
                                                    key={report.id}
                                                    className={`transition ${
                                                        selected?.id === report.id
                                                            ? 'bg-blue-50'
                                                            : 'hover:bg-blue-50/40'
                                                    }`}
                                                >
                                                    <td className="px-4 py-3 [overflow-wrap:anywhere]">
                                                        <button
                                                            type="button"
                                                            onClick={() => openDetails(report)}
                                                            className="font-semibold text-blue-700 hover:text-blue-900 hover:underline text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded"
                                                        >
                                                            {report.ringNumber}
                                                        </button>
                                                    </td>
                                                    <td className="px-4 py-3">
                                                        <StatusBadge status={report.status} />
                                                    </td>
                                                    <td className="px-4 py-3 text-gray-700 [overflow-wrap:anywhere]">
                                                        {[report.foundLocation, report.foundCountry].filter(Boolean).join(', ') || '—'}
                                                    </td>
                                                    <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                                                        {formatGlobalDate(report.createdAt)}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                <div className="flex flex-wrap items-center justify-between gap-3 mt-4">
                                    <span className="text-sm text-gray-600">Zgłoszeń: {totalElements}</span>
                                    {totalPages > 1 && (
                                        <div className="flex items-center gap-3">
                                            <button
                                                onClick={() => changePage(page - 1)}
                                                disabled={isMutating || page === 0}
                                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                            >
                                                Poprzednia
                                            </button>
                                            <span className="text-sm text-gray-600">Strona {page + 1} z {totalPages}</span>
                                            <button
                                                onClick={() => changePage(page + 1)}
                                                disabled={isMutating || page >= totalPages - 1}
                                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                            >
                                                Następna
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>

                            <div className="xl:col-span-2">
                                {selected ? (
                                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
                                        <div className="flex items-start justify-between gap-3 mb-4">
                                            <div className="min-w-0">
                                                <h2 className="text-lg font-bold text-gray-900 [overflow-wrap:anywhere]">
                                                    {selected.ringNumber}
                                                </h2>
                                                <p className="text-xs text-gray-500 mt-1">
                                                    Zgłoszono {formatGlobalDate(selected.createdAt)}
                                                    {selected.updatedAt && ` · zmieniono ${formatGlobalDate(selected.updatedAt)}`}
                                                </p>
                                            </div>
                                            <StatusBadge status={selected.status} />
                                        </div>

                                        <dl className="text-sm border-t border-gray-100 pt-3">
                                            <div className="flex justify-between gap-4 py-2 border-b border-gray-100">
                                                <dt className="text-gray-600">Język znalazcy</dt>
                                                <dd className="font-semibold text-gray-900">
                                                    {formatReportLanguage(selected.preferredLanguage)}
                                                </dd>
                                            </div>
                                            <div className="flex justify-between gap-4 py-2 border-b border-gray-100">
                                                <dt className="text-gray-600">Telefon</dt>
                                                <dd className="font-semibold text-gray-900 [overflow-wrap:anywhere] text-right">
                                                    {selected.contactPhone
                                                        ? <a href={`tel:${selected.contactPhone.replace(/\s/g, '')}`} className="hover:text-blue-700">{selected.contactPhone}</a>
                                                        : '—'}
                                                </dd>
                                            </div>
                                            <div className="flex justify-between gap-4 py-2 border-b border-gray-100">
                                                <dt className="text-gray-600">E-mail</dt>
                                                <dd className="font-semibold text-gray-900 [overflow-wrap:anywhere] text-right">
                                                    {selected.contactEmail
                                                        ? <a href={`mailto:${selected.contactEmail}`} className="hover:text-blue-700">{selected.contactEmail}</a>
                                                        : '—'}
                                                </dd>
                                            </div>
                                            <div className="flex justify-between gap-4 py-2 border-b border-gray-100">
                                                <dt className="text-gray-600">Miejsce</dt>
                                                <dd className="font-semibold text-gray-900 [overflow-wrap:anywhere] text-right">
                                                    {[selected.foundLocation, selected.foundCountry].filter(Boolean).join(', ') || '—'}
                                                </dd>
                                            </div>
                                        </dl>

                                        {selected.description && (
                                            <div className="mt-4">
                                                <h3 className="text-sm font-semibold text-gray-700 mb-1">Opis od znalazcy</h3>
                                                <p className="text-sm text-gray-700 bg-gray-50 border border-gray-200 rounded-md p-3 whitespace-pre-wrap [overflow-wrap:anywhere]">
                                                    {selected.description}
                                                </p>
                                            </div>
                                        )}

                                        {/* Dozwolone przejścia statusów; pusta lista oznacza sprawę zamkniętą. */}
                                        <div className="mt-5">
                                            <h3 className="text-sm font-semibold text-gray-700 mb-2">Obsługa zgłoszenia</h3>
                                            {ALLOWED_STATUS_TRANSITIONS[selected.status].length > 0 ? (
                                                <div className="flex flex-wrap gap-2">
                                                    {ALLOWED_STATUS_TRANSITIONS[selected.status].map(status => (
                                                        <button
                                                            key={status}
                                                            type="button"
                                                            disabled={isMutating}
                                                            onClick={() => void handleStatusChange(selected, status)}
                                                            className={`px-3 py-1.5 rounded-md text-sm font-semibold text-white transition disabled:opacity-50 disabled:cursor-not-allowed ${
                                                                status === 'REJECTED' ? 'bg-gray-600 hover:bg-gray-700' : 'bg-blue-600 hover:bg-blue-700'
                                                            }`}
                                                        >
                                                            {formatStatusAction(status)}
                                                        </button>
                                                    ))}
                                                </div>
                                            ) : (
                                                <p className="text-sm text-gray-500">
                                                    Sprawa jest zamknięta — statusu nie można już zmienić.
                                                </p>
                                            )}
                                        </div>

                                        <div className="mt-5">
                                            <label htmlFor="admin-note" className="block text-sm font-semibold text-gray-700 mb-1">
                                                Notatka administratora
                                            </label>
                                            <p className="text-xs text-gray-500 mb-2">
                                                Widoczna wyłącznie dla administratorów, np. ustalony właściciel lub przyczyna odrzucenia.
                                            </p>
                                            <textarea
                                                id="admin-note"
                                                rows={4}
                                                maxLength={2000}
                                                value={noteDraft}
                                                onChange={event => setNoteDraft(event.target.value)}
                                                className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-y"
                                            />
                                            <div className="flex justify-end mt-2">
                                                <button
                                                    type="button"
                                                    disabled={isMutating}
                                                    onClick={() => void handleSaveNote()}
                                                    className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-semibold hover:bg-blue-700 transition disabled:bg-gray-300 disabled:cursor-not-allowed"
                                                >
                                                    {isSavingNote ? 'Zapisywanie…' : 'Zapisz notatkę'}
                                                </button>
                                            </div>
                                        </div>

                                        {detailsError && (
                                            <p role="alert" className="mt-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2">
                                                {detailsError}
                                            </p>
                                        )}

                                        <div className="mt-5 pt-4 border-t border-gray-100 flex justify-end">
                                            <button
                                                type="button"
                                                disabled={isMutating}
                                                onClick={() => handleDelete(selected)}
                                                className="px-4 py-2 border border-red-200 text-red-700 rounded-md text-sm font-medium hover:bg-red-50 transition disabled:opacity-50 disabled:cursor-not-allowed"
                                            >
                                                Usuń zgłoszenie
                                            </button>
                                        </div>
                                    </div>
                                ) : (
                                    <EmptyState>Wybierz zgłoszenie z listy, aby zobaczyć dane kontaktowe znalazcy.</EmptyState>
                                )}
                            </div>
                        </div>
                    )}
                </main>

                <Footer />

                <ConfirmModal
                    isOpen={modalConfig.isOpen}
                    title={modalConfig.title}
                    message={modalConfig.message}
                    isAlert={modalConfig.isAlert}
                    onConfirm={modalConfig.onConfirm}
                    onCancel={closeModal}
                />
            </div>
        </AdminGuard>
    );
}
