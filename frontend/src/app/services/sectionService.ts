import { SectionDto } from '@/app/types/section';
import { API_URL, ensureOk, fetchApi } from '@/app/utils/apiClient';

export const sectionService = {
    /** Lista sekcji oddziału w kolejności wyświetlania (endpoint publiczny). */
    getSections: async (signal?: AbortSignal): Promise<SectionDto[]> => {
        const response = await fetchApi(`${API_URL}/api/sections`, { signal });
        await ensureOk(response, 'Nie udało się pobrać listy sekcji.');
        return response.json();
    }
};
