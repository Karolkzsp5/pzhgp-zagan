export type BoardRole =
    | 'PREZES'
    | 'WICEPREZES_DS_LOTOWYCH'
    | 'WICEPREZES_DS_FINANSOWYCH'
    | 'WICEPREZES_DS_GOSPODARCZYCH'
    | 'SEKRETARZ'
    | 'SKARBNIK'
    | 'CZLONEK_ZARZADU';

export const BOARD_ROLE_LABELS: Record<BoardRole, string> = {
    PREZES: 'Prezes',
    WICEPREZES_DS_LOTOWYCH: 'V-ce Prezes ds. lotowych',
    WICEPREZES_DS_FINANSOWYCH: 'V-ce Prezes ds. finansowych',
    WICEPREZES_DS_GOSPODARCZYCH: 'V-ce Prezes ds. gospodarczych',
    SEKRETARZ: 'Sekretarz',
    SKARBNIK: 'Skarbnik',
    CZLONEK_ZARZADU: 'Członek Zarządu'
};

export const BRANCH_ROLE_ORDER: Record<BoardRole, number> = {
    PREZES: 1,
    WICEPREZES_DS_LOTOWYCH: 2,
    WICEPREZES_DS_FINANSOWYCH: 3,
    WICEPREZES_DS_GOSPODARCZYCH: 4,
    SEKRETARZ: 5,
    CZLONEK_ZARZADU: 6,
    SKARBNIK: 99
};

export const SECTION_ROLE_ORDER: Partial<Record<BoardRole, number>> = {
    PREZES: 1,
    SKARBNIK: 2,
    SEKRETARZ: 3
};

export const SECTION_ROLES: BoardRole[] = ['PREZES', 'SKARBNIK', 'SEKRETARZ'];

/** Hodowca do wyboru w formularzu zarządu (bez danych kontaktowych). */
export interface BoardCandidateDto {
    id: number;
    name: string;
    surname: string;
    sectionId: number | null;
}

export interface BoardMemberDto {
    id: number;
    role: BoardRole;
    managedSectionId: number | null;
    managedSectionName: string | null;
    firstName: string;
    lastName: string;
    publicPhoneNumber: string | null;
    breederId: number | null;
}

export interface BoardMemberRequest {
    role: BoardRole;
    managedSectionId?: number | null;
    breederId?: number | null;
    customName?: string | null;
    customSurname?: string | null;
    contactPhone?: string | null;
}