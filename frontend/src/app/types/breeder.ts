import { UserRole } from '@/app/utils/authSession';

export type AccountStatus = 'PENDING' | 'ACTIVE' | 'BLOCKED';

/** Operacja administratora na koncie hodowcy. */
export type AccountAction = 'approve' | 'reject' | 'block' | 'unblock';

/** Konto hodowcy w postaci zwracanej przez panel administratora. */
export interface BreederDto {
    id: number;
    name: string;
    surname: string;
    email: string;
    phoneNumber: string;
    dateOfBirth: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
    sectionId: number;
    sectionName: string;
    status: AccountStatus;
    createdAt: string;
    role: UserRole;
}
