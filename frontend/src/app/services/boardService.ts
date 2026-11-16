import { BoardCandidateDto, BoardMemberDto, BoardMemberRequest } from '@/app/types/board';
import { requestJson, requestVoid } from '@/app/utils/apiClient';

export const boardService = {
    /** Skład zarządu (endpoint publiczny). */
    getAllBoardMembers: (): Promise<BoardMemberDto[]> =>
        requestJson('/api/board', 'Nie udało się pobrać danych zarządu.', { isPublic: true }),

    /** Aktywni hodowcy do wyboru w formularzu zarządu (tylko dla administratora). */
    getBoardCandidates: (): Promise<BoardCandidateDto[]> =>
        requestJson('/api/board/candidates', 'Nie udało się pobrać listy hodowców.'),

    createBoardMember: (data: BoardMemberRequest): Promise<void> =>
        requestVoid('/api/board', 'Nie udało się dodać członka zarządu.', { method: 'POST', body: data }),

    updateBoardMember: (id: number, data: BoardMemberRequest): Promise<void> =>
        requestVoid(`/api/board/${id}`, 'Nie udało się zaktualizować członka zarządu.', { method: 'PUT', body: data }),

    deleteBoardMember: (id: number): Promise<void> =>
        requestVoid(`/api/board/${id}`, 'Nie udało się usunąć członka zarządu.', { method: 'DELETE' })
};