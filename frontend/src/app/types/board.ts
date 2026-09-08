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

export interface BoardMemberDto {
    id: number;
    role: BoardRole;
    sortOrder: number;
    managedSectionId: number | null;
    managedSectionName: string | null;
    firstName: string;
    lastName: string;
    publicPhoneNumber: string | null;
    breederId: number | null;
}

export interface BoardMemberRequest {
    role: BoardRole;
    sortOrder: number;
    managedSectionId?: number | null;
    breederId?: number | null;
    customName?: string | null;
    customSurname?: string | null;
    contactPhone?: string | null;
}