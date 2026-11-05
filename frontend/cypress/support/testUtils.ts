export type TestUserRole = 'BREEDER' | 'MODERATOR' | 'ADMINISTRATOR';

export interface TestUser {
    role: TestUserRole;
    email: string;
    name: string;
}

export interface MockCurrentUser {
    id: number;
    sub: string;
    name: string;
    role: TestUserRole;
    status: 'ACTIVE';
}

export const TEST_USERS = {
    admin: {
        role: 'ADMINISTRATOR',
        email: 'admin@pzhgp.pl',
        name: 'Admin'
    },
    moderator: {
        role: 'MODERATOR',
        email: 'moderator@pzhgp.pl',
        name: 'Moderator'
    },
    breeder: {
        role: 'BREEDER',
        email: 'breeder@pzhgp.pl',
        name: 'Hodowca'
    }
} satisfies Record<string, TestUser>;

// Frontend pobiera użytkownika z /api/auth/me zamiast dekodować JWT.
export const createTestUser = (role: TestUserRole, email: string, name: string): MockCurrentUser => ({
    id: 1,
    sub: email,
    name,
    role,
    status: 'ACTIVE'
});

export const createTestUserFor = (user: TestUser): MockCurrentUser => createTestUser(user.role, user.email, user.name);