describe('Proces Rejestracji Hodowcy', () => {
    beforeEach(() => {
        cy.visit('/register');
    });

    it('Should display a registration form with the relevant fields', () => {
        cy.get('h1').should('contain.text', 'Rejestracja Hodowcy PZHGP');

        cy.get('label').contains('Imię').should("exists").and('be.visible');
        cy.get('input[name="name"]').should('exist').and('be.visible');

        cy.get('label').contains('Nazwisko').should("exists").and('be.visible');
        cy.get('input[name="surname"]').should('exist').and('be.visible');

        cy.get('label').contains('Data urodzenia').should("exists").and('be.visible');
        cy.get('input[name="dateOfBirth"]').should('exist').and('be.visible');

        cy.get('label').contains('Adres E-mail').should("exists").and('be.visible');
        cy.get('input[name="email"]').should('exist').and('be.visible');

        cy.get('label').contains('Numer telefonu').should("exists").and('be.visible');
        cy.get('input[name="phoneNumber"]').should('exist').and('be.visible');

        cy.get('label').contains('Hasło').should("exists").and('be.visible');
        cy.get('input[name="password"]').should('exist').and('be.visible');

        cy.get('label').contains('Powtórz hasło').should("exists").and('be.visible');
        cy.get('input[name="confirmPassword"]').should('exist').and('be.visible');

        cy.get('label').contains('Wybierz sekcję do której chcesz należeć').should("exists").and('be.visible');
        cy.get('select[name="sectionId"]').should('exist').and('be.visible');

        cy.get('label').contains('Kod pocztowy').should("exists").and('be.visible');
        cy.get('input[name="postalCode"]').should('exist').and('be.visible');

        cy.get('label').contains('Miejscowość').should("exists").and('be.visible');
        cy.get('input[name="city"]').should('exist').and('be.visible');

        cy.get('label').contains('Ulica').should("exists").and('be.visible');
        cy.get('input[name="street"]').should('exist').and('be.visible');

        cy.get('button[type="submit"]').should('be.visible').and('not.be.disabled');

        cy.contains('Masz już konto?').should("exists").and('be.visible');
        cy.get('a').should('contain.text', 'Zaloguj się tutaj').and("exists").and('be.visible');
    });
});