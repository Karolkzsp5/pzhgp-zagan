import { getAuthToken } from '@/utils/jwt';
import { BoardMemberDto, BoardMemberRequest } from '@/app/types/board';

const API_URL = process.env.NEXT_PUBLIC_API_URL || '';

const getHeaders = () => {
    const token = getAuthToken();
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
};

export const boardService = {
    getAllBoardMembers: async (): Promise<BoardMemberDto[]> => {
        const response = await fetch(`${API_URL}/api/board`, {
            method: 'GET',
            headers: getHeaders(),
        });
        if (!response.ok) throw new Error('Błąd podczas pobierania zarządu');
        return response.json();
    },

    createBoardMember: async (data: BoardMemberRequest): Promise<void> => {
        const response = await fetch(`${API_URL}/api/board`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error(await response.text());
    },

    updateBoardMember: async (id: number, data: BoardMemberRequest): Promise<void> => {
        const response = await fetch(`${API_URL}/api/board/${id}`, {
            method: 'PUT',
            headers: getHeaders(),
            body: JSON.stringify(data),
        });
        if (!response.ok) throw new Error(await response.text());
    },

    deleteBoardMember: async (id: number): Promise<void> => {
        const response = await fetch(`${API_URL}/api/board/${id}`, {
            method: 'DELETE',
            headers: getHeaders(),
        });
        if (!response.ok) throw new Error(await response.text());
    }
};