describe('Breeder Login Process', () => {
    beforeEach(() => {
        cy.visit('/login');
    });

    it('Should display a login form with the relevant fields', () => {
        cy.get('h1').should("exists").and('be.visible').and('contain.text', 'Logowanie Hodowcy');
        cy.get('form').should("exists").and('be.visible');
        cy.get('input[name="email"]').should("exists").and('be.visible');
        cy.get('input[name="password"]').should("exists").and('be.visible');
        cy.get('input[name="remember-me"]').should("exists").and('be.visible');
        cy.get('label[for="remember-me"]').should("exists").and('be.visible');
        cy.get('button[type="submit"]').should("exists").and('be.visible').and('not.be.disabled');
        cy.contains('Nie masz jeszcze konta?').should("exists").and('be.visible');
        cy.get('a').should('contain.text', 'Zarejestruj się tutaj').and("exists").and('be.visible');
    });

    it('Should display an error message if the login details are incorrect', () => {
        cy.intercept('POST', 'http://localhost:8080/api/auth/login', {
            statusCode: 401,
            body: 'Nieprawidłowy adres e-mail lub hasło.',
        }).as('loginRequest');

        cy.get('input[name="email"]').type('zly@adres.pl');
        cy.get('input[name="password"]').type('ZleHaslo123!');
        cy.get('button[type="submit"]').click();

        cy.wait('@loginRequest');

        cy.contains('Nieprawidłowy adres e-mail lub hasło.').should('be.visible');
        cy.get('button[type="submit"]').should('not.be.disabled');
    });

    it('Should log the user in and redirect them to the home page', () => {
        cy.intercept('POST', 'http://localhost:8080/api/auth/login', {
            statusCode: 200,
            body: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mockedToken',
        }).as('loginSuccess');

        cy.get('input[name="email"]').type('poprawny@email.pl');
        cy.get('input[name="password"]').type('DobreHaslo123!');
        cy.get('input[name="remember-me"]').check();
        cy.get('button[type="submit"]').click();

        cy.wait('@loginSuccess');

        cy.url().should('eq', Cypress.config().baseUrl + '/');
        cy.window().its('localStorage').invoke('getItem', 'jwt_token').should('exist');
    });
});