import { FlightPlanDetailsDto, FlightPlanEntryRequest, FlightPlanSummaryDto, FlightResultUploadRequest } from '@/app/types/flightPlan';
import { requestCreatedId, requestJson, requestText, requestVoid } from '@/app/utils/apiClient';

export const flightPlanService = {
    getAllPlans: (): Promise<FlightPlanSummaryDto[]> =>
        requestJson('/api/flight-plans', 'Nie udało się pobrać planów lotów.'),

    getPlanByYear: (year: number): Promise<FlightPlanDetailsDto> =>
        requestJson(`/api/flight-plans/${year}`, 'Nie udało się pobrać planu lotów.'),

    createPlan: (year: number): Promise<number> =>
        requestCreatedId('/api/flight-plans', 'Nie udało się utworzyć planu lotów.', { method: 'POST', body: { year } }),

    addEntry: (year: number, data: FlightPlanEntryRequest): Promise<number> =>
        requestCreatedId(`/api/flight-plans/${year}/entries`, 'Nie udało się dodać lotu do planu.', { method: 'POST', body: data }),

    updateEntry: (entryId: number, data: FlightPlanEntryRequest): Promise<void> =>
        requestVoid(`/api/flight-plans/entries/${entryId}`, 'Nie udało się zaktualizować lotu.', { method: 'PUT', body: data }),

    deleteEntry: (entryId: number): Promise<void> =>
        requestVoid(`/api/flight-plans/entries/${entryId}`, 'Nie udało się usunąć lotu.', { method: 'DELETE' }),

    deletePlan: (year: number): Promise<void> =>
        requestVoid(`/api/flight-plans/${year}`, 'Nie udało się usunąć planu lotów.', { method: 'DELETE' }),

    uploadResult: (entryId: number, file: File, metadata: FlightResultUploadRequest): Promise<number> => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }));

        return requestCreatedId(`/api/flight-results/entries/${entryId}`, 'Nie udało się wgrać wyników lotu.', { method: 'POST', body: formData });
    },

    getResultText: (resultId: number): Promise<string> =>
        requestText(`/api/flight-results/${resultId}/file`, 'Nie udało się pobrać wyników lotu.'),

    deleteResult: (resultId: number): Promise<void> =>
        requestVoid(`/api/flight-results/${resultId}`, 'Nie udało się usunąć wyników lotu.', { method: 'DELETE' })
};
