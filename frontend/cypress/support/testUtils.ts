export type TestUserRole = 'BREEDER' | 'MODERATOR' | 'ADMINISTRATOR';

interface TestUser {
    role: TestUserRole;
    email: string;
    name: string;
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

export const createFakeToken = (role: TestUserRole, email: string, name: string) => {
    const header = {
        alg: 'HS256',
        typ: 'JWT'
    };

    const payload = {
        sub: email,
        name,
        role,
        exp: 9999999999
    };

    return [
        btoa(JSON.stringify(header)),
        btoa(JSON.stringify(payload)),
        'mock-signature'
    ].join('.');
};

export const createFakeTokenForUser = (user: TestUser) => {
    return createFakeToken(user.role, user.email, user.name);
};