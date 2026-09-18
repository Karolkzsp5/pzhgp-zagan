import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { BoardMemberDto, BoardMemberRequest } from '@/app/types/board';

export const boardService = {
    getAllBoardMembers: async (): Promise<BoardMemberDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/board`);
        if (!response.ok) throw new Error(await readApiError(response, 'Nie udało się pobrać danych zarządu.'));
        return response.json();
    },

    createBoardMember: async (data: BoardMemberRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error(await readApiError(response, 'Nie udało się dodać członka zarządu.'));
    },

    updateBoardMember: async (id: number, data: BoardMemberRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error(await readApiError(response, 'Nie udało się zaktualizować członka zarządu.'));
    },

    deleteBoardMember: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/${id}`, { method: 'DELETE' });
        if (!response.ok) throw new Error(await readApiError(response, 'Nie udało się usunąć członka zarządu.'));
    }
};
