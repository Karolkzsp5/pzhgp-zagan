'use client';

import { Fragment, ReactNode, useCallback, useEffect, useId, useMemo, useState } from 'react';
import ErrorState from '@/app/components/ErrorState';
import LoadingState from '@/app/components/LoadingState';
import Modal from '@/app/components/Modal';
import useLatestRequest from '@/app/hooks/useLatestRequest';
import { flightPlanService } from '@/app/services/flightPlanService';
import { FlightPlanEntryDto, FlightResultSummaryDto } from '@/app/types/flightPlan';
import { getErrorMessage } from '@/app/utils/apiClient';
import { downloadFile } from '@/app/utils/download';
import { formatFlightResultLabel, formatLocalDate } from '@/app/utils/formatters';

// Stała wysokość, żeby okno nie zmieniało rozmiaru (a pole wyszukiwania położenia) podczas wpisywania.
const CONTENT_AREA_CLASS = 'h-[50vh] sm:h-[60vh] rounded-md border border-gray-200 bg-gray-50';

interface FlightResultPreviewModalProps {
    flight: FlightPlanEntryDto;
    result: FlightResultSummaryDto;
    onClose: () => void;
}

/**
 * Podgląd pliku z wynikami lotu. Kolumny w takich plikach są wyrównane spacjami,
 * więc treść jest pokazywana czcionką o stałej szerokości, bez zmiany układu.
 */
export default function FlightResultPreviewModal({ flight, result, onClose }: FlightResultPreviewModalProps) {
    const searchInputId = useId();
    const [content, setContent] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [query, setQuery] = useState('');

    const { beginRequest, isLatestRequest } = useLatestRequest();

    const loadContent = useCallback(async () => {
        const requestId = beginRequest();
        setIsLoading(true);
        setLoadError('');

        try {
            const text = await flightPlanService.getResultText(result.id);

            if (!isLatestRequest(requestId)) return;

            setContent(text);
        } catch (error) {
            if (!isLatestRequest(requestId)) return;

            setLoadError(getErrorMessage(error, 'Nie udało się pobrać wyników lotu.'));
        } finally {
            if (isLatestRequest(requestId)) {
                setIsLoading(false);
            }
        }
    }, [result.id, beginRequest, isLatestRequest]);

    useEffect(() => {
        void loadContent();
    }, [loadContent]);

    const lines = useMemo(() => content === null ? [] : splitLines(content), [content]);
    const searchPattern = useMemo(() => createSearchPattern(query), [query]);

    const matchingLines = useMemo(() => {
        if (!searchPattern) return lines;

        const matcher = new RegExp(searchPattern, 'iu');
        return lines.filter(line => matcher.test(line));
    }, [lines, searchPattern]);

    const handleDownload = () => {
        if (content === null) return;

        downloadFile(new Blob([content], { type: 'text/plain;charset=utf-8' }), result.originalFileName);
    };

    return (
        <Modal isOpen title={`Wyniki lotu ${flight.location}`} onClose={onClose} maxWidthClass="max-w-6xl">
            <div className="px-6 py-5 space-y-4">
                <div className="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm text-gray-600" data-cy="flight-result-details">
                    <span className="px-2.5 py-0.5 rounded-full bg-blue-100 text-blue-800 text-xs font-semibold">
                        {formatFlightResultLabel(result)}
                    </span>
                    <span>{formatLocalDate(flight.scheduledDate)}</span>
                    <span aria-hidden="true" className="text-gray-300">•</span>
                    <span>{flight.distanceKm} km</span>
                    <span aria-hidden="true" className="hidden sm:inline text-gray-300">•</span>
                    <span className="basis-full sm:basis-auto break-all text-gray-500">{result.originalFileName}</span>
                </div>

                {isLoading ? (
                    <LoadingState />
                ) : loadError ? (
                    <ErrorState message={loadError} onRetry={() => void loadContent()} />
                ) : (
                    <>
                        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-3">
                            <div className="w-full sm:max-w-sm">
                                <label htmlFor={searchInputId} className="block text-sm font-medium text-gray-700">
                                    Szukaj w wynikach
                                </label>
                                <input
                                    id={searchInputId}
                                    type="search"
                                    value={query}
                                    onChange={(event) => setQuery(event.target.value)}
                                    placeholder="np. nazwisko lub numer obrączki"
                                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 outline-none transition focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>

                            <button
                                type="button"
                                onClick={handleDownload}
                                className="px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md text-sm font-bold hover:bg-gray-100 transition shrink-0"
                            >
                                Pobierz plik
                            </button>
                        </div>

                        <p aria-live="polite" className="text-xs text-gray-500" data-cy="flight-result-search-status">
                            {searchPattern
                                ? `Znalezione wiersze: ${matchingLines.length}`
                                : 'Wpisz nazwisko lub numer obrączki, aby zobaczyć tylko pasujące wiersze.'}
                        </p>

                        {searchPattern && matchingLines.length === 0 ? (
                            <p className={`${CONTENT_AREA_CLASS} flex items-center justify-center px-4 text-center text-sm text-gray-500`}>
                                Brak wierszy zawierających podany tekst.
                            </p>
                        ) : (
                            <pre
                                tabIndex={0}
                                aria-label="Treść pliku z wynikami"
                                data-cy="flight-result-content"
                                className={`${CONTENT_AREA_CLASS} overflow-auto p-4 font-mono text-xs leading-5 text-gray-800 whitespace-pre focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500`}
                            >
                                {searchPattern
                                    ? matchingLines.map((line, index) => (
                                        <Fragment key={index}>
                                            {highlightMatches(line, searchPattern)}
                                            {'\n'}
                                        </Fragment>
                                    ))
                                    : lines.join('\n')}
                            </pre>
                        )}
                    </>
                )}
            </div>
        </Modal>
    );
}

/** Dzieli treść na wiersze; pomija znaki końca strony z wydruków i puste wiersze na końcu pliku. */
function splitLines(content: string): string[] {
    return content
        .replace(/\r\n?/g, '\n')
        .replace(/\f/g, '')
        .replace(/\n+$/, '')
        .split('\n');
}

/**
 * Wzorzec wyszukiwania bez znaków specjalnych wyrażeń regularnych. Odstępy w zapytaniu pasują
 * do dowolnej liczby spacji, bo kolumny w pliku są wyrównane spacjami ("Kowalski Jan" znajdzie "KOWALSKI   JAN").
 */
function createSearchPattern(query: string): string | null {
    const words = query.trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) return null;

    return words.map(word => word.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('\\s+');
}

function highlightMatches(line: string, searchPattern: string): ReactNode[] {
    return line.split(new RegExp(`(${searchPattern})`, 'giu')).map((part, index) =>
        index % 2 === 1
            ? <mark key={index} className="bg-yellow-200 text-gray-900 rounded-sm">{part}</mark>
            : part
    );
}
