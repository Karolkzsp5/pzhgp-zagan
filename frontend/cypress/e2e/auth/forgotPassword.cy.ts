describe('Password Reset Request Test', () => {
    const confirmation = 'Jeśli konto z podanym adresem istnieje, wysłaliśmy na nie wiadomość z linkiem do zmiany hasła.';

    beforeEach(() => {
        // Niezalogowana sesja przy wejściu na stronę.
        cy.intercept('GET', '**/api/auth/me', {
            statusCode: 401,
            body: 'Wymagane jest uwierzytelnienie.'
        }).as('initialCurrentUser');

        cy.mockTurnstile();
        cy.visit('/forgot-password');
        cy.wait('@initialCurrentUser');
    });

    it('Should display the password reset request form', () => {
        cy.get('h1').should('be.visible').and('contain.text', 'Odzyskiwanie hasła');
        cy.contains('Podaj adres e-mail przypisany do konta.').should('be.visible');

        cy.contains('label', 'Adres e-mail').should('be.visible');
        cy.get('input[name="email"]').should('be.visible').and('have.attr', 'required');
        cy.get('button[type="submit"]').should('be.visible').and('not.be.disabled').and('contain.text', 'Wyślij link');

        cy.contains('Pamiętasz hasło?').should('be.visible');
        cy.contains('a', 'Zaloguj się tutaj').should('be.visible').and('have.attr', 'href', '/login');
    });

    it('Should send the e-mail address with the CAPTCHA token and display a confirmation', () => {
        cy.intercept('POST', '**/api/auth/forgot-password', {
            statusCode: 204
        }).as('forgotPasswordRequest');

        cy.get('input[name="email"]').type('test.cypress1@test.cy');
        cy.get('button[type="submit"]').click();

        cy.wait('@forgotPasswordRequest').then((interception) => {
            expect(interception.request.body).to.deep.equal({
                email: 'test.cypress1@test.cy',
                captchaToken: 'cypress-captcha-token'
            });
        });

        cy.get('[role="status"]').should('be.visible').and('contain.text', confirmation);
        cy.get('form').should('not.exist');
    });

    it('Should prevent sending the request until the CAPTCHA verification is completed', () => {
        cy.intercept('POST', '**/api/auth/forgot-password', cy.spy().as('forgotPasswordRequest'));

        cy.intercept('GET', '**/api/auth/me', {
            statusCode: 401,
            body: 'Wymagane jest uwierzytelnienie.'
        }).as('currentUserAfterReload');

        cy.mockTurnstile(null);
        cy.visit('/forgot-password');
        cy.wait('@currentUserAfterReload');

        cy.get('input[name="email"]').type('test.cypress1@test.cy');
        cy.get('button[type="submit"]').click();

        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Dokończ weryfikację CAPTCHA przed wysłaniem formularza.');
        cy.get('@forgotPasswordRequest').should('not.have.been.called');
    });

    it('Should display the server error when the attempt limit is exceeded', () => {
        cy.intercept('POST', '**/api/auth/forgot-password', {
            statusCode: 429,
            body: 'Zbyt wiele prób odzyskiwania hasła. Spróbuj ponownie później.'
        }).as('rateLimitedRequest');

        cy.get('input[name="email"]').type('test.cypress1@test.cy');
        cy.get('button[type="submit"]').click();

        cy.wait('@rateLimitedRequest');
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Zbyt wiele prób odzyskiwania hasła. Spróbuj ponownie później.');
        cy.get('input[name="email"]').should('have.value', 'test.cypress1@test.cy');
    });
});