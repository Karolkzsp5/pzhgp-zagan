export enum BoardRole {
    PREZES = 'PREZES',
    WICEPREZES_DS_LOTOWYCH = 'WICEPREZES_DS_LOTOWYCH',
    WICEPREZES_DS_FINANSOWYCH = 'WICEPREZES_DS_FINANSOWYCH',
    WICEPREZES_DS_GOSPODARCZYCH = 'WICEPREZES_DS_GOSPODARCZYCH',
    SEKRETARZ = 'SEKRETARZ',
    SKARBNIK = 'SKARBNIK',
    CZLONEK_ZARZADU = 'CZLONEK_ZARZADU'
}

export const BoardRoleTranslations: Record<BoardRole, string> = {
    [BoardRole.PREZES]: "Prezes",
    [BoardRole.WICEPREZES_DS_LOTOWYCH]: "V-ce Prezes ds. lotowych",
    [BoardRole.WICEPREZES_DS_FINANSOWYCH]: "V-ce Prezes ds. finansowych",
    [BoardRole.WICEPREZES_DS_GOSPODARCZYCH]: "V-ce Prezes ds. gospodarczych",
    [BoardRole.SEKRETARZ]: "Sekretarz",
    [BoardRole.SKARBNIK]: "Skarbnik",
    [BoardRole.CZLONEK_ZARZADU]: "Członek Zarządu"
};

export const BRANCH_ROLE_ORDER: Record<BoardRole, number> = {
    [BoardRole.PREZES]: 1,
    [BoardRole.WICEPREZES_DS_LOTOWYCH]: 2,
    [BoardRole.WICEPREZES_DS_FINANSOWYCH]: 3,
    [BoardRole.WICEPREZES_DS_GOSPODARCZYCH]: 4,
    [BoardRole.SEKRETARZ]: 5,
    [BoardRole.CZLONEK_ZARZADU]: 6,
    [BoardRole.SKARBNIK]: 99
};

export const SECTION_ROLE_ORDER: Partial<Record<BoardRole, number>> = {
    [BoardRole.PREZES]: 1,
    [BoardRole.SKARBNIK]: 2,
    [BoardRole.SEKRETARZ]: 3
};

export const SECTION_ROLES: BoardRole[] = [BoardRole.PREZES, BoardRole.SKARBNIK, BoardRole.SEKRETARZ];

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