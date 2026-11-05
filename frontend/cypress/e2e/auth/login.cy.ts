import { createTestUser } from '../../support/testUtils';

describe('Breeder Login Process', () => {
    beforeEach(() => {
        // Niezalogowana sesja przy pierwszym wejściu na stronę.
        cy.intercept('GET', '**/api/auth/me', {
            statusCode: 401,
            body: 'Wymagane jest uwierzytelnienie.'
        }).as('initialCurrentUser');

        cy.visit('/login', {
            onBeforeLoad: (win) => {
                win.document.cookie = 'XSRF-TOKEN=cypress-test-csrf; Path=/; SameSite=Lax';
            }
        });

        cy.window().then((win) => {
            return new Cypress.Promise<void>((resolve) => {
                win.requestAnimationFrame(() => {
                    win.requestAnimationFrame(() => resolve());
                });
            });
        });
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
        cy.intercept('POST', '**/api/auth/login-cookie*', {
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
        cy.intercept('POST', '**/api/auth/login-cookie*', {
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
        cy.intercept('POST', '**/api/auth/login-cookie*', {
            statusCode: 403,
            body: 'Twoje konto zostało zablokowane.'
        }).as('blockedAccountRequest');

        cy.get('input[name="email"]').clear().type('blocked.user@test.com');
        cy.get('input[name="password"]').clear().type('ValidPassword1!');
        cy.get('button[type="submit"]').click();

        cy.wait('@blockedAccountRequest');
        cy.contains('Twoje konto zostało zablokowane.').should('be.visible');
        cy.location('pathname').should('eq', '/login');
        cy.window().then((win) => {
            expect(win.localStorage.getItem('jwt_token')).to.be.null;
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });
    });

    const mockSuccessfulLogin = () => {
        const testUser = createTestUser('BREEDER', 'test.cypress1@test.cy', 'Test');
        let authenticated = false;

        cy.intercept('GET', '**/api/auth/me', (request) => {
            request.reply(authenticated
                ? { statusCode: 200, body: testUser }
                : { statusCode: 401, body: 'Wymagane jest uwierzytelnienie.' });
        }).as('currentUser');

        cy.intercept('POST', '**/api/auth/login-cookie*', (request) => {
            authenticated = true;
            request.reply({ statusCode: 204 });
        }).as('successfulLogin');

        cy.intercept('GET', '**/api/announcements*', {
            statusCode: 200,
            body: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 }
        }).as('getHomeAnnouncements');

        cy.mockNavbarNotifications();
        // Po udanym logowaniu frontend prosi o świeży token CSRF.
        cy.intercept('GET', '**/api/auth/csrf', { statusCode: 204 }).as('refreshCsrf');
    };

    it('Should log in with a session cookie when "Remember me" is not selected', () => {
        mockSuccessfulLogin();

        cy.get('input[name="email"]').type('test.cypress1@test.cy');
        cy.get('input[name="password"]').type('Test.cypress1');
        cy.get('input[name="remember-me"]').should('not.be.checked');
        cy.get('button[type="submit"]').click();

        cy.wait('@successfulLogin').then(({ request }) => {
            expect(new URL(request.url).searchParams.get('rememberMe')).to.eq('false');
            expect(request.body).to.deep.equal({ email: 'test.cypress1@test.cy', password: 'Test.cypress1' });
            expect(request.headers['x-xsrf-token']).to.eq('cypress-test-csrf');
        });
        cy.location('pathname').should('eq', '/');
        cy.wait(['@getHomeAnnouncements', '@getNotifications', '@getUnreadCount']);
        cy.contains('Witaj, Test');

        cy.window().then((win) => {
            expect(win.localStorage.getItem('jwt_token')).to.be.null;
            expect(win.localStorage.getItem('auth_persistence')).to.be.null;
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });
    });

    it('Should log in and pass rememberMe=true to the cookie endpoint', () => {
        mockSuccessfulLogin();

        cy.get('input[name="email"]').type('test.cypress1@test.cy');
        cy.get('input[name="password"]').type('Test.cypress1');
        cy.get('input[name="remember-me"]').check();
        cy.get('button[type="submit"]').click();

        cy.wait('@successfulLogin').then(({ request }) => {
            expect(new URL(request.url).searchParams.get('rememberMe')).to.eq('true');
            expect(request.headers['x-xsrf-token']).to.eq('cypress-test-csrf');
        });
        cy.location('pathname').should('eq', '/');
        cy.contains('Witaj, Test');

        cy.window().then((win) => {
            expect(win.localStorage.getItem('jwt_token')).to.be.null;
            expect(win.localStorage.getItem('auth_persistence')).to.be.null;
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });
    });

    it('Should display a message when the session has expired', () => {
        cy.visit('/login?expired=true');
        cy.contains('Sesja wygasła. Zaloguj się ponownie.').should('be.visible');
    });
});