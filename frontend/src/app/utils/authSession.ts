import type { AccountStatus } from '@/app/types/breeder';

export type UserRole = 'BREEDER' | 'MODERATOR' | 'ADMINISTRATOR';

export interface CurrentUser {
    id: number;
    email: string;
    name: string;
    role: UserRole;
    status: AccountStatus;
}

export const AUTH_CHANGED_EVENT = 'pzhgp-auth-changed';
export const AUTH_SYNC_KEY = 'pzhgp_auth_sync';

let authGeneration = 0;

export const getAuthGeneration = (): number => authGeneration;

// Numer generacji unieważnia odpowiedzi HTTP rozpoczęte dla poprzedniej sesji.
export const invalidateAuthSession = (notify = true, broadcast = true) => {
    authGeneration++;
    if (typeof window === 'undefined') return;

    if (notify) window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
    if (broadcast) {
        // W storage trafia wyłącznie sygnał między kartami, NIGDY JWT.
        localStorage.setItem(AUTH_SYNC_KEY, `${Date.now()}-${authGeneration}`);
    }
};