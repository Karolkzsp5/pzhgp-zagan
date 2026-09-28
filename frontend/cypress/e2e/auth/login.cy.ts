import { createFakeToken } from '../../support/testUtils';

describe('Breeder Login Process', () => {
    beforeEach(() => {
        cy.visit('/login');
    });

    it('Should display a login form with the relevant fields', () => {
        cy.get('h1').should('be.visible').and('contain.text', 'Logowanie Hodowcy');
        cy.get('form').should('be.visible');
        cy.get('input[name="email"]').should('be.visible');
        cy.get('input[name="password"]').should('be.visible');
        cy.get('input[name="remember-me"]').should('be.visible');
        cy.get('label[for="remember-me"]').should('be.visible');
        cy.get('button[type="submit"]').should('be.visible').and('not.be.disabled');
        cy.contains('Nie masz jeszcze konta?').should('be.visible');
        cy.contains('a', 'Zarejestruj się tutaj').should('be.visible').and('have.attr', 'href', '/register');
    });

    it('Should display an error message if the credentials are incorrect', () => {
        cy.intercept('POST', '**/api/auth/login', {
            statusCode: 401,
            body: 'Nieprawidłowy adres e-mail lub hasło.'
        }).as('loginErrorRequest');

        cy.get('input[name="email"]').clear().type('wrong.email@test.cy');
        cy.get('input[name="password"]').clear().type('WrongPassword1!');
        cy.get('button[type="submit"]').click();

        cy.wait('@loginErrorRequest');
        cy.contains('Nieprawidłowy adres e-mail lub hasło.').should('be.visible');
        cy.location('pathname').should('eq', '/login');

        cy.window().then((win) => {
            expect(win.localStorage.getItem('jwt_token')).to.be.null;
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });
    });

    it('Should prevent user from logging in if their account has not been approved by an administrator (PENDING Status)', () => {
        cy.intercept('POST', '**/api/auth/login', {
            statusCode: 403,
            body: 'Twoje konto oczekuje jeszcze na akceptację administratora.'
        }).as('pendingAccountRequest');

        cy.get('input[name="email"]').clear().type('pending.user@test.cy');
        cy.get('input[name="password"]').clear().type('ValidPassword1!');
        cy.get('button[type="submit"]').click();

        cy.wait('@pendingAccountRequest');
        cy.contains('Twoje konto oczekuje jeszcze na akceptację administratora.').should('be.visible');
    });

    it('Should prevent user from logging in if their account is blocked', () => {
        cy.intercept('POST', '**/api/auth/login', {
            statusCode: 403,
            body: 'Twoje konto zostało zablokowane.'
        }).as('blockedAccountRequest');

        cy.get('input[name="email"]').type('blocked.user@test.cy');
        cy.get('input[name="password"]').type('ValidPassword1!');
        cy.get('button[type="submit"]').click();

        cy.wait('@blockedAccountRequest');

        cy.contains('Twoje konto zostało zablokowane.').should('be.visible');
        cy.location('pathname').should('eq', '/login');

        cy.window().then((win) => {
            expect(win.localStorage.getItem('jwt_token')).to.be.null;
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });
    });

    it('Should store the token in sessionStorage when "Remember me" is not selected', () => {
        const mockToken = createFakeToken('BREEDER', 'test.cypress1@test.cy', 'Test');

        cy.intercept('POST', '**/api/auth/login', {
            statusCode: 200,
            body: mockToken
        }).as('successfulLogin');

        cy.get('input[name="email"]').type('test.cypress1@test.cy');
        cy.get('input[name="password"]').type('Test.cypress1');

        cy.get('input[name="remember-me"]').should('not.be.checked');

        cy.get('button[type="submit"]').click();

        cy.wait('@successfulLogin');

        cy.window().then((win) => {
            expect(win.sessionStorage.getItem('jwt_token')).to.eq(mockToken);
            expect(win.localStorage.getItem('jwt_token')).to.be.null;
        });
    });

    it('Should log the user in and redirect them to the home page', () => {
        const payload = {
            sub: 'test.cypress1@test.cy',
            name: 'Test',
            role: 'BREEDER',
            exp: Math.floor(Date.now() / 1000) + 3600
        };

        const mockToken = [
            btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' })),
            btoa(JSON.stringify(payload)),
            'mock-signature'
        ].join('.');

        cy.intercept('POST', '**/api/auth/login', {
            statusCode: 200,
            body: mockToken
        }).as('successfulLogin');

        cy.get('input[name="email"]').clear().type('test.cypress1@test.cy');

        cy.get('input[name="password"]').clear().type('Test.cypress1');
        cy.get('input[name="remember-me"]').check();

        cy.intercept('GET', '**/api/notifications', {
            statusCode: 200,
            body: []
        });

        cy.intercept('GET', '**/api/notifications/unread-count', {
            statusCode: 200,
            body: 0
        });

        cy.intercept('GET', '**/api/announcements*', {
            statusCode: 200,
            body: {
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: 0,
                size: 10
            }
        });
        cy.get('button[type="submit"]').click();

        cy.wait('@successfulLogin').then((interception) => {
            expect(interception.request.body).to.deep.equal({
                email: 'test.cypress1@test.cy',
                password: 'Test.cypress1'
            });
        });
        cy.location('pathname').should('eq', '/');

        cy.window().then((win) => {
            expect(win.localStorage.getItem('jwt_token')).to.eq(mockToken);
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });
        cy.contains('Witaj, Test');
    });

    it('Should display a message when the session has expired', () => {
        cy.visit('/login?expired=true');

        cy.contains('Sesja wygasła. Zaloguj się ponownie.')
            .should('be.visible');
    });
});