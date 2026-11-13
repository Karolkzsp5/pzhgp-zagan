import { createTestUser } from '../support/testUtils';

describe('Settings Page Tests', () => {
    const breederUser = createTestUser('BREEDER', 'jan.kowalski@test.pl', 'Jan');

    const createAccount = () => ({
        name: 'Jan',
        surname: 'Kowalski',
        dateOfBirth: '1985-04-20',
        email: 'jan.kowalski@test.pl',
        phoneNumber: '600100200',
        postalCode: '68-100',
        city: 'Żagań',
        street: 'Słowackiego',
        houseNumber: '10',
        sectionName: 'Wymiarki',
        role: 'BREEDER',
        createdAt: '2024-03-12T12:00:00Z',
        boardFunctions: ['Skarbnik – Zarząd Sekcji Wymiarki']
    });

    const visitSettings = (account: ReturnType<typeof createAccount> = createAccount()) => {
        // Stała data „dzisiaj”, żeby staż konta i nazwa pobieranego pliku nie zależały od dnia uruchomienia testów.
        cy.clock(new Date('2026-11-15T12:00:00Z'), ['Date']);

        cy.intercept('GET', '**/api/account', {
            statusCode: 200,
            body: account
        }).as('getAccount');

        cy.mockNavbarNotifications();
        cy.visitAsUser('/settings', breederUser);
        cy.wait(['@getAccount', '@getNotifications', '@getUnreadCount']);
    };

    const getSection = (title: string) => cy.contains('h2', title).closest('section');

    // ==========================================
    // WYŚWIETLANIE STRONY
    // ==========================================

    it('Should display the personal data, contact details and account details', () => {
        visitSettings();

        cy.checkLoggedInNavbar('Jan');
        cy.get('h1').should('be.visible').and('have.text', 'Ustawienia');

        getSection('Dane osobowe').within(() => {
            cy.contains('Aby je zmienić, skontaktuj się z administratorem.').should('be.visible');
            cy.contains('dd', 'Jan Kowalski').should('be.visible');
            cy.contains('dd', '20.04.1985').should('be.visible');
            cy.contains('dd', 'jan.kowalski@test.pl').should('be.visible');
            cy.contains('dd', 'Wymiarki').should('be.visible');
        });

        getSection('Dane kontaktowe').within(() => {
            cy.contains('dd', '600 100 200').should('be.visible');
            cy.contains('dd', 'ul. Słowackiego 10').should('be.visible').and('contain.text', '68-100 Żagań');
            cy.contains('button', 'Zmień dane kontaktowe').should('be.visible');
        });

        getSection('Konto').within(() => {
            cy.contains('dd', 'Hodowca').should('be.visible');
            cy.contains('dd', '12.03.2024').should('be.visible');
            cy.contains('dd', '2 lata, 8 miesięcy i 3 dni').should('be.visible');
            cy.contains('li', 'Skarbnik – Zarząd Sekcji Wymiarki').should('be.visible');
        });

        getSection('Zmiana hasła').within(() => {
            cy.get('#current-password').should('be.visible').and('have.attr', 'type', 'password');
            cy.get('#new-password').should('be.visible').and('have.attr', 'type', 'password');
            cy.get('#confirm-new-password').should('be.visible').and('have.attr', 'type', 'password');
        });

        cy.contains('button', 'Wyloguj ze wszystkich urządzeń').should('be.visible');
        cy.contains('button', 'Pobierz moje dane').should('be.visible');
        cy.checkFooter();
    });

    it('Should display "Brak" when the user has no board function', () => {
        visitSettings({ ...createAccount(), boardFunctions: [] });

        getSection('Konto').within(() => {
            cy.contains('dd', 'Brak').should('be.visible');
            cy.get('li').should('not.exist');
        });
    });

    it('Should display an error and successfully retry loading the account', () => {
        let attempts = 0;

        cy.intercept('GET', '**/api/account', (req) => {
            attempts++;
            req.reply(attempts === 1
                ? { statusCode: 500, body: 'Wystąpił nieoczekiwany błąd serwera.' }
                : { statusCode: 200, body: createAccount() });
        }).as('getAccountWithRetry');

        cy.mockNavbarNotifications();
        cy.visitAsUser('/settings', breederUser);
        cy.wait('@getAccountWithRetry');

        cy.contains('Wystąpił nieoczekiwany błąd serwera.').should('be.visible');
        cy.contains('button', 'Spróbuj ponownie').click();

        cy.wait('@getAccountWithRetry');
        cy.contains('h2', 'Dane osobowe').should('be.visible');
    });

    // ==========================================
    // DANE KONTAKTOWE
    // ==========================================

    it('Should update the contact details', () => {
        const updatedAccount = { ...createAccount(), phoneNumber: '600999888', postalCode: '68-200', city: 'Szprotawa', street: 'Długa', houseNumber: '5/2' };

        cy.intercept('PUT', '**/api/account/contact', (req) => {
            expect(req.body).to.deep.equal({
                phoneNumber: '600999888',
                postalCode: '68-200',
                city: 'Szprotawa',
                street: 'Długa',
                houseNumber: '5/2'
            });
            expect(req.headers['x-xsrf-token']).to.eq('cypress-test-csrf');

            req.reply({ statusCode: 200, body: updatedAccount });
        }).as('updateContact');

        visitSettings();

        cy.contains('button', 'Zmień dane kontaktowe').click();
        cy.get('#contact-phone').should('have.value', '600 100 200').clear().type('600999888').should('have.value', '600 999 888');
        cy.get('#contact-postal-code').should('have.value', '68-100').clear().type('68200').should('have.value', '68-200');
        cy.get('#contact-city').clear().type('Szprotawa');
        cy.get('#contact-street').clear().type('Długa');
        cy.get('#contact-house-number').clear().type('5/2');
        cy.contains('button', 'Zapisz zmiany').click();

        cy.wait('@updateContact');

        getSection('Dane kontaktowe').within(() => {
            cy.get('[role="status"]').should('be.visible').and('contain.text', 'Dane kontaktowe zostały zapisane.');
            cy.contains('dd', '600 999 888').should('be.visible');
            cy.contains('dd', 'ul. Długa 5/2').should('be.visible').and('contain.text', '68-200 Szprotawa');
            cy.get('form').should('not.exist');
        });
    });

    it('Should validate the contact details before sending them', () => {
        cy.intercept('PUT', '**/api/account/contact', cy.spy().as('updateContact'));
        visitSettings();

        cy.contains('button', 'Zmień dane kontaktowe').click();

        cy.get('#contact-phone').clear().type('600100');
        cy.contains('button', 'Zapisz zmiany').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Numer telefonu musi zawierać dokładnie 9 cyfr.');

        cy.get('#contact-phone').clear().type('600100200');
        cy.get('#contact-house-number').clear().type('0');
        cy.contains('button', 'Zapisz zmiany').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4).');

        cy.get('@updateContact').should('not.have.been.called');
    });

    it('Should display the server error when the phone number is already used', () => {
        cy.intercept('PUT', '**/api/account/contact', {
            statusCode: 409,
            body: 'Użytkownik z podanym numerem telefonu już istnieje.'
        }).as('duplicatePhone');

        visitSettings();

        cy.contains('button', 'Zmień dane kontaktowe').click();
        cy.get('#contact-phone').clear().type('600100300');
        cy.contains('button', 'Zapisz zmiany').click();

        cy.wait('@duplicatePhone');
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Użytkownik z podanym numerem telefonu już istnieje.');
        cy.get('#contact-phone').should('have.value', '600 100 300');
    });

    it('Should cancel editing without saving the changes', () => {
        cy.intercept('PUT', '**/api/account/contact', cy.spy().as('updateContact'));
        visitSettings();

        cy.contains('button', 'Zmień dane kontaktowe').click();
        cy.get('#contact-city').clear().type('Szprotawa');
        cy.contains('button', 'Anuluj').click();

        getSection('Dane kontaktowe').within(() => {
            cy.get('form').should('not.exist');
            cy.contains('dd', '68-100 Żagań').should('be.visible');
        });
        cy.get('@updateContact').should('not.have.been.called');
    });

    // ==========================================
    // ZMIANA HASŁA
    // ==========================================

    it('Should change the password and go to the login page', () => {
        cy.intercept('PUT', '**/api/account/password', (req) => {
            expect(req.body).to.deep.equal({ currentPassword: 'StareHaslo1!', newPassword: 'NoweHaslo1!' });
            req.reply({ statusCode: 204 });
        }).as('changePassword');

        visitSettings();

        cy.get('#current-password').type('StareHaslo1!');
        cy.get('#new-password').type('NoweHaslo1!');
        cy.contains('strong', 'Bardzo silne').should('be.visible');
        cy.get('#confirm-new-password').type('NoweHaslo1!');
        cy.contains('button', 'Zmień hasło').click();

        cy.wait('@changePassword');

        cy.location('pathname').should('eq', '/login');
        cy.location('search').should('eq', '?notice=password-changed');
        cy.get('[role="status"]').should('be.visible').and('contain.text', 'Hasło zostało zmienione. Zaloguj się ponownie.');
    });

    it('Should apply the password rules before sending the request', () => {
        cy.intercept('PUT', '**/api/account/password', cy.spy().as('changePassword'));
        visitSettings();

        cy.get('#new-password').type('NoweHaslo1!');
        cy.get('#confirm-new-password').type('NoweHaslo1!');
        cy.contains('button', 'Zmień hasło').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Podaj obecne hasło.');

        cy.get('#current-password').type('StareHaslo1!');
        cy.get('#new-password').clear().type('nowehaslo1!');
        cy.contains('button', 'Zmień hasło').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Hasło musi zawierać co najmniej jedną dużą literę.');

        cy.get('#new-password').clear().type('NoweHaslo1!');
        cy.get('#confirm-new-password').clear().type('NoweHaslo2!');
        cy.contains('button', 'Zmień hasło').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Podane hasła nie są identyczne.');

        cy.get('#new-password').clear().type('StareHaslo1!');
        cy.get('#confirm-new-password').clear().type('StareHaslo1!');
        cy.contains('button', 'Zmień hasło').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nowe hasło musi się różnić od obecnego.');

        cy.get('@changePassword').should('not.have.been.called');
    });

    it('Should display the server error when the current password is wrong', () => {
        cy.intercept('PUT', '**/api/account/password', {
            statusCode: 400,
            body: 'Obecne hasło jest nieprawidłowe.'
        }).as('wrongCurrentPassword');

        visitSettings();

        cy.get('#current-password').type('ZleHaslo1!');
        cy.get('#new-password').type('NoweHaslo1!');
        cy.get('#confirm-new-password').type('NoweHaslo1!');
        cy.contains('button', 'Zmień hasło').click();

        cy.wait('@wrongCurrentPassword');
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Obecne hasło jest nieprawidłowe.');
        cy.location('pathname').should('eq', '/settings');
    });

    // ==========================================
    // SESJE
    // ==========================================

    it('Should log out of all devices after confirmation', () => {
        cy.intercept('POST', '**/api/account/logout-all', { statusCode: 204 }).as('logoutAll');
        visitSettings();

        cy.contains('button', 'Wyloguj ze wszystkich urządzeń').click();
        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('Zostaniesz wylogowany na wszystkich urządzeniach, także na tym.').should('be.visible');
            cy.contains('button', 'Anuluj').click();
        });
        cy.get('[role="dialog"]').should('not.exist');

        cy.contains('button', 'Wyloguj ze wszystkich urządzeń').click();
        cy.get('[role="dialog"]').within(() => {
            cy.contains('button', 'Wyloguj').click();
        });

        cy.wait('@logoutAll');
        cy.location('pathname').should('eq', '/login');
        cy.get('[role="status"]').should('be.visible').and('contain.text', 'Wylogowano Cię ze wszystkich urządzeń.');
    });

    // ==========================================
    // EKSPORT DANYCH
    // ==========================================

    it('Should download the account data as a JSON file', () => {
        const exportedData = {
            exportedAt: '2026-11-15T12:00:00Z',
            account: createAccount(),
            forumThreads: [{ title: 'Mój wątek', category: 'Loty i treningi', createdAt: '2025-05-01T10:00:00Z' }],
            forumPosts: [],
            announcements: [],
            flights: []
        };

        cy.intercept('GET', '**/api/account/export', { statusCode: 200, body: exportedData }).as('exportData');
        visitSettings();

        cy.contains('button', 'Pobierz moje dane').click();
        cy.wait('@exportData');

        cy.readFile(`${Cypress.config('downloadsFolder')}/pzhgp-moje-dane-2026-11-15.json`).should((file) => {
            expect(file.account.email).to.eq('jan.kowalski@test.pl');
            expect(file.forumThreads[0].title).to.eq('Mój wątek');
        });
    });

    // ==========================================
    // DOSTĘP
    // ==========================================

    it('Should redirect an unauthenticated user to the login page', () => {
        cy.intercept('GET', '**/api/auth/me', {
            statusCode: 401,
            body: 'Wymagane jest uwierzytelnienie.'
        }).as('getUnauthenticatedUser');

        cy.visit('/settings');
        cy.wait('@getUnauthenticatedUser').its('response.statusCode').should('eq', 401);
        cy.location('pathname').should('eq', '/login');
        cy.location('search').should('eq', '?next=%2Fsettings');
    });
});