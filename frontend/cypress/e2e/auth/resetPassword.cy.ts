describe('Password Reset Test', () => {
    const visitResetPage = (query = '?token=test-reset-token') => {
        // Niezalogowana sesja przy wejściu na stronę.
        cy.intercept('GET', '**/api/auth/me', {
            statusCode: 401,
            body: 'Wymagane jest uwierzytelnienie.'
        }).as('initialCurrentUser');

        cy.visit(`/reset-password${query}`);
        cy.wait('@initialCurrentUser');
    };

    const fillPasswords = (password: string, confirmPassword = password) => {
        cy.get('input[name="password"]').clear().type(password);
        cy.get('input[name="confirmPassword"]').clear().type(confirmPassword);
    };

    it('Should display the new password form', () => {
        visitResetPage();

        cy.get('h1').should('be.visible').and('contain.text', 'Ustaw nowe hasło');

        cy.contains('label', 'Nowe hasło').should('be.visible');
        cy.get('input[name="password"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Powtórz hasło').should('be.visible');
        cy.get('input[name="confirmPassword"]').should('be.visible').and('have.attr', 'required');

        cy.get('button[type="submit"]').should('be.visible').and('not.be.disabled').and('contain.text', 'Zmień hasło');
        cy.contains('a', 'Przejdź do logowania').should('be.visible').and('have.attr', 'href', '/login');
    });

    it('Should check the strength of the password and apply the registration password rules', () => {
        cy.intercept('POST', '**/api/auth/reset-password', cy.spy().as('resetPasswordRequest'));
        visitResetPage();

        cy.get('input[name="password"]').type('test');
        cy.contains('Siła hasła:').should('be.visible');
        cy.contains('strong', 'Bardzo słabe').should('be.visible');

        fillPasswords('testtest');
        cy.get('button[type="submit"]').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Hasło musi zawierać co najmniej jedną dużą literę.');

        fillPasswords('Testtest1');
        cy.get('button[type="submit"]').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Hasło musi zawierać co najmniej jeden znak specjalny.');

        cy.get('input[name="password"]').clear().type('Testcypress1@');
        cy.contains('strong', 'Bardzo silne').should('be.visible');

        cy.get('@resetPasswordRequest').should('not.have.been.called');
    });

    it('Should return an error if the passwords are not the same', () => {
        cy.intercept('POST', '**/api/auth/reset-password', cy.spy().as('resetPasswordRequest'));
        visitResetPage();

        fillPasswords('Testcypress1@', 'Testcypress2@');
        cy.get('button[type="submit"]').click();

        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Podane hasła nie są identyczne.');
        cy.get('@resetPasswordRequest').should('not.have.been.called');
    });

    it('Should send the token from the link with the new password and display a confirmation', () => {
        cy.intercept('POST', '**/api/auth/reset-password', {
            statusCode: 204
        }).as('resetPasswordRequest');
        visitResetPage();

        fillPasswords('Testcypress1@');
        cy.get('button[type="submit"]').click();

        cy.wait('@resetPasswordRequest').then((interception) => {
            expect(interception.request.body).to.deep.equal({
                token: 'test-reset-token',
                password: 'Testcypress1@'
            });
        });

        cy.get('[role="status"]').should('be.visible').and('contain.text', 'Hasło zostało zmienione. Możesz się teraz zalogować.');
        cy.get('form').should('not.exist');

        cy.contains('a', 'Przejdź do logowania').click();
        cy.location('pathname').should('eq', '/login');
    });

    it('Should display the server error when the link has expired', () => {
        cy.intercept('POST', '**/api/auth/reset-password', {
            statusCode: 400,
            body: 'Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link.'
        }).as('expiredLinkRequest');
        visitResetPage();

        fillPasswords('Testcypress1@');
        cy.get('button[type="submit"]').click();

        cy.wait('@expiredLinkRequest');
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Link do zmiany hasła jest nieprawidłowy lub wygasł. Poproś o nowy link.');
    });

    it('Should display an error instead of the form when the link has no token', () => {
        visitResetPage('');

        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Link do zmiany hasła jest nieprawidłowy.');
        cy.contains('a', 'Poproś o nowy link').should('be.visible').and('have.attr', 'href', '/forgot-password');
        cy.get('form').should('not.exist');
    });
});