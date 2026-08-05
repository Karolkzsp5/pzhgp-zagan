describe('Breeder Registration Process', () => {
    beforeEach(() => {
        cy.visit('/register');
    });

    it('Should display a registration form with the relevant fields', () => {
        cy.get('h1').should('contain.text', 'Rejestracja Hodowcy PZHGP');

        cy.contains('label', 'Imię').should('be.visible');
        cy.get('input[name="name"]').should('be.visible');

        cy.contains('label', 'Nazwisko').should('be.visible');
        cy.get('input[name="surname"]').should('be.visible');

        cy.contains('label', 'Data urodzenia').should('be.visible');
        cy.get('input[name="dateOfBirth"]').should('be.visible');

        cy.contains('label', 'Adres E-mail').should('be.visible');
        cy.get('input[name="email"]').should('be.visible');

        cy.contains('label', 'Numer telefonu').should('be.visible');
        cy.get('input[name="phoneNumber"]').should('be.visible');

        cy.contains('label', 'Hasło').should('be.visible');
        cy.get('input[name="password"]').should('be.visible');

        cy.contains('label', 'Powtórz hasło').should('be.visible');
        cy.get('input[name="confirmPassword"]').should('be.visible');

        cy.contains('label', 'Wybierz sekcję do której chcesz należeć').should('be.visible');
        cy.get('select[name="sectionId"]').should('be.visible');

        cy.contains('label', 'Kod pocztowy').should('be.visible');
        cy.get('input[name="postalCode"]').should('be.visible');

        cy.contains('label', 'Miejscowość').should('be.visible');
        cy.get('input[name="city"]').should('be.visible');

        cy.contains('label', 'Ulica').should('be.visible');
        cy.get('input[name="street"]').should('be.visible');

        cy.get('button[type="submit"]').should('be.visible').and('not.be.disabled');

        cy.contains('Masz już konto?').should('be.visible');
        cy.get('a').should('contain.text', 'Zaloguj się tutaj').and('be.visible');
    });
});