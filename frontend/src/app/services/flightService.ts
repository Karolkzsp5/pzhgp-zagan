import { PageResponse } from '@/app/types/common';
import { FlightDetailsDto, FlightSummaryDto, FlightUploadRequest } from '@/app/types/flight';
import { requestCreatedId, requestJson, requestVoid } from '@/app/utils/apiClient';

export const flightService = {
    /**
     * Wgrywa plik GPX wraz z opcjonalnymi metadanymi lotu.
     * Czasy rozpoczęcia i zakończenia oraz punkty trasy są odczytywane
     * z pliku GPX po stronie serwera. Metadane użytkownika przesyłane są
     * jako osobna część multipart o typie application/json.
     */
    uploadFlight: (file: File, metadata: FlightUploadRequest): Promise<number> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));

        return requestCreatedId('/api/flights', 'Nie udało się wgrać pliku GPX.', { method: 'POST', body: formData });
    },

    getMyFlights: (page = 0, size = 10): Promise<PageResponse<FlightSummaryDto>> =>
        requestJson(`/api/flights?page=${page}&size=${size}`, 'Nie udało się pobrać listy lotów.'),

    /**
     * Pobiera szczegóły lotu wraz z trasą.
     *
     * @param toleranceMeters tolerancja upraszczania trasy w metrach; 0 zwraca pełną trasę
     */
    getFlight: (id: number, toleranceMeters?: number): Promise<FlightDetailsDto> => {
        const query = toleranceMeters !== undefined ? `?tolerance=${toleranceMeters}` : '';
        return requestJson(`/api/flights/${id}${query}`, 'Nie udało się pobrać danych lotu.');
    },

    deleteFlight: (id: number): Promise<void> =>
        requestVoid(`/api/flights/${id}`, 'Nie udało się usunąć lotu.', { method: 'DELETE' })
};
