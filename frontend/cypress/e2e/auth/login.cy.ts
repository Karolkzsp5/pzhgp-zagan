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
        cy.get('a').should('contain.text', 'Zarejestruj się tutaj').and('be.visible');
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
    });

    it('Should prevent user from logging in if their account has not been approved by an administrator', () => {
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

    it('Should log the user in and redirect them to the home page', () => {
        cy.get('input[name="email"]').clear().type('test.cypress1@test.cy');
        cy.get('input[name="password"]').clear().type('Test.cypress1');
        cy.get('input[name="remember-me"]').check();
        cy.get('button[type="submit"]').click();

        cy.location('pathname').should('eq', '/');

        cy.window().its('localStorage').invoke('getItem', 'jwt_token').should('exist');
    });
});