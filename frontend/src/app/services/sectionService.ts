import { SectionDto } from '@/app/types/section';
import { API_URL, ensureOk, fetchApi } from '@/app/utils/apiClient';

export const sectionService = {
    /** Lista sekcji oddziału w kolejności wyświetlania (endpoint publiczny). */
    getSections: async (): Promise<SectionDto[]> => {
        const response = await fetchApi(`${API_URL}/api/sections`);
        await ensureOk(response, 'Nie udało się pobrać listy sekcji.');
        return response.json();
    }
};
