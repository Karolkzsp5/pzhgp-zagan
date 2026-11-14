'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import ErrorState from '@/app/components/ErrorState';
import LoadingState from '@/app/components/LoadingState';
import Modal from '@/app/components/Modal';
import useLatestRequest from '@/app/hooks/useLatestRequest';
import { flightPlanService } from '@/app/services/flightPlanService';
import { FlightPlanEntryDto, FlightResultSummaryDto } from '@/app/types/flightPlan';
import { getErrorMessage } from '@/app/utils/apiClient';
import { downloadFile } from '@/app/utils/download';
import { formatFlightResultLabel, formatLocalDate } from '@/app/utils/formatters';

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
    const [content, setContent] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');

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

    const displayedContent = useMemo(() => content === null ? '' : normalizeContent(content), [content]);
    const isLoaded = !isLoading && !loadError && content !== null;

    const handleDownload = () => {
        if (content === null) return;

        downloadFile(new Blob([content], { type: 'text/plain;charset=utf-8' }), result.originalFileName);
    };

    return (
        <Modal isOpen title={`Wyniki lotu ${flight.location}`} onClose={onClose} maxWidthClass="max-w-6xl">
            <div className="px-6 py-5 space-y-4">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
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

                    {isLoaded && (
                        <button
                            type="button"
                            onClick={handleDownload}
                            className="px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md text-sm font-bold hover:bg-gray-100 transition shrink-0"
                        >
                            Pobierz plik
                        </button>
                    )}
                </div>

                {isLoading ? (
                    <LoadingState />
                ) : loadError ? (
                    <ErrorState message={loadError} onRetry={() => void loadContent()} />
                ) : (
                    <pre
                        tabIndex={0}
                        aria-label="Treść pliku z wynikami"
                        data-cy="flight-result-content"
                        className="max-h-[60vh] overflow-auto rounded-md border border-gray-200 bg-gray-50 p-4 font-mono text-xs leading-5 text-gray-800 whitespace-pre focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500"
                    >
                        {displayedContent}
                    </pre>
                )}
            </div>
        </Modal>
    );
}

/** Ujednolica końce wierszy, pomija znaki końca strony z wydruków i puste wiersze na końcu pliku. */
function normalizeContent(content: string): string {
    return content
        .replace(/\r\n?/g, '\n')
        .replace(/\f/g, '')
        .replace(/\n+$/, '');
}
