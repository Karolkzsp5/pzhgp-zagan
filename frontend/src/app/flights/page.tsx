"use client";

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import AuthGuard from '@/app/components/AuthGuard';
import ConfirmModal from '@/app/components/ConfirmModal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import EmptyState from '@/app/components/EmptyState';
import { flightService } from '@/app/services/flightService';
import { FlightSummaryDto, formatDateTime, formatDuration, formatSpeed } from '@/app/types/flight';

const PAGE_SIZE = 10;
const MAX_GPX_FILE_SIZE = 10 * 1024 * 1024;

export default function FlightsPage() {
    const router = useRouter();
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [flights, setFlights] = useState<FlightSummaryDto[]>([]);
    const [totalPages, setTotalPages] = useState(0);
    const [page, setPage] = useState(0);
    const [isLoading, setIsLoading] = useState(true);
    const [listError, setListError] = useState('');

    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [isDragging, setIsDragging] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadError, setUploadError] = useState('');

    const [name, setName] = useState('');
    const [ringNumber, setRingNumber] = useState('');
    const [releaseSite, setReleaseSite] = useState('');

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const closeModal = () => setModalConfig(prev => ({ ...prev, isOpen: false }));

    const showAlert = (title: string, message: string) =>
        setModalConfig({ isOpen: true, title, message, isAlert: true, onConfirm: closeModal });

    const fetchFlights = useCallback(async (targetPage: number) => {
        setIsLoading(true);
        setListError('');

        try {
            const result = await flightService.getMyFlights(targetPage, PAGE_SIZE);
            setFlights(result.content);
            setTotalPages(result.totalPages);
            setPage(result.number);
        } catch (error) {
            setListError(error instanceof Error ? error.message : 'Nie udało się pobrać listy lotów.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        void fetchFlights(0);
    }, [fetchFlights]);

    const selectFile = (file: File | null) => {
        setUploadError('');

        if (!file) {
            setSelectedFile(null);
            return;
        }

        if (!file.name.toLowerCase().endsWith('.gpx')) {
            setUploadError('Wybierz plik z rozszerzeniem .gpx (eksport z programu obsługującego obrączki).');
            setSelectedFile(null);
            return;
        }

        if (file.size > MAX_GPX_FILE_SIZE) {
            setUploadError('Plik GPX może mieć maksymalnie 10 MB.');
            setSelectedFile(null);
            return;
        }

        setSelectedFile(file);
    };

    const handleDrop = (event: React.DragEvent<HTMLLabelElement>) => {
        event.preventDefault();
        setIsDragging(false);
        selectFile(event.dataTransfer.files?.[0] ?? null);
    };

    const handleUpload = async (event: React.FormEvent) => {
        event.preventDefault();
        if (!selectedFile || isUploading) return;

        setIsUploading(true);
        setUploadError('');

        try {
            const flightId = await flightService.uploadFlight(selectedFile, {
                name: name.trim() || null,
                ringNumber: ringNumber.trim() || null,
                releaseSite: releaseSite.trim() || null
            });

            router.push(`/flights/${flightId}`);
        } catch (error) {
            setUploadError(error instanceof Error ? error.message : 'Nie udało się wgrać pliku GPX.');
            setIsUploading(false);
        }
    };

    const handleDelete = (flight: FlightSummaryDto) => {
        setModalConfig({
            isOpen: true,
            title: 'Usuń lot',
            message: `Czy na pewno chcesz usunąć lot "${flight.name}" wraz z zapisaną trasą? Tej operacji nie można cofnąć.`,
            isAlert: false,
            onConfirm: async () => {
                closeModal();
                try {
                    await flightService.deleteFlight(flight.id);
                    const isLastOnPage = flights.length === 1 && page > 0;
                    await fetchFlights(isLastOnPage ? page - 1 : page);
                } catch (error) {
                    showAlert('Błąd', error instanceof Error ? error.message : 'Nie udało się usunąć lotu.');
                }
            }
        });
    };

    return (
        <AuthGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <div className="border-b border-gray-200 pb-5 mb-8">
                        <h1 className="text-3xl font-bold text-gray-900">Mapy lotów</h1>
                        <p className="mt-2 text-sm text-gray-600">
                            Wgraj plik <code className="text-xs bg-gray-100 px-1 py-0.5 rounded">.gpx</code> z obrączki
                            lokalizacyjnej, aby zobaczyć trasę gołębia na mapie oraz statystyki lotu.
                        </p>
                    </div>

                    {/* Formularz wgrywania pliku */}
                    <form
                        onSubmit={handleUpload}
                        className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-10"
                    >
                        <h2 className="text-lg font-bold text-gray-900 mb-4">Wgraj nowy lot</h2>

                        <label
                            onDragOver={event => {
                                event.preventDefault();
                                setIsDragging(true);
                            }}
                            onDragLeave={() => setIsDragging(false)}
                            onDrop={handleDrop}
                            className={`flex flex-col items-center justify-center w-full border-2 border-dashed rounded-lg p-8 cursor-pointer transition ${
                                isDragging
                                    ? 'border-blue-500 bg-blue-50'
                                    : 'border-gray-300 bg-gray-50 hover:border-blue-400 hover:bg-blue-50/40'
                            }`}
                        >
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept=".gpx"
                                className="sr-only"
                                onChange={event => selectFile(event.target.files?.[0] ?? null)}
                            />

                            <svg className="w-10 h-10 text-gray-400 mb-3" viewBox="0 -960 960 960" fill="currentColor">
                                <path d="M260-160q-91 0-155.5-63T40-377q0-78 47-139t123-78q25-92 100-149t170-57q117 0 198.5 81.5T760-520q69 8 114.5 59.5T920-340q0 75-52.5 127.5T740-160H520q-33 0-56.5-23.5T440-240v-206l-64 62-56-56 160-160 160 160-56 56-64-62v206h220q42 0 71-29t29-71q0-42-29-71t-71-29h-60v-80q0-83-58.5-141.5T480-720q-83 0-141.5 58.5T280-520h-20q-58 0-99 41t-41 99q0 58 41 99t99 41h100v80H260Zm220-280Z"/>
                            </svg>

                            {selectedFile ? (
                                <>
                                    <span className="text-sm font-semibold text-gray-900 text-center max-w-full [overflow-wrap:anywhere]">
                                        {selectedFile.name}
                                    </span>
                                    <span className="text-xs text-gray-500 mt-1 text-center">
                                        {(selectedFile.size / 1024).toFixed(0)} kB — kliknij, aby wybrać inny plik
                                    </span>
                                </>
                            ) : (
                                <>
                                    <span className="text-sm font-semibold text-gray-700">
                                        Przeciągnij plik .gpx lub kliknij, aby wybrać
                                    </span>
                                    <span className="text-xs text-gray-500 mt-1">Maksymalny rozmiar pliku: 10 MB</span>
                                </>
                            )}
                        </label>

                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-5">
                            <div>
                                <label htmlFor="flight-name" className="block text-sm font-medium text-gray-700 mb-1">
                                    Nazwa lotu <span className="text-gray-400 font-normal">(opcjonalnie)</span>
                                </label>
                                <input
                                    id="flight-name"
                                    type="text"
                                    maxLength={150}
                                    value={name}
                                    onChange={event => setName(event.target.value)}
                                    placeholder="np. Lot konkursowy Dessau"
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                />
                            </div>

                            <div>
                                <label htmlFor="ring-number" className="block text-sm font-medium text-gray-700 mb-1">
                                    Numer obrączki <span className="text-gray-400 font-normal">(opcjonalnie)</span>
                                </label>
                                <input
                                    id="ring-number"
                                    type="text"
                                    maxLength={32}
                                    value={ringNumber}
                                    onChange={event => setRingNumber(event.target.value)}
                                    placeholder="odczytany z pliku"
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                />
                            </div>

                            <div>
                                <label htmlFor="release-site" className="block text-sm font-medium text-gray-700 mb-1">
                                    Miejsce wypuszczenia <span className="text-gray-400 font-normal">(opcjonalnie)</span>
                                </label>
                                <input
                                    id="release-site"
                                    type="text"
                                    maxLength={150}
                                    value={releaseSite}
                                    onChange={event => setReleaseSite(event.target.value)}
                                    placeholder="np. Dessau"
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                                />
                            </div>
                        </div>

                        {uploadError && (
                            <p role="alert" className="mt-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">
                                {uploadError}
                            </p>
                        )}

                        <div className="mt-5 flex justify-end">
                            <button
                                type="submit"
                                disabled={!selectedFile || isUploading}
                                className="px-5 py-2.5 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center gap-2"
                            >
                                {isUploading && (
                                    <span className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></span>
                                )}
                                {isUploading ? 'Analizuję trasę…' : 'Wgraj i przeanalizuj'}
                            </button>
                        </div>
                    </form>

                    {/* Lista wgranych lotów */}
                    <h2 className="text-lg font-bold text-gray-900 mb-4">Moje loty</h2>

                    {isLoading ? (
                        <LoadingState />
                    ) : listError ? (
                        <ErrorState message={listError} onRetry={() => void fetchFlights(page)} />
                    ) : flights.length === 0 ? (
                        <EmptyState>Nie masz jeszcze zapisanych lotów. Wgraj pierwszy plik GPX, aby zobaczyć trasę na mapie.</EmptyState>
                    ) : (
                        <>
                            <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200 text-sm">
                                    <thead className="bg-blue-100">
                                        <tr>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-700">Lot</th>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-700">Obrączka</th>
                                            <th className="px-4 py-3 text-right font-semibold text-gray-700">Dystans</th>
                                            <th className="px-4 py-3 text-right font-semibold text-gray-700">Czas nagrania</th>
                                            <th className="px-4 py-3 text-right font-semibold text-gray-700">Prędkość</th>
                                            <th className="px-4 py-3 text-left font-semibold text-gray-700">Wgrano</th>
                                            <th className="px-4 py-3"></th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {flights.map(flight => (
                                            <tr key={flight.id} className="hover:bg-blue-50/40 transition">
                                                <td className="px-4 py-3 min-w-[16rem] max-w-[22rem]">
                                                    <Link
                                                        href={`/flights/${flight.id}`}
                                                        className="font-semibold text-blue-700 hover:text-blue-900 hover:underline [overflow-wrap:anywhere] line-clamp-2"
                                                    >
                                                        {flight.name}
                                                    </Link>
                                                    {flight.releaseSite && (
                                                        <span className="block text-xs text-gray-500 [overflow-wrap:anywhere]">
                                                            Wypuszczenie: {flight.releaseSite}
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="px-4 py-3 text-gray-700">{flight.ringNumber ?? '—'}</td>
                                                <td className="px-4 py-3 text-right text-gray-900 whitespace-nowrap">
                                                    {flight.straightLineDistanceKm.toFixed(2)} km
                                                </td>
                                                <td className="px-4 py-3 text-right text-gray-900 whitespace-nowrap">
                                                    {formatDuration(flight.durationSeconds)}
                                                </td>
                                                <td className="px-4 py-3 text-right whitespace-nowrap">
                                                    {flight.timestampsAvailable ? (
                                                        <span className="text-gray-900 font-semibold">
                                                            {formatSpeed(flight.averageSpeedMetersPerMinute)}
                                                        </span>
                                                    ) : (
                                                        <span className="text-gray-400" title="Plik nie zawiera znaczników czasu">
                                                            —
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                                                    {formatDateTime(flight.uploadedAt)}
                                                </td>
                                                <td className="px-4 py-3 text-right">
                                                    {flight.canDelete && (
                                                        <button
                                                            type="button"
                                                            onClick={() => handleDelete(flight)}
                                                            className="text-gray-400 hover:text-red-600 p-1 transition"
                                                            title="Usuń lot"
                                                            aria-label={`Usuń lot ${flight.name}`}
                                                        >
                                                            <svg className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                                                                <path d="M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z"/>
                                                            </svg>
                                                        </button>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            {totalPages > 1 && (
                                <div className="flex items-center justify-center gap-3 mt-6">
                                    <button
                                        type="button"
                                        onClick={() => void fetchFlights(page - 1)}
                                        disabled={page === 0}
                                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
                                    >
                                        Poprzednia
                                    </button>
                                    <span className="text-sm text-gray-600">
                                        Strona {page + 1} z {totalPages}
                                    </span>
                                    <button
                                        type="button"
                                        onClick={() => void fetchFlights(page + 1)}
                                        disabled={page >= totalPages - 1}
                                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
                                    >
                                        Następna
                                    </button>
                                </div>
                            )}
                        </>
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
        </AuthGuard>
    );
}