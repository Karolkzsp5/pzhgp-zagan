describe('Znalezione gołębie - Testy E2E', () => {

    const API = 'http://localhost:8080';

    // Token z rolą ADMINISTRATOR, zbudowany tak jak w pozostałych testach panelu.
    const adminToken = 'header.eyJzdWIiOiJhZG1pbkBwemhncC5wbCIsInJvbGUiOiJBRE1JTklTVFJBVE9SIiwiZXhwIjo5OTk5OTk5OTk5fQ.signature';

    const mockReports = {
        content: [
            {
                id: 1,
                ringNumber: 'PL-0208-24-1234',
                contactPhone: '+49 30 12345678',
                contactEmail: 'finder@example.de',
                foundLocation: 'Cottbus',
                foundCountry: 'Niemcy',
                description: 'Gołąb siedzi na parapecie od wczoraj.',
                preferredLanguage: 'DE',
                status: 'PENDING',
                adminNote: null,
                createdAt: '2026-09-20T10:00:00',
                updatedAt: null
            },
            {
                id: 2,
                ringNumber: 'PL-0208-24-5678',
                contactPhone: null,
                contactEmail: 'znalazca@example.pl',
                foundLocation: 'Żagań',
                foundCountry: 'Polska',
                description: null,
                preferredLanguage: 'PL',
                status: 'APPROVED',
                adminNote: 'Ustalono sekcję.',
                createdAt: '2026-09-19T08:30:00',
                updatedAt: '2026-09-19T09:00:00'
            }
        ],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 10
    };

    describe('Publiczny formularz zgłoszenia', () => {

        beforeEach(() => {
            cy.visit('/found-pigeons');
        });

        it('jest dostępny bez logowania', () => {
            cy.contains('h1', 'Znalazłeś gołębia pocztowego?').should('be.visible');
            cy.get('#ring-number').should('be.visible');
            cy.get('#contact-phone').should('be.visible');
            cy.get('#contact-email').should('be.visible');
        });

        it('domyślnie wyświetla się po polsku', () => {
            cy.get('button[aria-pressed="true"]').should('contain.text', 'Polski');
            cy.contains('Numer obrączki').should('be.visible');
        });

        it('przełącza język na angielski i niemiecki', () => {
            cy.contains('button', 'English').click();
            cy.contains('h1', 'Have you found a racing pigeon?').should('be.visible');
            cy.contains('label', 'Ring number').should('be.visible');
            cy.contains('button', 'Send report').should('be.visible');

            cy.contains('button', 'Deutsch').click();
            cy.contains('h1', 'Haben Sie eine Brieftaube gefunden?').should('be.visible');
            cy.contains('label', 'Ringnummer').should('be.visible');
            cy.contains('button', 'Meldung senden').should('be.visible');

            cy.contains('button', 'Polski').click();
            cy.contains('h1', 'Znalazłeś gołębia pocztowego?').should('be.visible');
        });

        it('pokazuje informację o wykorzystaniu danych kontaktowych', () => {
            cy.contains('Co zrobimy z Twoimi danymi').should('be.visible');
            cy.contains('Widzi je tylko administrator oddziału').should('be.visible');
        });

        it('wymaga numeru obrączki', () => {
            cy.get('#contact-phone').type('601234567');
            cy.contains('button', 'Wyślij zgłoszenie').click();

            cy.contains('Podaj numer obrączki gołębia.').should('be.visible');
        });

        it('wymaga przynajmniej jednej metody kontaktu', () => {
            cy.get('#ring-number').type('PL-0208-24-1234');
            cy.contains('button', 'Wyślij zgłoszenie').click();

            cy.contains('Podaj numer telefonu lub adres e-mail.').should('be.visible');
        });

        it('odrzuca niepoprawny adres e-mail', () => {
            cy.get('#ring-number').type('PL-0208-24-1234');
            cy.get('#contact-email').type('to-nie-jest-email');
            cy.contains('button', 'Wyślij zgłoszenie').click();

            cy.contains('Podaj poprawny adres e-mail.').should('be.visible');
        });

        it('odrzuca numer telefonu o zbyt małej liczbie cyfr', () => {
            cy.get('#ring-number').type('PL-0208-24-1234');
            cy.get('#contact-phone').type('12345');
            cy.contains('button', 'Wyślij zgłoszenie').click();

            cy.contains('Podaj poprawny numer telefonu').should('be.visible');
        });

        it('przyjmuje zagraniczny numer telefonu i potwierdza wysłanie', () => {
            cy.intercept('POST', `${API}/api/found-pigeons`, {
                statusCode: 201,
                body: { id: 10 }
            }).as('submitReport');

            cy.get('#ring-number').type('PL-0208-24-1234');
            cy.get('#contact-phone').type('+49 30 12345678');
            cy.get('#found-country').type('Niemcy');
            cy.contains('button', 'Wyślij zgłoszenie').click();

            cy.wait('@submitReport').its('request.body').should(body => {
                expect(body.ringNumber).to.equal('PL-0208-24-1234');
                expect(body.contactPhone).to.equal('+49 30 12345678');
                expect(body.preferredLanguage).to.equal('PL');
            });

            cy.contains('Dziękujemy za zgłoszenie').should('be.visible');
        });

        it('zapisuje wybrany język razem ze zgłoszeniem', () => {
            cy.intercept('POST', `${API}/api/found-pigeons`, {
                statusCode: 201,
                body: { id: 11 }
            }).as('submitReport');

            cy.contains('button', 'Deutsch').click();
            cy.get('#ring-number').type('PL-0208-24-1234');
            cy.get('#contact-email').type('hans@example.de');
            cy.contains('button', 'Meldung senden').click();

            cy.wait('@submitReport').its('request.body.preferredLanguage').should('equal', 'DE');
            cy.contains('Vielen Dank für Ihre Meldung').should('be.visible');
        });

        it('pokazuje komunikat błędu w języku formularza', () => {
            cy.intercept('POST', `${API}/api/found-pigeons`, {
                statusCode: 500,
                body: 'Wystąpił nieoczekiwany błąd serwera.'
            }).as('submitReport');

            cy.contains('button', 'English').click();
            cy.get('#ring-number').type('PL-0208-24-1234');
            cy.get('#contact-email').type('finder@example.com');
            cy.contains('button', 'Send report').click();

            cy.wait('@submitReport');
            cy.contains('The report could not be sent.').should('be.visible');
        });

        it('nie pokazuje odnośnika do panelu, gdy użytkownik nie jest administratorem', () => {
            cy.contains('Panel zgłoszeń').should('not.exist');
        });
    });

    describe('Panel administratora', () => {

        beforeEach(() => {
            cy.intercept('GET', `${API}/api/admin/found-pigeons*`, {
                statusCode: 200,
                body: mockReports
            }).as('getReports');

            cy.visit('/found-pigeons/admin', {
                onBeforeLoad: (win) => {
                    win.localStorage.setItem('jwt_token', adminToken);
                }
            });

            cy.wait('@getReports');
        });

        it('wyświetla listę zgłoszeń ze statusami', () => {
            cy.contains('h1', 'Zgłoszenia znalezionych gołębi').should('be.visible');
            cy.contains('td', 'PL-0208-24-1234').should('be.visible');
            cy.contains('Oczekujące').should('be.visible');
            cy.contains('W obsłudze').should('be.visible');
        });

        it('po wybraniu zgłoszenia pokazuje dane kontaktowe i język znalazcy', () => {
            cy.contains('td', 'PL-0208-24-1234').click();

            cy.contains('+49 30 12345678').should('be.visible');
            cy.contains('finder@example.de').should('be.visible');
            cy.contains('niemiecki').should('be.visible');
            cy.contains('Cottbus, Niemcy').should('be.visible');
        });

        it('filtruje zgłoszenia według statusu', () => {
            cy.contains('button', 'Oczekujące').click();

            cy.wait('@getReports').its('request.url').should('include', 'status=PENDING');
        });

        it('wyszukuje zgłoszenia po numerze obrączki', () => {
            cy.get('#ring-search').type('PL-0208');
            cy.contains('button', 'Szukaj').click();

            cy.wait('@getReports').its('request.url').should('include', 'ringNumber=PL-0208');
        });

        it('pokazuje tylko dozwolone zmiany statusu', () => {
            cy.contains('td', 'PL-0208-24-1234').click();

            // PENDING: można zatwierdzić albo odrzucić, ale nie zakończyć od razu.
            cy.contains('button', 'Zatwierdź').should('be.visible');
            cy.contains('button', 'Odrzuć').should('be.visible');
            cy.contains('button', 'Oznacz jako zakończone').should('not.exist');
        });

        it('zatwierdza zgłoszenie', () => {
            cy.intercept('PATCH', `${API}/api/admin/found-pigeons/1/status`, {
                statusCode: 200,
                body: { ...mockReports.content[0], status: 'APPROVED' }
            }).as('updateStatus');

            cy.contains('td', 'PL-0208-24-1234').click();
            cy.contains('button', 'Zatwierdź').click();

            cy.wait('@updateStatus').its('request.body.status').should('equal', 'APPROVED');
        });

        it('zapisuje notatkę administratora', () => {
            cy.intercept('PATCH', `${API}/api/admin/found-pigeons/1/note`, {
                statusCode: 200,
                body: { ...mockReports.content[0], adminNote: 'Właściciel ustalony.' }
            }).as('updateNote');

            cy.contains('td', 'PL-0208-24-1234').click();
            cy.get('#admin-note').type('Właściciel ustalony.');
            cy.contains('button', 'Zapisz notatkę').click();

            cy.wait('@updateNote').its('request.body.adminNote').should('equal', 'Właściciel ustalony.');
        });

        it('nie pozwala zmienić statusu zamkniętej sprawy', () => {
            cy.intercept('GET', `${API}/api/admin/found-pigeons*`, {
                statusCode: 200,
                body: {
                    ...mockReports,
                    content: [{ ...mockReports.content[0], status: 'RESOLVED' }],
                    totalElements: 1
                }
            }).as('getResolved');

            cy.reload();
            cy.wait('@getResolved');

            cy.contains('td', 'PL-0208-24-1234').click();
            cy.contains('Sprawa jest zamknięta').should('be.visible');
        });

        it('prosi o potwierdzenie przed usunięciem zgłoszenia', () => {
            cy.contains('td', 'PL-0208-24-1234').click();
            cy.contains('button', 'Usuń zgłoszenie').click();

            cy.contains('Czy na pewno chcesz trwale usunąć zgłoszenie').should('be.visible');
            cy.contains('button', 'Anuluj').click();
            cy.contains('Czy na pewno chcesz trwale usunąć zgłoszenie').should('not.exist');
        });
    });
});
