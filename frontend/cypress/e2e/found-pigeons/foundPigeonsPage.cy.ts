import { createFakeToken } from '../../support/testUtils';

describe('Found Pigeons Page Tests', () => {
    const adminToken = createFakeToken('ADMINISTRATOR', 'admin@pzhgp.pl', 'Admin');
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    beforeEach(() => {
        cy.visit('/found-pigeons');
    });

    // ==========================================
    // PAGE RENDERING
    // ==========================================

    it('Should correctly render all visible elements of the public report form', () => {
        cy.checkLoggedOutNavbar();

        cy.get('[role="group"][aria-label="Język"]').should('be.visible');
        cy.contains('button', 'Polski').should('be.visible').and('have.attr', 'aria-pressed', 'true');
        cy.contains('button', 'English').should('be.visible').and('have.attr', 'aria-pressed', 'false');
        cy.contains('button', 'Deutsch').should('be.visible').and('have.attr', 'aria-pressed', 'false');

        cy.get('h1').should('be.visible').and('have.text', 'Znalazłeś gołębia pocztowego?');
        cy.contains('Jeżeli znalazłeś gołębia z obrączką, wypełnij poniższy formularz.').should('be.visible');

        cy.contains('h2', 'Formularz zgłoszenia').should('be.visible');

        cy.contains('label', 'Numer obrączki').should('be.visible');
        cy.get('#ring-number').should('be.visible')
            .and('have.attr', 'type', 'text')
            .and('have.attr', 'maxlength', '15')
            .and('have.attr', 'placeholder', 'np. PL-0369-24-1234');

        cy.contains('legend', 'Kontakt do Ciebie').should('be.visible');
        cy.contains('Podaj przynajmniej jeden sposób kontaktu - telefon albo adres e-mail.').should('be.visible');

        cy.contains('label', 'Telefon').should('be.visible');
        cy.get('#contact-phone').should('be.visible').and('have.attr', 'type', 'tel').and('have.attr', 'maxlength', '15');

        cy.contains('label', 'Adres e-mail').should('be.visible');
        cy.get('#contact-email').should('be.visible').and('have.attr', 'type', 'email').and('have.attr', 'maxlength', '320');

        cy.contains('label', 'Kraj odnalezienia').should('be.visible');
        cy.get('#found-country').should('be.visible').and('have.attr', 'maxlength', '100');

        cy.contains('label', 'Miejscowość lub okolica').should('be.visible');
        cy.get('#found-location').should('be.visible').and('have.attr', 'maxlength', '150');

        cy.contains('label', 'Opis okoliczności').should('be.visible');
        cy.get('#description').should('be.visible').and('have.attr', 'maxlength', '1000');
        cy.contains('0 z 1000 znaków').should('be.visible');

        cy.contains('h3', 'Co zrobimy z Twoimi danymi').should('be.visible');
        cy.contains('Widzi je tylko administrator oddziału.').should('be.visible');

        cy.contains('Pola oznaczone gwiazdką są wymagane.').should('be.visible');
        cy.contains('button', 'Wyślij zgłoszenie').should('be.visible').and('not.be.disabled');

        cy.contains('Panel zgłoszeń').should('not.exist');

        cy.checkFooter();
    });

    // ==========================================
    // LANGUAGE
    // ==========================================

    it('Should switch the report form between Polish, English and German', () => {
        cy.contains('button', 'English').click();

        cy.get('[role="group"][aria-label="Language"]').should('be.visible');
        cy.contains('button', 'English').should('have.attr', 'aria-pressed', 'true');
        cy.get('h1').should('have.text', 'Have you found a racing pigeon?');
        cy.contains('h2', 'Report form').should('be.visible');
        cy.contains('label', 'Ring number').should('be.visible');
        cy.contains('legend', 'Your contact details').should('be.visible');
        cy.contains('label', 'Phone number').should('be.visible');
        cy.contains('label', 'E-mail address').should('be.visible');
        cy.contains('label', 'Country where it was found').should('be.visible');
        cy.contains('label', 'Town or area').should('be.visible');
        cy.contains('label', 'Circumstances').should('be.visible');
        cy.contains('h3', 'What we do with your details').should('be.visible');
        cy.contains('button', 'Send report').should('be.visible');

        cy.contains('button', 'Deutsch').click();

        cy.get('[role="group"][aria-label="Sprache"]').should('be.visible');
        cy.contains('button', 'Deutsch').should('have.attr', 'aria-pressed', 'true');
        cy.get('h1').should('have.text', 'Haben Sie eine Brieftaube gefunden?');
        cy.contains('h2', 'Meldeformular').should('be.visible');
        cy.contains('label', 'Ringnummer').should('be.visible');
        cy.contains('legend', 'Ihre Kontaktdaten').should('be.visible');
        cy.contains('label', 'Telefonnummer').should('be.visible');
        cy.contains('label', 'E-Mail-Adresse').should('be.visible');
        cy.contains('button', 'Meldung senden').should('be.visible');

        cy.contains('button', 'Polski').click();

        cy.get('[role="group"][aria-label="Język"]').should('be.visible');
        cy.contains('button', 'Polski').should('have.attr', 'aria-pressed', 'true');
        cy.get('h1').should('have.text', 'Znalazłeś gołębia pocztowego?');
    });

    // ==========================================
    // ACCESS
    // ==========================================

    it('Should hide the admin reports link from an unauthenticated user', () => {
        cy.contains('Panel zgłoszeń').should('not.exist');
    });

    it('Should hide the admin reports link from a breeder', () => {
        cy.mockNavbarNotifications();
        cy.visitWithToken('/found-pigeons', breederToken);

        cy.wait(['@getNotifications', '@getUnreadCount']);

        cy.checkLoggedInNavbar('Hodowca');
        cy.contains('Panel zgłoszeń').should('not.exist');
    });

    it('Should display the admin reports link for an administrator', () => {
        cy.mockNavbarNotifications();
        cy.visitWithToken('/found-pigeons', adminToken);

        cy.wait(['@getNotifications', '@getUnreadCount']);

        cy.checkLoggedInNavbar('Admin');
        cy.contains('a', 'Panel zgłoszeń').should('be.visible').and('have.attr', 'href', '/found-pigeons/admin');
    });

    // ==========================================
    // RING NUMBER VALIDATION
    // ==========================================

    it('Should require a ring number', () => {
        cy.get('#contact-phone').type('601234567');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.contains('Podaj numer obrączki gołębia.').should('be.visible');
        cy.get('#ring-number').should('have.attr', 'aria-invalid', 'true');
    });

    it('Should reject an invalid ring number format', () => {
        cy.get('#ring-number').type('PL-1234');
        cy.get('#contact-phone').type('601234567');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.contains('Numer obrączki musi mieć format PL-0369-RR-NNNN, np. PL-0369-24-1234.').should('be.visible');
        cy.get('#ring-number').should('have.attr', 'aria-invalid', 'true');
    });

    it('Should automatically convert the ring number to uppercase', () => {
        cy.get('#ring-number').type('pl-0369-26-1234').should('have.value', 'PL-0369-26-1234');
    });

    // ==========================================
    // CONTACT VALIDATION
    // ==========================================

    it('Should require at least one contact method', () => {
        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.contains('Podaj numer telefonu lub adres e-mail.').should('be.visible');
        cy.get('#contact-phone').should('have.attr', 'aria-invalid', 'true');
        cy.get('#contact-email').should('have.attr', 'aria-invalid', 'true');
    });

    it('Should reject an invalid email address', () => {
        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-email').type('invalid-email');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.contains('Podaj poprawny adres e-mail.').should('be.visible');
        cy.get('#contact-email').should('have.attr', 'aria-invalid', 'true');
    });

    it('Should reject an invalid phone number', () => {
        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-phone').type('12345');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.contains('Podaj poprawny numer telefonu (od 6 do 15 cyfr).').should('be.visible');
        cy.get('#contact-phone').should('have.attr', 'aria-invalid', 'true');
    });

    it('Should update the description character counter', () => {
        cy.get('#description').type('Testowy opis znalezionego gołębia.');
        cy.contains('34 z 1000 znaków').should('be.visible');
    });

    // ==========================================
    // SUCCESSFUL SUBMISSION
    // ==========================================

    it('Should successfully submit a complete report and allow reporting another pigeon', () => {
        cy.intercept('POST', '**/api/found-pigeons', (req) => {
            expect(req.body).to.deep.equal({
                ringNumber: 'PL-0369-26-1234',
                contactPhone: '+49 30 12345678',
                contactEmail: 'finder@example.de',
                foundCountry: 'Niemcy',
                foundLocation: 'Cottbus',
                description: 'Gołąb siedzi na parapecie od wczoraj.',
                preferredLanguage: 'PL'
            });

            req.reply({
                statusCode: 201,
                body: {id: 10}
            });
        }).as('submitReport');

        cy.get('#ring-number').type('pl-0369-26-1234');
        cy.get('#contact-phone').type('+49 30 12345678');
        cy.get('#contact-email').type('finder@example.de');
        cy.get('#found-country').type('Niemcy');
        cy.get('#found-location').type('Cottbus');
        cy.get('#description').type('Gołąb siedzi na parapecie od wczoraj.');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.wait('@submitReport');

        cy.contains('h2', 'Dziękujemy za zgłoszenie').should('be.visible');
        cy.contains('Zgłoszenie trafiło do administratora oddziału.').should('be.visible');
        cy.contains('button', 'Zgłoś kolejnego gołębia').should('be.visible');

        cy.contains('button', 'Zgłoś kolejnego gołębia').click();

        cy.contains('h2', 'Formularz zgłoszenia').should('be.visible');
        cy.get('#ring-number').should('have.value', '');
        cy.get('#contact-phone').should('have.value', '');
        cy.get('#contact-email').should('have.value', '');
        cy.get('#found-country').should('have.value', '');
        cy.get('#found-location').should('have.value', '');
        cy.get('#description').should('have.value', '');
        cy.contains('0 z 1000 znaków').should('be.visible');
    });

    it('Should successfully submit a report using only an email address', () => {
        cy.intercept('POST', '**/api/found-pigeons', (req) => {
            expect(req.body).to.deep.equal({
                ringNumber: 'PL-0369-26-1234',
                contactPhone: null,
                contactEmail: 'finder@example.com',
                foundCountry: null,
                foundLocation: null,
                description: null,
                preferredLanguage: 'PL'
            });

            req.reply({
                statusCode: 201,
                body: {id: 11}
            });
        }).as('submitEmailReport');

        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-email').type('finder@example.com');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.wait('@submitEmailReport');
        cy.contains('h2', 'Dziękujemy za zgłoszenie').should('be.visible');
    });

    it('Should successfully submit a report using only a phone number', () => {
        cy.intercept('POST', '**/api/found-pigeons', (req) => {
            expect(req.body).to.deep.equal({
                ringNumber: 'PL-0369-26-1234',
                contactPhone: '601234567',
                contactEmail: null,
                foundCountry: null,
                foundLocation: null,
                description: null,
                preferredLanguage: 'PL'
            });

            req.reply({
                statusCode: 201,
                body: {id: 12}
            });
        }).as('submitPhoneReport');

        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-phone').type('601234567');
        cy.contains('button', 'Wyślij zgłoszenie').click();

        cy.wait('@submitPhoneReport');
        cy.contains('h2', 'Dziękujemy za zgłoszenie').should('be.visible');
    });

    it('Should send the selected language with the report', () => {
        cy.intercept('POST', '**/api/found-pigeons', (req) => {
            expect(req.body.preferredLanguage).to.eq('DE');

            req.reply({
                statusCode: 201,
                body: {id: 13}
            });
        }).as('submitGermanReport');

        cy.contains('button', 'Deutsch').click();
        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-email').type('finder@example.de');
        cy.contains('button', 'Meldung senden').click();

        cy.wait('@submitGermanReport');

        cy.contains('h2', 'Vielen Dank für Ihre Meldung').should('be.visible');
        cy.contains('button', 'Weitere Taube melden').should('be.visible');
    });

    // ==========================================
    // SUBMISSION ERRORS
    // ==========================================

    it('Should display a localized error when report submission fails', () => {
        cy.intercept('POST', '**/api/found-pigeons', {
            statusCode: 500,
            body: 'Wystąpił nieoczekiwany błąd serwera.'
        }).as('submitReportError');

        cy.contains('button', 'English').click();
        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-email').type('finder@example.com');
        cy.contains('button', 'Send report').click();

        cy.wait('@submitReportError');

        cy.get('[role="alert"]').should('be.visible')
            .and('contain.text', 'The report could not be sent. Please try again in a moment.');

        cy.contains('h2', 'Report form').should('be.visible');
    });

    it('Should display a localized rate limit error after receiving HTTP 429', () => {
        cy.intercept('POST', '**/api/found-pigeons', {
            statusCode: 429,
            body: 'Wysłano zbyt wiele zgłoszeń.'
        }).as('submitReportRateLimit');

        cy.contains('button', 'Deutsch').click();
        cy.get('#ring-number').type('PL-0369-26-1234');
        cy.get('#contact-email').type('finder@example.de');
        cy.contains('button', 'Meldung senden').click();

        cy.wait('@submitReportRateLimit');

        cy.get('[role="alert"]').should('be.visible')
            .and('contain.text', 'Es wurden zu viele Meldungen gesendet. Bitte versuchen Sie es später erneut.');
    });
});