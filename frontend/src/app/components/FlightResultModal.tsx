"use client";

import {FormEvent, useEffect, useRef, useState} from 'react';
import Modal from '@/app/components/Modal';
import {flightPlanService} from '@/app/services/flightPlanService';
import {FlightPlanEntryDto, FlightResultScope, FlightResultSummaryDto} from '@/app/types/flightPlan';
import {API_URL, fetchWithAuth, readApiError} from '@/app/utils/apiClient';
import {formatLocalDate} from '@/app/utils/formatters';

interface SectionDto {
    id: number;
    name: string;
}

interface FlightResultModalProps {
    isOpen: boolean;
    entry: FlightPlanEntryDto;
    onClose: () => void;
    onChanged: () => Promise<void>;
}

const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

export default function FlightResultModal({isOpen, entry, onClose, onChanged}: FlightResultModalProps) {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [sections, setSections] = useState<SectionDto[]>([]);
    const [scope, setScope] = useState<FlightResultScope>('BRANCH');
    const [sectionId, setSectionId] = useState('');
    const [file, setFile] = useState<File | null>(null);
    const [isDragging, setIsDragging] = useState(false);

    const [isLoadingSections, setIsLoadingSections] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [deletingResultId, setDeletingResultId] = useState<number | null>(null);
    const [resultToDeleteId, setResultToDeleteId] = useState<number | null>(null);

    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const hasBranchResult = entry.results.some(result => result.scope === 'BRANCH');

    const usedSectionIds = new Set(entry.results.filter(result =>
        result.scope === 'SECTION' && result.sectionId !== null)
        .map(result => result.sectionId as number)
    );

    useEffect(() => {
        if (!isOpen) return;

        setScope('BRANCH');
        setSectionId('');
        setFile(null);
        setError('');
        setSuccessMessage('');
        setResultToDeleteId(null);

        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    }, [isOpen, entry.id]);

    useEffect(() => {
        if (isOpen && hasBranchResult && scope === 'BRANCH') {
            setScope('SECTION');
        }
    }, [isOpen, hasBranchResult, scope]);

    useEffect(() => {
        if (!isOpen) return;

        const loadSections = async () => {
            setIsLoadingSections(true);

            try {
                const response = await fetchWithAuth(`${API_URL}/api/sections`);

                if (!response.ok) {
                    throw new Error(await readApiError(response, 'Nie udało się pobrać listy sekcji.'));
                }

                setSections(await response.json());
            } catch (error) {
                setError(error instanceof Error ? error.message : 'Nie udało się pobrać listy sekcji.');
            } finally {
                setIsLoadingSections(false);
            }
        };

        void loadSections();
    }, [isOpen]);

    const clearMessages = () => {
        setError('');
        setSuccessMessage('');
    };

    const selectFile = (selectedFile: File | null) => {
        setError('');
        setSuccessMessage('');

        if (!selectedFile) {
            setFile(null);
            return;
        }

        if (!selectedFile.name.toLowerCase().endsWith('.txt')) {
            setError('Dozwolone są wyłącznie pliki z rozszerzeniem .txt.');
            setFile(null);

            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }

            return;
        }

        if (selectedFile.size > MAX_FILE_SIZE_BYTES) {
            setError('Plik jest za duży. Maksymalny rozmiar pliku z wynikami to 5 MB.');
            setFile(null);

            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }

            return;
        }

        setFile(selectedFile);
    };

    const handleDrop = (event: React.DragEvent<HTMLLabelElement>) => {
        event.preventDefault();
        setIsDragging(false);

        if (isUploading) return;

        selectFile(event.dataTransfer.files?.[0] ?? null);
    };

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        clearMessages();

        if (!file) {
            setError('Wybierz plik z wynikami.');
            return;
        }

        if (scope === 'BRANCH' && hasBranchResult) {
            setError('Wyniki oddziałowe dla tego lotu zostały już wgrane.');
            return;
        }

        if (scope === 'SECTION' && !sectionId) {
            setError('Wybierz sekcję.');
            return;
        }

        const parsedSectionId = scope === 'SECTION' ? Number(sectionId) : null;

        if (parsedSectionId !== null && usedSectionIds.has(parsedSectionId)) {
            setError('Wyniki wybranej sekcji dla tego lotu zostały już wgrane.');
            return;
        }

        setIsUploading(true);

        try {
            await flightPlanService.uploadResult(entry.id, file, {
                scope,
                sectionId: parsedSectionId
            });

            setFile(null);
            setSectionId('');

            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }

            await onChanged();

            setSuccessMessage('Wyniki zostały wgrane.');
        } catch (error) {
            setError(error instanceof Error ? error.message : 'Nie udało się wgrać wyników lotu.');
        } finally {
            setIsUploading(false);
        }
    };

    const handleDeleteResult = async (result: FlightResultSummaryDto) => {
        clearMessages();
        setDeletingResultId(result.id);

        try {
            await flightPlanService.deleteResult(result.id);
            await onChanged();

            setResultToDeleteId(null);
            setSuccessMessage('Wyniki zostały usunięte.');
        } catch (error) {
            setError(error instanceof Error ? error.message : 'Nie udało się usunąć wyników lotu.');
        } finally {
            setDeletingResultId(null);
        }
    };

    const availableSections = sections.filter(section => !usedSectionIds.has(section.id));
    const noAvailableResultSlot = hasBranchResult && !isLoadingSections && availableSections.length === 0;

    return (
        <Modal
            isOpen={isOpen}
            title="Wyniki lotu"
            onClose={onClose}
            closeDisabled={isUploading || deletingResultId !== null}
            maxWidthClass="max-w-2xl"
        >
            <div className="p-6 space-y-6">
                <div className="bg-gray-50 border border-gray-200 rounded-md px-4 py-3">
                    <p className="font-semibold text-gray-900">
                        {entry.location}
                    </p>

                    <p className="mt-1 text-sm text-gray-500">
                        {formatLocalDate(entry.scheduledDate)}
                        {' · '}
                        {entry.distanceKm} km
                    </p>
                </div>

                <section>
                    <h3 className="text-sm font-bold text-gray-700 mb-3">
                        Wgrane wyniki
                    </h3>

                    {entry.results.length === 0 ? (
                        <p className="text-sm text-gray-400">
                            Dla tego lotu nie wgrano jeszcze żadnych wyników.
                        </p>
                    ) : (
                        <div className="border border-gray-200 rounded-md divide-y divide-gray-200">
                            {entry.results.map(result => (
                                <div
                                    key={result.id}
                                    className="px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                                >
                                    <div className="min-w-0">
                                        <p className="text-sm font-semibold text-gray-800">
                                            {formatResultLabel(result)}
                                        </p>

                                        <p className="text-xs text-gray-500 truncate">
                                            {result.originalFileName}
                                        </p>
                                    </div>

                                    {resultToDeleteId === result.id ? (
                                        <div className="flex items-center gap-2 shrink-0">
                                            <span className="text-xs text-gray-500">
                                                Usunąć?
                                            </span>

                                            <button
                                                type="button"
                                                disabled={deletingResultId !== null}
                                                onClick={() =>
                                                    setResultToDeleteId(null)
                                                }
                                                className="text-xs font-medium text-gray-600 hover:text-gray-900 disabled:opacity-50"
                                            >
                                                Anuluj
                                            </button>

                                            <button
                                                type="button"
                                                disabled={deletingResultId !== null}
                                                onClick={() =>
                                                    void handleDeleteResult(result)
                                                }
                                                className="text-xs font-semibold text-red-600 hover:text-red-800 disabled:opacity-50"
                                            >
                                                {deletingResultId === result.id ? 'Usuwanie...' : 'Usuń'}
                                            </button>
                                        </div>
                                    ) : (
                                        <button
                                            type="button"
                                            onClick={() =>
                                                setResultToDeleteId(result.id)
                                            }
                                            disabled={isUploading || deletingResultId !== null}
                                            className="text-sm font-medium text-red-600 hover:text-red-800 transition shrink-0 disabled:opacity-50"
                                        >
                                            Usuń
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </section>

                <div className="border-t border-gray-200"/>

                <section>
                    <h3 className="text-sm font-bold text-gray-700 mb-4">
                        Dodaj wyniki
                    </h3>

                    {noAvailableResultSlot ? (
                        <p className="text-sm text-gray-500">
                            Dla tego lotu wgrano już wszystkie dostępne wyniki.
                        </p>
                    ) : (
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label htmlFor="flight-result-scope"
                                       className="block text-sm font-medium text-gray-700 mb-1">
                                    Rodzaj wyników
                                </label>

                                <select
                                    id="flight-result-scope"
                                    value={scope}
                                    onChange={event => {
                                        clearMessages();
                                        setScope(
                                            event.target.value as FlightResultScope
                                        );
                                        setSectionId('');
                                    }}
                                    disabled={isUploading}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                                >
                                    <option value="BRANCH" disabled={hasBranchResult}>
                                        Oddział
                                        {hasBranchResult ? ' — wyniki już dodane' : ''}
                                    </option>

                                    <option value="SECTION"
                                            disabled={!isLoadingSections && availableSections.length === 0}
                                    >
                                        Sekcja
                                    </option>
                                </select>
                            </div>

                            {scope === 'SECTION' && (
                                <div>
                                    <label htmlFor="flight-result-section"
                                           className="block text-sm font-medium text-gray-700 mb-1">
                                        Sekcja
                                    </label>

                                    <select
                                        id="flight-result-section"
                                        value={sectionId}
                                        onChange={event => {
                                            clearMessages();
                                            setSectionId(event.target.value);
                                        }}
                                        disabled={isUploading || isLoadingSections}
                                        required
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                                    >
                                        <option value="" disabled hidden>
                                            {isLoadingSections ? 'Ładowanie sekcji...' : 'Wybierz sekcję'}
                                        </option>

                                        {availableSections.map(section => (
                                            <option key={section.id} value={section.id}>
                                                {section.name}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                            )}

                            <div>
                                <span className="block text-sm font-medium text-gray-700 mb-1">
                                    Plik wyników
                                </span>

                                <label onDragOver={event => {
                                    event.preventDefault();

                                    if (!isUploading) {
                                        setIsDragging(true);
                                    }
                                }}
                                       onDragLeave={() => setIsDragging(false)}
                                       onDrop={handleDrop}
                                       className={`flex flex-col items-center justify-center w-full border-2 border-dashed rounded-lg p-6 transition ${
                                           isUploading
                                               ? 'border-gray-300 bg-gray-50 opacity-60 cursor-not-allowed'
                                               : isDragging
                                                   ? 'border-blue-500 bg-blue-50 cursor-pointer'
                                                   : 'border-gray-300 bg-gray-50 hover:border-blue-400 hover:bg-blue-50/40 cursor-pointer'
                                       }`}
                                >
                                    <input
                                        ref={fileInputRef}
                                        id="flight-result-file"
                                        type="file"
                                        accept=".txt,text/plain"
                                        disabled={isUploading}
                                        className="sr-only"
                                        onChange={event =>
                                            selectFile(event.target.files?.[0] ?? null)
                                        }
                                    />
                                    <svg xmlns="http://www.w3.org/2000/svg" className="w-10 h-10 text-gray-400 mb-3"
                                         viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                                        <path d="M240-192q-80 0-136-56.5T48-385q0-76 51.5-131.5T227-576q23.43-85.75 93.7-138.87Q390.98-768 480-768q107 0 185.5 68.5T744-528q70 0 119 49t49 118q0 70.42-49 119.71Q814-192 744-192H516q-29.7 0-50.85-21.15Q444-234.3 444-264v-174l-57 57-51-51 144-144 144 144-51 51-57-57v174h228q40.32 0 68.16-27.77 27.84-27.78 27.84-68Q840-400 812.16-428q-27.84-28-68.16-28h-72v-72q0-73-57.5-120.5t-135-47.5Q402-696 348-639.5T283-504h-43q-49.71 0-84.86 35.2-35.14 35.2-35.14 85t35.14 84.8q35.15 35 84.86 35h132v72H240Zm240-246Z"/>
                                    </svg>

                                    {file ? (
                                        <>
                                            <span
                                                className="text-sm font-semibold text-gray-900 text-center max-w-full [overflow-wrap:anywhere]">
                                                {file.name}
                                            </span>

                                            <span className="text-xs text-gray-500 mt-1 text-center">
                                                {(file.size / 1024).toFixed(0)} kB
                                                {' — '}
                                                kliknij, aby wybrać inny plik
                                            </span>
                                        </>
                                    ) : (
                                        <>
                                            <span className="text-sm font-semibold text-gray-700 text-center">
                                                Przeciągnij plik .txt lub kliknij, aby wybrać
                                            </span>

                                            <span className="text-xs text-gray-500 mt-1">
                                                Maksymalny rozmiar pliku: 5 MB
                                            </span>
                                        </>
                                    )}
                                </label>
                            </div>

                            {error && (
                                <p className="text-sm text-red-600" role="alert">
                                    {error}
                                </p>
                            )}

                            {successMessage && (
                                <p className="text-sm text-green-700" role="status">
                                    {successMessage}
                                </p>
                            )}

                            <div className="flex justify-end gap-3 pt-2">
                                <button
                                    type="button"
                                    onClick={onClose}
                                    disabled={isUploading}
                                    className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
                                >
                                    Zamknij
                                </button>

                                <button
                                    type="submit"
                                    disabled={
                                        isUploading ||
                                        isLoadingSections ||
                                        !file
                                    }
                                    className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-sm font-bold transition disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    {isUploading ? 'Wgrywanie...' : 'Wgraj wyniki'}
                                </button>
                            </div>
                        </form>
                    )}
                </section>

                {noAvailableResultSlot && (
                    <div className="flex justify-end">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition"
                        >
                            Zamknij
                        </button>
                    </div>
                )}
            </div>
        </Modal>
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