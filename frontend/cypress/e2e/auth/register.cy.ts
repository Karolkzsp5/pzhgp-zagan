describe('Breeder Registration Process', () => {
    beforeEach(() => {
        cy.visit('/register');
    });

    it('Should display a registration form with the relevant fields', () => {
        cy.get('h1').should('contain.text', 'Rejestracja Hodowcy PZHGP');

        cy.contains('label', 'Imię').should('be.visible');
        cy.get('input[name="name"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Nazwisko').should('be.visible');
        cy.get('input[name="surname"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Data urodzenia').should('be.visible');
        cy.get('input[name="dateOfBirth"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Adres E-mail').should('be.visible');
        cy.get('input[name="email"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Numer telefonu').should('be.visible');
        cy.get('input[name="phoneNumber"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Hasło').should('be.visible');
        cy.get('input[name="password"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Powtórz hasło').should('be.visible');
        cy.get('input[name="confirmPassword"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Wybierz sekcję do której chcesz należeć').should('be.visible');
        cy.get('select[name="sectionId"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Kod pocztowy').should('be.visible');
        cy.get('input[name="postalCode"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Miejscowość').should('be.visible');
        cy.get('input[name="city"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Ulica').should('be.visible');
        cy.get('input[name="street"]').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Numer domu/lokalu').should('be.visible');
        cy.get('input[name="houseNumber"]').should('be.visible').and('have.attr', 'required');

        cy.get('button[type="submit"]').should('be.visible').and('not.be.disabled');

        cy.contains('Masz już konto?').should('be.visible');
        cy.get('a').should('contain.text', 'Zaloguj się tutaj').and('be.visible');
    });

    ////////////////////////
    ///////Name tests///////
    ////////////////////////

    // it('Name test - Should ignore any letters and special characters included in the "name" input', () => {
    //     cy.get('input[name="name"]').type('T1o@m3a$s5z', { delay: 50 });
    //     cy.get('input[name="name"]').should('have.value', 'Tomasz');
    // });

    ////////////////////////
    //////Surame tests//////
    ////////////////////////

    // it('Surname test - Should ignore any letters and special characters included in the "Surname" input', () => {
    //     cy.get('input[name="surname"]').type('N1o@w3a$k');
    //     cy.get('input[name="surname"]').should('have.value', 'Nowak');
    // });

    ////////////////////////
    //////E-mail tests//////
    ////////////////////////

    it('E-mail test - Should return a backend error (e.g. User with this email address already exists.)', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        cy.get('input[name="phoneNumber"]').type('123456789');
        cy.get('input[name="password"]').type('Testcypress1@');
        cy.get('input[name="confirmPassword"]').type('Testcypress1@');
        cy.selectSection();

        cy.get('input[name="email"]').type('jan.nowak@example.com');
        cy.get('button[type="submit"]').click();
        cy.wait(2000);
        cy.contains('Użytkownik z podanym adresem e-mail już istnieje.').should('be.visible');
    });

    ////////////////////////
    ///Phone number tests///
    ////////////////////////

    // it('Phone number test - Should format phone number correctly as it is entered', () => {
    //     cy.get('input[name="phoneNumber"]').type('123456789');
    //     cy.get('input[name="phoneNumber"]').should('have.value', '123 456 789');
    // });
    //
    // it('Phone number test - Should correctly handle a phone number containing spaces', () => {
    //     cy.get('input[name="phoneNumber"]').type('123 456 789');
    //     cy.get('input[name="phoneNumber"]').should('have.value', '123 456 789');
    // });
    //
    // it('Phone number test - Should ignore any letters and special characters included in the phone number', () => {
    //     cy.get('input[name="phoneNumber"]').clear().type('1a2@3c4$5e6^7g8*9');
    //     cy.get('input[name="phoneNumber"]').should('have.value', '123 456 789');
    // });

    it('Phone number test - Should prevent registration when phone number has less than 9 digits', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        cy.get('input[name="password"]').type('Testcypress1@');
        cy.get('input[name="confirmPassword"]').type('Testcypress1@');
        cy.selectSection();

        cy.get('input[name="phoneNumber"]').clear().type('123');
        cy.get('button[type="submit"]').click();
        cy.contains('Numer telefonu musi zawierać dokładnie 9 cyfr.').should('be.visible');
    });

    it('Phone number test - Should prevent registration when user with the same phone number already exists', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        cy.get('input[name="password"]').type('Testcypress1@');
        cy.get('input[name="confirmPassword"]').type('Testcypress1@');
        cy.selectSection();

        cy.get('input[name="phoneNumber"]').clear().type('123456789');
        cy.get('button[type="submit"]').click();
        cy.contains('Użytkownik z podanym numerem telefonu już istnieje.').should('be.visible');
    });

    ////////////////////////
    /////Password tests/////
    ////////////////////////

    it('should return an error if the passwords are not the same', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        cy.get('input[name="password"]').type('Testcypress1@', { delay: 20 });
        cy.get('input[name="confirmPassword"]').type('Testcypress');
        cy.selectSection();

        cy.get('button[type="submit"]').click();
        cy.contains('Podane hasła nie są identyczne.').should('be.visible');
    });

    it('Should check the strength of the password', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        // Too short password
        cy.get('input[name="password"]').type('Haslo1!');
        cy.get('input[name="confirmPassword"]').type('Haslo1!');
        cy.selectSection();
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi mieć co najmniej 8 znaków.').should('be.visible');
        cy.get('input[name="password"]').clear();

        // No special character
        cy.get('input[name="password"]').type('HasloBezZnaku123');
        cy.get('input[name="confirmPassword"]').clear().type('HasloBezZnaku123');
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi zawierać co najmniej jeden znak specjalny.').should('be.visible');
    });

    ////////////////////////
    /////Section tests//////
    ////////////////////////

//     it('Should prevent registration when section is not selected', () => {
//         cy.get('input[name="name"]').type('Tomasz', { delay: 50 });
//         cy.get('input[name="surname"]').type('Nowak');
//         cy.get('input[name="dateOfBirth"]').clear().type('2003-01-12');
//         cy.get('input[name="email"]').type('testcypress1@test.com');
//         cy.get('input[name="phoneNumber"]').type('123456789');
//         cy.get('input[name="password"]').type('Testcypress1@');
//         cy.get('input[name="confirmPassword"]').type('Testcypress1@');
//
//         cy.get('input[name="postalCode"]').type('11-111');
//         cy.get('input[name="city"]').type('Test');
//         cy.get('input[name="street"]').type('Testowa');
//         cy.get('input[name="houseNumber"]').type('15b');
//
//         cy.get('button[type="submit"]').click();
//         cy.contains('Proszę wybrać sekcję do której chcesz należeć').should('be.visible');
//     });

    ////////////////////////
    ////Postal code tests///
    ////////////////////////

//
//     it('Powinien pomyślnie zarejestrować użytkownika i wyświetlić zielony komunikat', () => {
//         cy.intercept('POST', 'http://localhost:8080/api/auth/register', {
//             statusCode: 201,
//             body: 'Konto zostało pomyślnie utworzone. Oczekuje na akceptację administratora.',
//         }).as('registerSuccess');
//
//         cy.get('input[name="name"]').type('Tomasz', { delay: 50 });
//         cy.get('input[name="surname"]').type('Nowak');
//         cy.get('input[name="dateOfBirth"]').clear().type('2003-01-12');
//         cy.get('input[name="email"]').type('testcypress1@test.com');
//         cy.get('input[name="phoneNumber"]').type('123456789');
//         cy.get('input[name="password"]').type('Testcypress1@');
//         cy.get('input[name="confirmPassword"]').type('Testcypress1@');
//         cy.get('select[name="sectionId"]').select('1');
//         cy.get('input[name="postalCode"]').type('11-111');
//         cy.get('input[name="city"]').type('Test');
//         cy.get('input[name="street"]').type('Testowa');
//         cy.get('input[name="houseNumber"]').type('15b');
//         cy.get('button[type="submit"]').click();
//
//         cy.wait('@registerSuccess');
//
//         cy.contains('Konto zostało pomyślnie utworzone. Oczekuje na akceptację administratora.')
//             .should('be.visible')
//             .and('have.class', 'text-green-700');
//     });
});