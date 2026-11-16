import { SectionDto } from '@/app/types/section';
import { requestJson } from '@/app/utils/apiClient';

export const sectionService = {
    /** Lista sekcji oddziału w kolejności wyświetlania (endpoint publiczny). */
    getSections: (): Promise<SectionDto[]> =>
        requestJson('/api/sections', 'Nie udało się pobrać listy sekcji.', { isPublic: true })
};