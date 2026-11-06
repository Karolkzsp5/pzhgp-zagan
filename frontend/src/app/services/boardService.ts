import { BoardCandidateDto, BoardMemberDto, BoardMemberRequest } from '@/app/types/board';
import { API_URL, ensureOk, fetchApi, fetchWithAuth } from '@/app/utils/apiClient';

export const boardService = {
    /** Skład zarządu (endpoint publiczny). */
    getAllBoardMembers: async (): Promise<BoardMemberDto[]> => {
        const response = await fetchApi(`${API_URL}/api/board`);
        await ensureOk(response, 'Nie udało się pobrać danych zarządu.');
        return response.json();
    },

    /** Aktywni hodowcy do wyboru w formularzu zarządu (tylko dla administratora). */
    getBoardCandidates: async (signal?: AbortSignal): Promise<BoardCandidateDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/candidates`, { signal });
        await ensureOk(response, 'Nie udało się pobrać listy hodowców.');
        return response.json();
    },

    createBoardMember: async (data: BoardMemberRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        await ensureOk(response, 'Nie udało się dodać członka zarządu.');
    },

    updateBoardMember: async (id: number, data: BoardMemberRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        await ensureOk(response, 'Nie udało się zaktualizować członka zarządu.');
    },

    deleteBoardMember: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/${id}`, { method: 'DELETE' });
        await ensureOk(response, 'Nie udało się usunąć członka zarządu.');
    }
};