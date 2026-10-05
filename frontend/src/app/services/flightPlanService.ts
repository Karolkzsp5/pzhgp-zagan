import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { FlightPlanDetailsDto, FlightPlanEntryRequest, FlightPlanSummaryDto, FlightResultUploadRequest } from '@/app/types/flightPlan';

const ensureOk = async (response: Response, fallback: string): Promise<void> => {
    if (!response.ok) {
        throw new Error(await readApiError(response, fallback));
    }
};

export const flightPlanService = {
    getAllPlans: async (): Promise<FlightPlanSummaryDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans`);
        await ensureOk(response, 'Nie udało się pobrać planów lotów.');
        return response.json();
    },

    getPlanByYear: async (year: number): Promise<FlightPlanDetailsDto> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans/${year}`);
        await ensureOk(response, 'Nie udało się pobrać planu lotów.');
        return response.json();
    },

    createPlan: async (year: number): Promise<number> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ year })
        });

        await ensureOk(response, 'Nie udało się utworzyć planu lotów.');

        const body = await response.json();
        return body.id as number;
    },

    addEntry: async (year: number, data: FlightPlanEntryRequest): Promise<number> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans/${year}/entries`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        await ensureOk(response, 'Nie udało się dodać lotu do planu.');

        const body = await response.json();
        return body.id as number;
    },

    updateEntry: async (entryId: number, data: FlightPlanEntryRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans/entries/${entryId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        await ensureOk(response, 'Nie udało się zaktualizować lotu.');
    },

    deleteEntry: async (entryId: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans/entries/${entryId}`, {
            method: 'DELETE'
        });

        await ensureOk(response, 'Nie udało się usunąć lotu.');
    },

    deletePlan: async (year: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/flight-plans/${year}`, {
            method: 'DELETE'
        });

        await ensureOk(response, 'Nie udało się usunąć planu lotów.');
    },

    uploadResult: async (
        entryId: number,
        file: File,
        metadata: FlightResultUploadRequest
    ): Promise<number> => {
        const formData = new FormData();

        formData.append('file', file);
        formData.append(
            'metadata',
            new Blob([JSON.stringify(metadata)], { type: 'application/json' })
        );

        const response = await fetchWithAuth(
            `${API_URL}/api/flight-results/entries/${entryId}`,
            {
                method: 'POST',
                body: formData
            }
        );

        await ensureOk(response, 'Nie udało się wgrać wyników lotu.');

        const body = await response.json();
        return body.id as number;
    },

    getResultFile: async (resultId: number): Promise<Blob> => {
        const response = await fetchWithAuth(
            `${API_URL}/api/flight-results/${resultId}/file`
        );

        await ensureOk(response, 'Nie udało się pobrać wyników lotu.');

        return response.blob();
    },

    deleteResult: async (resultId: number): Promise<void> => {
        const response = await fetchWithAuth(
            `${API_URL}/api/flight-results/${resultId}`,
            {
                method: 'DELETE'
            }
        );

        await ensureOk(response, 'Nie udało się usunąć wyników lotu.');
    }
};