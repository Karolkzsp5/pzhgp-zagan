"use client";

import { use, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import dynamic from 'next/dynamic';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import AuthGuard from '@/app/components/AuthGuard';
import ConfirmModal from '@/app/components/ConfirmModal';
import FlightProfileChart from '@/app/components/FlightProfileChart';
import { flightService } from '@/app/services/flightService';
import {
    FlightDetailsDto,
    formatDateTime,
    formatDuration,
    formatSpeed,
    formatTime
} from '@/app/types/flight';
import { SPEED_RAMP, computeSpeedThresholds, speedLegendLabels } from '@/app/utils/flightScale';

/**
 * Leaflet operuje bezpośrednio na obiekcie window, dlatego mapa ładowana jest wyłącznie
 * po stronie przeglądarki. Renderowanie na serwerze kończyłoby się błędem "window is not defined".
 */
const FlightMap = dynamic(() => import('@/app/components/FlightMap'), {
    ssr: false,
    loading: () => (
        <div className="h-full flex items-center justify-center bg-gray-100">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-700"></div>
        </div>
    )
});

/** Prędkości odtwarzania trasy — liczba punktów pokazywanych na sekundę. */
const PLAYBACK_SPEEDS = [8, 25, 60];

function StatTile({ label, value, unit, hint }: {
    label: string;
    value: string;
    unit?: string;
    hint?: string;
}) {
    return (
        <div className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">{label}</p>
            <p className="mt-1 text-2xl font-bold text-gray-900 [overflow-wrap:anywhere]">
                {value}
                {unit && <span className="text-base font-semibold text-gray-500 ml-1">{unit}</span>}
            </p>
            {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
        </div>
    );
}

/**
 * Wiersz tabeli szczegółów. Wartości liczbowe pozostają w jednej linii, natomiast
 * {@code wrapValue} pozwala zawinąć długi tekst — na przykład nazwę wgranego pliku,
 * która potrafi mieć kilkadziesiąt znaków bez spacji.
 */
function StatRow({ label, value, hint, wrapValue = false }: {
    label: string;
    value: string;
    hint?: string;
    wrapValue?: boolean;
}) {
    return (
        <div
            className={`py-2 border-b border-gray-100 last:border-0 gap-x-4 ${
                wrapValue
                    ? 'flex flex-col sm:flex-row sm:items-baseline sm:justify-between'
                    : 'flex items-baseline justify-between'
            }`}
        >
            <div className="min-w-0">
                <span className="text-sm text-gray-600">{label}</span>
                {hint && <span className="block text-xs text-gray-400">{hint}</span>}
            </div>
            <span
                className={`text-sm font-semibold text-gray-900 min-w-0 ${
                    wrapValue
                        ? '[overflow-wrap:anywhere] sm:text-right'
                        : 'text-right whitespace-nowrap'
                }`}
            >
                {value}
            </span>
        </div>
    );
}

export default function FlightDetailsPage({ params }: { params: Promise<{ flightId: string }> }) {
    const resolvedParams = use(params);
    const flightId = Number(resolvedParams.flightId);
    const router = useRouter();

    /** Poprawność identyfikatora wynika wprost z adresu, więc nie wymaga stanu komponentu. */
    const isValidId = Number.isFinite(flightId);

    const [flight, setFlight] = useState<FlightDetailsDto | null>(null);
    const [isLoading, setIsLoading] = useState(isValidId);
    const [error, setError] = useState('');

    const [showStraightLine, setShowStraightLine] = useState(true);

    const [playbackPosition, setPlaybackPosition] = useState(0);
    const [isPlaying, setIsPlaying] = useState(false);
    const [playbackSpeed, setPlaybackSpeed] = useState(PLAYBACK_SPEEDS[1]);
    const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const closeModal = () => setModalConfig(prev => ({ ...prev, isOpen: false }));

    useEffect(() => {
        if (!isValidId) return;

        let cancelled = false;

        flightService
            .getFlight(flightId)
            .then(data => {
                if (!cancelled) setFlight(data);
            })
            .catch(caught => {
                if (!cancelled) setError(caught instanceof Error ? caught.message : 'Nie udało się pobrać lotu.');
            })
            .finally(() => {
                if (!cancelled) setIsLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [flightId, isValidId]);

    /** Odtwarzacz przechodzi przez całą zarejestrowaną trasę. */
    const trackPoints = useMemo(() => flight?.trackPoints ?? [], [flight]);

    const pointIndexes = useMemo(() => trackPoints.map((_, index) => index), [trackPoints]);

    const speedLegend = useMemo(
        () => speedLegendLabels(computeSpeedThresholds(trackPoints)),
        [trackPoints]
    );

    // Odtwarzanie trasy: znacznik przesuwa się po kolejnych punktach fazy lotu.
    const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

    useEffect(() => {
        if (!isPlaying || pointIndexes.length === 0) return;

        intervalRef.current = setInterval(() => {
            setPlaybackPosition(previous => {
                if (previous >= pointIndexes.length - 1) {
                    setIsPlaying(false);
                    return previous;
                }
                return previous + 1;
            });
        }, 1000 / playbackSpeed);

        return () => {
            if (intervalRef.current) clearInterval(intervalRef.current);
        };
    }, [isPlaying, playbackSpeed, pointIndexes.length]);

    const togglePlayback = useCallback(() => {
        setIsPlaying(previous => {
            if (!previous && playbackPosition >= pointIndexes.length - 1) {
                setPlaybackPosition(0);
            }
            return !previous;
        });
    }, [playbackPosition, pointIndexes.length]);

    /**
     * Kliknięcie w wykres ustawia pozycję na pasku odtwarzania, dzięki czemu oba sposoby
     * wskazywania punktu trasy — wykres i suwak — działają wymiennie i pozostają zgodne.
     */
    const selectPointFromChart = useCallback((originalIndex: number | null) => {
        if (originalIndex === null) return;

        const position = pointIndexes.indexOf(originalIndex);
        if (position < 0) return;

        setIsPlaying(false);
        setPlaybackPosition(position);
        setHoveredIndex(null);
    }, [pointIndexes]);

    const handleDelete = () => {
        if (!flight) return;

        setModalConfig({
            isOpen: true,
            title: 'Usuń lot',
            message: `Czy na pewno chcesz usunąć lot "${flight.name}" wraz z zapisaną trasą? Tej operacji nie można cofnąć.`,
            isAlert: false,
            onConfirm: async () => {
                closeModal();
                try {
                    await flightService.deleteFlight(flight.id);
                    router.push('/flights');
                } catch (caught) {
                    setModalConfig({
                        isOpen: true,
                        title: 'Błąd',
                        message: caught instanceof Error ? caught.message : 'Nie udało się usunąć lotu.',
                        isAlert: true,
                        onConfirm: closeModal
                    });
                }
            }
        });
    };

    const activeIndex = hoveredIndex ?? (pointIndexes.length > 0 ? pointIndexes[playbackPosition] : null);
    const activePoint = flight && activeIndex !== null ? flight.trackPoints[activeIndex] : null;

    if (isLoading) {
        return (
            <AuthGuard>
                <div className="min-h-screen bg-gray-50 flex flex-col">
                    <Navbar />
                    <div className="grow flex items-center justify-center">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
                    </div>
                    <Footer />
                </div>
            </AuthGuard>
        );
    }

    if (!isValidId || error || !flight) {
        return (
            <AuthGuard>
                <div className="min-h-screen bg-gray-50 flex flex-col">
                    <Navbar />
                    <main className="grow max-w-3xl mx-auto w-full py-16 px-4 text-center">
                        <h1 className="text-2xl font-bold text-gray-900 mb-3">Nie udało się otworzyć lotu</h1>
                        <p className="text-sm text-gray-600 mb-6">
                            {!isValidId ? 'Nieprawidłowy identyfikator lotu.' : error || 'Lot nie istnieje.'}
                        </p>
                        <Link
                            href="/flights"
                            className="inline-block px-5 py-2.5 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition"
                        >
                            Wróć do listy lotów
                        </Link>
                    </main>
                    <Footer />
                </div>
            </AuthGuard>
        );
    }

    const stats = flight.statistics;

    return (
        <AuthGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <Link
                        href="/flights"
                        className="inline-flex items-center gap-1 text-sm text-blue-700 hover:text-blue-900 hover:underline mb-4"
                    >
                        <svg className="w-4 h-4" viewBox="0 -960 960 960" fill="currentColor">
                            <path d="M400-80 0-480l400-400 71 71-329 329 329 329-71 71Z"/>
                        </svg>
                        Wszystkie loty
                    </Link>

                    <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 border-b border-gray-200 pb-5 mb-6">
                        <div className="min-w-0">
                            <h1 className="text-3xl font-bold text-gray-900 [overflow-wrap:anywhere]">{flight.name}</h1>
                            <p className="mt-2 text-sm text-gray-600 [overflow-wrap:anywhere]">
                                {flight.ringNumber && (
                                    <>Obrączka <strong className="text-gray-900">{flight.ringNumber}</strong> · </>
                                )}
                                Hodowca: {flight.ownerName}
                                {flight.releaseSite && <> · Wypuszczenie: {flight.releaseSite}</>}
                            </p>
                        </div>

                        {flight.canDelete && (
                            <button
                                onClick={handleDelete}
                                className="self-start px-4 py-2 border border-red-200 text-red-700 rounded-md text-sm font-medium hover:bg-red-50 transition whitespace-nowrap"
                            >
                                Usuń lot
                            </button>
                        )}
                    </div>

                    {/* Kluczowe liczby */}
                    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
                        <StatTile
                            label="Dystans"
                            value={stats.straightLineDistanceKm.toFixed(2)}
                            unit="km"
                            hint="w linii prostej, start–meta"
                        />
                        <StatTile
                            label="Czas nagrania"
                            value={stats.timestampsAvailable ? formatDuration(stats.durationSeconds) : '—'}
                            hint={
                                stats.timestampsAvailable
                                    ? `${formatTime(flight.startTime)} – ${formatTime(flight.endTime)}`
                                    : 'plik bez znaczników czasu'
                            }
                        />
                        <StatTile
                            label="Prędkość średnia"
                            value={
                                stats.timestampsAvailable
                                    ? Math.round(stats.averageSpeedMetersPerMinute).toLocaleString('pl-PL')
                                    : '—'
                            }
                            unit={stats.timestampsAvailable ? 'm/min' : undefined}
                            hint="wzdłuż zarejestrowanej trasy"
                        />
                        <StatTile
                            label="Prędkość maksymalna"
                            value={
                                stats.timestampsAvailable
                                    ? Math.round(stats.maxSpeedMetersPerMinute).toLocaleString('pl-PL')
                                    : '—'
                            }
                            unit={stats.timestampsAvailable ? 'm/min' : undefined}
                            hint="utrzymana przez co najmniej 15 s"
                        />
                    </div>

                    {/* Mapa trasy */}
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden mb-8">
                        <div className="h-[460px] sm:h-[560px]">
                            <FlightMap
                                flight={flight}
                                activeIndex={activeIndex}
                                showStraightLine={showStraightLine}
                            />
                        </div>

                        {/* Legenda i przełączniki warstw */}
                        <div className="border-t border-gray-100 p-4 flex flex-col lg:flex-row lg:items-center gap-4 lg:gap-8">
                            <div>
                                <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">
                                    Prędkość na trasie [m/min]
                                </p>
                                <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
                                    {SPEED_RAMP.map((color, index) => (
                                        <span key={color} className="flex items-center gap-1.5 text-xs text-gray-600">
                                            <span
                                                className="inline-block w-5 h-2 rounded-sm"
                                                style={{ backgroundColor: color }}
                                            />
                                            {speedLegend[index]}
                                        </span>
                                    ))}
                                </div>
                            </div>

                            <div className="flex flex-wrap gap-x-5 gap-y-2 lg:ml-auto">
                                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                                    <input
                                        type="checkbox"
                                        checked={showStraightLine}
                                        onChange={event => setShowStraightLine(event.target.checked)}
                                        className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                                    />
                                    Linia prosta start-meta
                                </label>
                            </div>
                        </div>

                        {/* Odtwarzanie trasy */}
                        {pointIndexes.length > 1 && (
                            <div className="border-t border-gray-100 p-4 bg-gray-50">
                                <div className="flex items-center gap-4">
                                    <button
                                        onClick={togglePlayback}
                                        className="shrink-0 w-10 h-10 rounded-full bg-blue-600 text-white flex items-center justify-center hover:bg-blue-700 transition"
                                        aria-label={isPlaying ? 'Zatrzymaj odtwarzanie trasy' : 'Odtwórz trasę'}
                                    >
                                        {isPlaying ? (
                                            <svg className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                                                <path d="M520-200v-560h240v560H520Zm-320 0v-560h240v560H200Z"/>
                                            </svg>
                                        ) : (
                                            <svg className="w-5 h-5 ml-0.5" viewBox="0 -960 960 960" fill="currentColor">
                                                <path d="M320-200v-560l440 280-440 280Z"/>
                                            </svg>
                                        )}
                                    </button>

                                    <input
                                        type="range"
                                        min={0}
                                        max={pointIndexes.length - 1}
                                        value={playbackPosition}
                                        onChange={event => {
                                            setIsPlaying(false);
                                            setPlaybackPosition(Number(event.target.value));
                                        }}
                                        className="grow accent-blue-600"
                                        aria-label="Pozycja na trasie lotu"
                                    />

                                    <select
                                        value={playbackSpeed}
                                        onChange={event => setPlaybackSpeed(Number(event.target.value))}
                                        className="shrink-0 text-sm border border-gray-300 rounded-md px-2 py-1.5 text-gray-700 bg-white"
                                        aria-label="Prędkość odtwarzania"
                                    >
                                        {PLAYBACK_SPEEDS.map(speed => (
                                            <option key={speed} value={speed}>
                                                {speed === PLAYBACK_SPEEDS[0] ? 'wolno' : speed === PLAYBACK_SPEEDS[1] ? 'normalnie' : 'szybko'}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {activePoint && (
                                    <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-600">
                                        <span>
                                            Godzina: <strong className="text-gray-900">{formatTime(activePoint.time)}</strong>
                                        </span>
                                        {activePoint.speedMetersPerMinute !== null && (
                                            <span>
                                                Prędkość:{' '}
                                                <strong className="text-gray-900">
                                                    {formatSpeed(activePoint.speedMetersPerMinute)}
                                                </strong>
                                            </span>
                                        )}
                                        {activePoint.elevation !== null && (
                                            <span>
                                                Wysokość: <strong className="text-gray-900">{Math.round(activePoint.elevation)} m n.p.m.</strong>
                                            </span>
                                        )}
                                        <span className="text-gray-400">
                                            punkt {playbackPosition + 1} z {pointIndexes.length}
                                        </span>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Profile wysokości i prędkości */}
                        <div className="lg:col-span-2 bg-white rounded-lg shadow-sm border border-gray-200 p-5 self-start">
                            <h2 className="text-lg font-bold text-gray-900 mb-4">Przebieg lotu</h2>

                            <FlightProfileChart
                                points={trackPoints}
                                originalIndexes={pointIndexes}
                                activeIndex={activeIndex}
                                onHover={setHoveredIndex}
                                onSelect={selectPointFromChart}
                            />
                        </div>

                        {/* Szczegółowe statystyki */}
                        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5">
                            <h2 className="text-lg font-bold text-gray-900 mb-3">Szczegóły lotu</h2>

                            <StatRow label="Początek nagrania" value={formatDateTime(flight.startTime)} />
                            <StatRow label="Koniec nagrania" value={formatDateTime(flight.endTime)} />
                            <StatRow
                                label="Pokonany dystans"
                                value={`${stats.trackDistanceKm.toFixed(2)} km`}
                                hint="wzdłuż zarejestrowanej trasy"
                            />
                            <StatRow
                                label="Prędkość w linii prostej"
                                value={stats.timestampsAvailable
                                    ? formatSpeed(stats.straightLineSpeedMetersPerMinute)
                                    : '—'}
                                hint="dystans start–meta podzielony przez czas nagrania"
                            />
                            {stats.minElevationMeters !== null && stats.maxElevationMeters !== null && (
                                <StatRow
                                    label="Wysokość lotu"
                                    value={`${Math.round(stats.minElevationMeters)}–${Math.round(stats.maxElevationMeters)} m n.p.m.`}
                                    hint={
                                        stats.elevationGainMeters !== null
                                            ? `suma wznosów ${Math.round(stats.elevationGainMeters)} m`
                                            : undefined
                                    }
                                />
                            )}

                            <StatRow
                                label="Punkty trasy"
                                value={`${stats.totalPoints}`}
                                hint={`na mapie wyświetlono ${flight.returnedPoints}`}
                            />
                            <StatRow label="Plik źródłowy" value={flight.originalFileName} wrapValue />
                        </div>
                    </div>

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