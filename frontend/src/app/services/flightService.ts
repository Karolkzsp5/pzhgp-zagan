import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { FlightDetailsDto, FlightSummaryDto, FlightUploadRequest, PageResponse } from '@/app/types/flight';

export const flightService = {
    /**
     * Wgrywa plik GPX wraz z opcjonalnymi metadanymi lotu.
     * Czasy rozpoczęcia i zakończenia oraz punkty trasy są odczytywane
     * z pliku GPX po stronie serwera. Metadane użytkownika przesyłane są
     * jako osobna część multipart o typie application/json.
     */
    uploadFlight: async (file: File, metadata: FlightUploadRequest): Promise<number> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));

        const response = await fetchWithAuth(`${API_URL}/api/flights`, { method: 'POST', body: formData });
        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się wgrać pliku GPX.'));
        }

        const body = await response.json();
        return body.id as number;
    },

    getMyFlights: async (page = 0, size = 10): Promise<PageResponse<FlightSummaryDto>> => {
        const response = await fetchWithAuth(`${API_URL}/api/flights?page=${page}&size=${size}`);
        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się pobrać listy lotów.'));
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
            throw new Error(await readApiError(response, 'Nie udało się pobrać danych lotu.'));
        }
        return response.json();
    },

    deleteFlight: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/flights/${id}`, { method: 'DELETE' });
        if (!response.ok) {
            throw new Error(await readApiError(response, 'Nie udało się usunąć lotu.'));
        }
    }
};
