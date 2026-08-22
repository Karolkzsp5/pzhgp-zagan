describe('Breeder Registration Process Test', () => {

    const mockSections = [
        { id: 1, name: 'Żagań' },
        { id: 2, name: 'Wymiarki' },
        { id: 3, name: 'Chotków' },
        { id: 4, name: 'Kożuchów' }
    ];

    beforeEach(() => {
        cy.intercept('GET', '**/api/sections', {
            statusCode: 200,
            body: mockSections
        }).as('getSections');

        cy.visit('/register');

        cy.wait('@getSections');
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

    it('Name test - Should ignore any letters and special characters included in the Name input', () => {
        cy.get('input[name="name"]').clear().type('T1o@m3a$s5z', { delay: 20 }).should('have.value', 'Tomasz');
    });

    it('Name test - Should accept Polish characters in first name', () => {
        cy.get('input[name="name"]').type('Łukasz').should('have.value', 'Łukasz');
    });

    ////////////////////////
    //////Surame tests//////
    ////////////////////////

    it('Surname test - Should ignore any letters and special characters included in the Surname input', () => {
        cy.get('input[name="surname"]').clear().type('N1o@w3a$k', { delay: 20 }).should('have.value', 'Nowak');
    });

    it('Surname test - Should accept Polish characters in last name', () => {
        cy.get('input[name="surname"]').type('Chrzęściewski').should('have.value', 'Chrzęściewski');
    });

    /////////////////////////
    ///Date of Birth tests///
    /////////////////////////

    it('Date of birth test - Should accept a valid date in YYYY-MM-DD format', () => {
        cy.get('input[name="dateOfBirth"]').clear().type('1990-08-25').should('have.value', '1990-08-25');
    });

    it('Date of birth test - Should trigger HTML5 validation when left empty', () => {
        cy.get('input[name="dateOfBirth"]').clear();
        cy.get('button[type="submit"]').click();

        cy.get('input[name="dateOfBirth"]').should('match', ':invalid');
    });

    ////////////////////////
    //////E-mail tests//////
    ////////////////////////

    it('E-mail test - Should reject an invalid email address', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        // without username
        cy.get('input[name="email"]').clear().type('@example.com');
        cy.get('button[type="submit"]').click();
        cy.contains('Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).').should('be.visible');

        // without '@' sign
        cy.get('input[name="email"]').clear().type('testcypressexample.com');
        cy.get('button[type="submit"]').click();
        cy.contains('Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).').should('be.visible');

        // without domain name
        cy.get('input[name="email"]').clear().type('testcypress@');
        cy.get('button[type="submit"]').click();
        cy.contains('Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).').should('be.visible');

        cy.get('input[name="email"]').clear().type('testcypress@.com');
        cy.get('button[type="submit"]').click();
        cy.contains('Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).').should('be.visible');

        // without '@' sign and domain name
        cy.get('input[name="email"]').clear().type('testcypress');
        cy.get('button[type="submit"]').click();
        cy.contains('Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).').should('be.visible');
    });

    it('E-mail test - Should accept a valid email address', () => {
        cy.get('input[name="email"]').type('testcypress@test.com').should('have.value', 'testcypress@test.com');
    });

    it('E-mail test - Should return an error: "User with this email address already exists."', () => {
        cy.intercept('POST', '**/api/auth/register', {
            statusCode: 400,
            body: 'Użytkownik z podanym adresem e-mail już istnieje.'
        }).as('duplicateEmailRequest');

        cy.get('form').invoke('attr', 'novalidate', '');

        cy.fillPhoneNumber();
        cy.fillPassword();
        cy.selectSection();
        cy.fillHouseNumber();

        cy.get('input[name="email"]').type('test.cypress0@test.cy');
        cy.get('button[type="submit"]').click();
        cy.wait('@duplicateEmailRequest');
        cy.contains('Użytkownik z podanym adresem e-mail już istnieje.').should('be.visible');
    });

    ////////////////////////
    ///Phone number tests///
    ////////////////////////

    it('Phone number test - Should format phone number correctly as it is entered', () => {
        cy.get('input[name="phoneNumber"]').clear().type('123456789', { delay: 20 }).should('have.value', '123 456 789');
    });

    it('Phone number test - Should correctly handle a phone number containing spaces', () => {
        cy.get('input[name="phoneNumber"]').clear().type('123 456 789').should('have.value', '123 456 789');
    });

    it('Phone number test - Should ignore any letters and special characters included in the phone number', () => {
        cy.get('input[name="phoneNumber"]').clear().type('1a2@3c4$5e6^7g8*9', { delay: 20 }).should('have.value', '123 456 789');
    });

    it('Phone number test - Should prevent registration when phone number has less than 9 digits', () => {
        cy.get('form').invoke('attr', 'novalidate', '');
        cy.fillEmail();

        cy.get('input[name="phoneNumber"]').clear().type('123');
        cy.get('button[type="submit"]').click();
        cy.contains('Numer telefonu musi zawierać dokładnie 9 cyfr.').should('be.visible');
    });

    it('Phone number test - Should prevent registration when user with the same phone number already exists', () => {
        cy.intercept('POST', '**/api/auth/register', {
            statusCode: 400,
            body: 'Użytkownik z podanym numerem telefonu już istnieje.'
        }).as('duplicatePhoneRequest');

        cy.get('form').invoke('attr', 'novalidate', '');

        cy.fillEmail();
        cy.get('input[name="password"]').type('Testcypress1@', { delay: 20 });
        cy.get('input[name="confirmPassword"]').type('Testcypress1@');
        cy.selectSection();
        cy.fillHouseNumber();

        cy.get('input[name="phoneNumber"]').clear().type('123456789');
        cy.get('button[type="submit"]').click();
        cy.wait('@duplicatePhoneRequest');
        cy.contains('Użytkownik z podanym numerem telefonu już istnieje.').should('be.visible');
    });

    ////////////////////////
    /////Password tests/////
    ////////////////////////

    it('Password test - Should check the strength of the password', () => {
        cy.get('form').invoke('attr', 'novalidate', '');
        cy.fillEmail();
        cy.fillPhoneNumber()

        // No capital letter
        cy.get('input[name="password"]').clear().type('testcypress1@');
        cy.get('input[name="confirmPassword"]').clear().type('testcypress1@');
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi zawierać co najmniej jedną dużą literę.').should('be.visible');

        // No small letter
        cy.get('input[name="password"]').clear().type('TESTCYPRESS1@');
        cy.get('input[name="confirmPassword"]').clear().type('TESTCYPRESS1@');
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi zawierać co najmniej jedną małą literę.').should('be.visible');

        // No digit
        cy.get('input[name="password"]').clear().type('Testcypress@');
        cy.get('input[name="confirmPassword"]').clear().type('Testcypress@');
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi zawierać co najmniej jedną cyfrę.').should('be.visible');

        // No special character
        cy.get('input[name="password"]').clear().type('Testcypress1');
        cy.get('input[name="confirmPassword"]').clear().type('Testcypress1');
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi zawierać co najmniej jeden znak specjalny.').should('be.visible');

        // Too short password
        cy.get('input[name="password"]').clear().type('Test');
        cy.get('input[name="confirmPassword"]').clear().type('Test');
        cy.get('button[type="submit"]').click();
        cy.contains('Hasło musi zawierać co najmniej 8 znaków.').should('be.visible');
    });

    it('Password test - Should return an error if the passwords are not the same', () => {
        cy.get('form').invoke('attr', 'novalidate', '');
        cy.fillEmail();
        cy.fillPhoneNumber();

        cy.get('input[name="password"]').clear().type('Testcypress1@', { delay: 20 });
        cy.get('input[name="confirmPassword"]').clear().type('Testcypress');
        cy.selectSection();

        cy.get('button[type="submit"]').click();
        cy.contains('Podane hasła nie są identyczne.').should('be.visible');
    });

    ////////////////////////
    /////Section tests//////
    ////////////////////////

    it('Section test - Should prevent registration when section is not selected', () => {
        cy.get('form').invoke('attr', 'novalidate', '');

        cy.fillEmail();
        cy.fillPhoneNumber();
        cy.fillPassword();

        cy.get('button[type="submit"]').click();
        cy.contains('Proszę wybrać sekcję do której chcesz należeć').should('be.visible');
    });

    ////////////////////////
    ////Postal code tests///
    ////////////////////////

    it('Postal code test - Should ignore any letters and special characters', () => {
        cy.get('input[name="postalCode"]').invoke('removeAttr', 'maxlength').clear()
            .type('1f1&-1a1@1u', { delay: 20 }).should('have.value', '11-111');
    });

    ////////////////////////
    ///////City tests///////
    ////////////////////////

    it('City test - Should ignore any letters and special characters', () => {
        cy.get('input[name="city"]').clear().type('D1ł@u3g$i5e', { delay: 20 }).should('have.value', 'Długie');
    });

    it('City test - Should accept Polish characters', () => {
        cy.get('input[name="city"]').clear().type('Długie').should('have.value', 'Długie');
    });

    ////////////////////////
    //////Street tests//////
    ////////////////////////

    it('Street test - Should ignore any letters and special characters', () => {
        cy.get('input[name="street"]').clear().type('P1o@l3n$a', { delay: 20 }).should('have.value', 'Polna');
    });

    it('Street test - Should accept Polish characters', () => {
        cy.get('input[name="street"]').clear().type('Żabia').should('have.value', 'Żabia');
    });

    ////////////////////////
    ///House number tests///
    ////////////////////////

    it('House number test - Should ignore any special characters except "/" and "-"', () => {
        cy.get('input[name="houseNumber"]').clear().type('1@#4a/%8*c', { delay: 20 }).should('have.value', '14a/8c');
    });

    it('House number test - Should ignore Polish characters', () => {
        cy.get('input[name="houseNumber"]').clear().type('1ąć4a/ó8ńc', { delay: 20 }).should('have.value', '14a/8c');
    });


    ///////////////////////////
    /////Registration test/////
    ///////////////////////////

    it('Should successfully register the user', () => {
        cy.intercept('POST', '**/api/auth/register', {
            statusCode: 201,
            body: ''
        }).as('successfulRegistration');

        cy.get('input[name="name"]').type('Tomasz', { delay: 50 });
        cy.get('input[name="surname"]').type('Nowak');
        cy.get('input[name="dateOfBirth"]').clear().type('2003-01-12');
        cy.get('input[name="email"]').type('testcypress@test.com');
        cy.get('input[name="phoneNumber"]').type('444444444');
        cy.get('input[name="password"]').type('Testcypress1@');
        cy.get('input[name="confirmPassword"]').type('Testcypress1@');
        cy.get('select[name="sectionId"]').select('1');
        cy.get('input[name="postalCode"]').type('11-111');
        cy.get('input[name="city"]').type('Test');
        cy.get('input[name="street"]').type('Testowa');
        cy.get('input[name="houseNumber"]').type('15b');

        cy.get('button[type="submit"]').click();
        cy.wait('@successfulRegistration');

        cy.location('search').should('include', 'registered=true');
        cy.contains('Rejestracja przebiegła pomyślnie').should('be.visible');
        cy.contains('Twoje konto zostało pomyślnie utworzone i oczekuje na akceptację administratora.').should('be.visible');

        cy.contains('button', 'Rozumiem').click();
        cy.contains('Rejestracja przebiegła pomyślnie').should('not.exist');
    });
});