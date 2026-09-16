import { fetchWithAuth } from '@/app/utils/apiClient';
import {
    FlightDetailsDto,
    FlightSummaryDto,
    FlightUploadRequest,
    PageResponse
} from '@/app/types/flight';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '';

const readError = async (response: Response, fallback: string): Promise<string> => {
    const message = await response.text();
    return message && message.trim().length > 0 ? message : fallback;
};

export const flightService = {
    /**
     * Wgrywa plik GPX wraz z metadanymi lotu — w tym obowiązkowymi godzinami
     * wypuszczenia i przylotu, na podstawie których liczone są statystyki.
     *
     * Metadane przesyłane są jako osobna część żądania o typie application/json,
     * dzięki czemu na serwerze podlegają walidacji adnotacjami Jakarta Validation.
     */
    uploadFlight: async (file: File, metadata: FlightUploadRequest): Promise<number> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append(
            'metadata',
            new Blob([JSON.stringify(metadata)], { type: 'application/json' })
        );

        const response = await fetchWithAuth(`${API_URL}/api/flights`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(await readError(response, 'Nie udało się wgrać pliku GPX.'));
        }

        const body = await response.json();
        return body.id as number;
    },

    getMyFlights: async (page = 0, size = 10): Promise<PageResponse<FlightSummaryDto>> => {
        const response = await fetchWithAuth(`${API_URL}/api/flights?page=${page}&size=${size}`);

        if (!response.ok) {
            throw new Error(await readError(response, 'Nie udało się pobrać listy lotów.'));
        }
        return response.json();
    },

    /**
     * Pobiera szczegóły lotu wraz z trasą.
     *
     * @param toleranceMeters tolerancja upraszczania trasy w metrach; 0 zwraca pełną trasę
     */
    getFlight: async (id: number, toleranceMeters?: number): Promise<FlightDetailsDto> => {
        const query = toleranceMeters !== undefined ? `?tolerance=${toleranceMeters}` : '';

        const response = await fetchWithAuth(`${API_URL}/api/flights/${id}${query}`);

        if (!response.ok) {
            throw new Error(await readError(response, 'Nie udało się pobrać danych lotu.'));
        }
        return response.json();
    },

    deleteFlight: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/flights/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(await readError(response, 'Nie udało się usunąć lotu.'));
        }
    }
};