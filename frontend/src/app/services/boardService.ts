import { fetchWithAuth } from '@/app/utils/apiClient';
import { BoardMemberDto, BoardMemberRequest } from '@/app/types/board';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '';

export const boardService = {
    getAllBoardMembers: async (): Promise<BoardMemberDto[]> => {
        const response = await fetchWithAuth(`${API_URL}/api/board`); // Usunięto POST i body
        if (!response.ok) throw new Error('Błąd podczas pobierania zarządu');
        return response.json();
    },

    createBoardMember: async (data: BoardMemberRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error(await response.text());
    },

    updateBoardMember: async (id: number, data: BoardMemberRequest): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error(await response.text());
    },

    deleteBoardMember: async (id: number): Promise<void> => {
        const response = await fetchWithAuth(`${API_URL}/api/board/${id}`, {
            method: 'DELETE'
        });
        if (!response.ok) throw new Error(await response.text());
    }
};