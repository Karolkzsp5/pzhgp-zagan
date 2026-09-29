import { createFakeToken } from '../../support/testUtils';

interface MockFlightSummary {
    id: number;
    name: string;
    ringNumber: string | null;
    releaseSite: string | null;
    ownerName: string;
    startTime: string | null;
    endTime: string | null;
    straightLineDistanceKm: number;
    durationSeconds: number;
    averageSpeedMetersPerMinute: number;
    totalPoints: number;
    timestampsAvailable: boolean;
    uploadedAt: string;
    canDelete: boolean;
}

interface MockFlightsPage {
    content: MockFlightSummary[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

describe('Flights Page Tests', () => {
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const createFlightsPage = (): MockFlightsPage => ({
        content: [
            {
                id: 1,
                name: 'Lot konkursowy Dessau',
                ringNumber: 'PL-0369-26-1234',
                releaseSite: 'Dessau',
                ownerName: 'Jan Kowalski',
                startTime: '2026-09-20T08:00:00Z',
                endTime: '2026-09-20T09:30:00Z',
                straightLineDistanceKm: 125.43,
                durationSeconds: 5400,
                averageSpeedMetersPerMinute: 1378,
                totalPoints: 1200,
                timestampsAvailable: true,
                uploadedAt: '2026-09-20T12:00:00Z',
                canDelete: true
            },
            {
                id: 2,
                name: 'Lot treningowy',
                ringNumber: null,
                releaseSite: null,
                ownerName: 'Jan Kowalski',
                startTime: null,
                endTime: null,
                straightLineDistanceKm: 20.5,
                durationSeconds: 0,
                averageSpeedMetersPerMinute: 0,
                totalPoints: 350,
                timestampsAvailable: false,
                uploadedAt: '2026-09-19T15:00:00Z',
                canDelete: true
            }
        ],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 10
    });

    const createEmptyFlightsPage = (): MockFlightsPage => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 10
    });

    const getFlightRow = (name: string) => {
        return cy.contains('a', name).closest('tr');
    };

    describe('Authenticated User', () => {
        let flightsPage: MockFlightsPage;

        beforeEach(() => {
            flightsPage = createFlightsPage();

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: flightsPage
                });
            }).as('getFlights');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/flights', breederToken);

            cy.wait(['@getFlights', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render all visible elements of the flights page', () => {
            cy.checkLoggedInNavbar('Hodowca');

            cy.get('h1').should('be.visible').and('have.text', 'Mapy lotów');
            cy.contains('Wgraj plik').should('be.visible');
            cy.contains('.gpx').should('be.visible');
            cy.contains('aby zobaczyć trasę gołębia na mapie oraz statystyki lotu.').should('be.visible');

            cy.contains('h2', 'Wgraj nowy lot').should('be.visible');

            cy.get('input[type="file"][accept=".gpx"]').should('exist');
            cy.contains('Przeciągnij plik .gpx lub kliknij, aby wybrać').should('be.visible');
            cy.contains('Maksymalny rozmiar pliku: 10 MB').should('be.visible');

            cy.contains('label', 'Nazwa lotu').should('be.visible');
            cy.get('#flight-name').should('be.visible')
                .and('have.attr', 'maxlength', '150')
                .and('have.attr', 'placeholder', 'np. Lot konkursowy Dessau');

            cy.contains('label', 'Numer obrączki').should('be.visible');
            cy.get('#ring-number').should('be.visible')
                .and('have.attr', 'maxlength', '32')
                .and('have.attr', 'placeholder', 'odczytany z pliku');

            cy.contains('label', 'Miejsce wypuszczenia').should('be.visible');
            cy.get('#release-site').should('be.visible')
                .and('have.attr', 'maxlength', '150')
                .and('have.attr', 'placeholder', 'np. Dessau');

            cy.contains('button', 'Wgraj i przeanalizuj').should('be.visible').and('be.disabled');

            cy.contains('h2', 'Moje loty').should('be.visible');

            cy.contains('th', 'Lot').should('be.visible');
            cy.contains('th', 'Obrączka').should('be.visible');
            cy.contains('th', 'Dystans').should('be.visible');
            cy.contains('th', 'Czas nagrania').should('be.visible');
            cy.contains('th', 'Prędkość').should('be.visible');
            cy.contains('th', 'Wgrano').should('be.visible');

            getFlightRow('Lot konkursowy Dessau').should('be.visible').within(() => {
                cy.contains('a', 'Lot konkursowy Dessau').should('have.attr', 'href', '/flights/1');
                cy.contains('Wypuszczenie: Dessau').should('be.visible');
                cy.contains('PL-0369-26-1234').should('be.visible');
                cy.contains('125.43 km').should('be.visible');
                cy.contains('1 godz. 30 min').should('be.visible');
                cy.get('td').eq(4).should('contain.text', 'm/min');
                cy.get('button[aria-label="Usuń lot Lot konkursowy Dessau"]').should('be.visible');
            });

            getFlightRow('Lot treningowy').should('be.visible').within(() => {
                cy.contains('a', 'Lot treningowy').should('have.attr', 'href', '/flights/2');
                cy.contains('20.50 km').should('be.visible');
                cy.get('span[title="Plik nie zawiera znaczników czasu"]').should('be.visible').and('have.text', '—');
                cy.get('button[aria-label="Usuń lot Lot treningowy"]').should('be.visible');
            });

            cy.contains('button', 'Poprzednia').should('not.exist');
            cy.contains('button', 'Następna').should('not.exist');

            cy.checkFooter();
        });

        // ==========================================
        // FILE SELECTION
        // ==========================================

        it('Should accept a valid GPX file selected from the file input', () => {
            cy.get('input[type="file"]').selectFile({
                contents: Cypress.Buffer.alloc(2048),
                fileName: 'test-flight.gpx',
                mimeType: 'application/gpx+xml'
            }, { force: true });

            cy.contains('test-flight.gpx').should('be.visible');
            cy.contains('2 kB — kliknij, aby wybrać inny plik').should('be.visible');
            cy.contains('button', 'Wgraj i przeanalizuj').should('not.be.disabled');
            cy.get('[role="alert"]').should('not.exist');
        });

        it('Should accept a valid GPX file using drag and drop', () => {
            cy.get('input[type="file"]').parent('label').selectFile({
                contents: Cypress.Buffer.alloc(3072),
                fileName: 'dragged-flight.GPX',
                mimeType: 'application/gpx+xml'
            }, { action: 'drag-drop' });

            cy.contains('dragged-flight.GPX').should('be.visible');
            cy.contains('3 kB — kliknij, aby wybrać inny plik').should('be.visible');
            cy.contains('button', 'Wgraj i przeanalizuj').should('not.be.disabled');
        });

        it('Should reject a file with an invalid extension', () => {
            cy.get('input[type="file"]').selectFile({
                contents: Cypress.Buffer.from('invalid file'),
                fileName: 'flight.txt',
                mimeType: 'text/plain'
            }, { force: true });

            cy.get('[role="alert"]').should('be.visible')
                .and('contain.text', 'Wybierz plik z rozszerzeniem .gpx (eksport z programu obsługującego obrączki).');

            cy.contains('flight.txt').should('not.exist');
            cy.contains('button', 'Wgraj i przeanalizuj').should('be.disabled');
        });

        it('Should reject a GPX file larger than 10 MB', () => {
            cy.get('input[type="file"]').selectFile({
                contents: Cypress.Buffer.alloc(10 * 1024 * 1024 + 1),
                fileName: 'large-flight.gpx',
                mimeType: 'application/gpx+xml'
            }, { force: true });

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Plik GPX może mieć maksymalnie 10 MB.');
            cy.contains('large-flight.gpx').should('not.exist');
            cy.contains('button', 'Wgraj i przeanalizuj').should('be.disabled');
        });

        // ==========================================
        // FLIGHT UPLOAD
        // ==========================================

        it('Should successfully upload a GPX file and navigate to the flight details page', () => {
            cy.intercept('POST', '**/api/flights', (req) => {
                expect(req.headers['content-type']).to.include('multipart/form-data');
                expect(req.body).to.exist;

                req.reply({
                    statusCode: 201,
                    body: { id: 99 }
                });
            }).as('uploadFlight');

            cy.intercept('GET', '**/api/flights/99', {
                statusCode: 500,
                body: 'Testowe zatrzymanie po przekierowaniu.'
            }).as('getUploadedFlight');

            cy.get('input[type="file"]').selectFile({
                contents: Cypress.Buffer.from('<gpx version="1.1"></gpx>'),
                fileName: 'dessau.gpx',
                mimeType: 'application/gpx+xml'
            }, { force: true });

            cy.get('#flight-name').type('Lot konkursowy Dessau');
            cy.get('#ring-number').type('PL-0369-26-1234');
            cy.get('#release-site').type('Dessau');
            cy.contains('button', 'Wgraj i przeanalizuj').click();

            cy.wait('@uploadFlight');

            cy.location('pathname').should('eq', '/flights/99');
        });

        it('Should display an API error when uploading a GPX file fails', () => {
            cy.intercept('POST', '**/api/flights', {
                statusCode: 400,
                body: 'Nieprawidłowy format pliku GPX.'
            }).as('uploadFlightError');

            cy.get('input[type="file"]').selectFile({
                contents: Cypress.Buffer.from('<gpx></gpx>'),
                fileName: 'invalid.gpx',
                mimeType: 'application/gpx+xml'
            }, { force: true });

            cy.contains('button', 'Wgraj i przeanalizuj').click();

            cy.wait('@uploadFlightError');

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nieprawidłowy format pliku GPX.');
            cy.contains('invalid.gpx').should('be.visible');
            cy.contains('button', 'Wgraj i przeanalizuj').should('not.be.disabled');
        });

        // ==========================================
        // EMPTY STATE
        // ==========================================

        it('Should display an empty state when the user has no saved flights', () => {
            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: createEmptyFlightsPage()
            }).as('getEmptyFlights');

            cy.reload();
            cy.wait('@getEmptyFlights');

            cy.contains('Nie masz jeszcze zapisanych lotów. Wgraj pierwszy plik GPX, aby zobaczyć trasę na mapie.').should('be.visible');
            cy.get('table').should('not.exist');
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading flights', () => {
            let shouldFail = true;

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać testowej listy lotów.'
                    });
                } else {
                    req.reply({
                        statusCode: 200,
                        body: flightsPage
                    });
                }
            }).as('getFlightsRetry');

            cy.reload();
            cy.wait('@getFlightsRetry');

            cy.contains('Nie udało się pobrać testowej listy lotów.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait('@getFlightsRetry');

            cy.contains('Nie udało się pobrać testowej listy lotów.').should('not.exist');
            cy.contains('a', 'Lot konkursowy Dessau').should('be.visible');
            cy.contains('a', 'Lot treningowy').should('be.visible');
        });

        // ==========================================
        // PAGINATION
        // ==========================================

        it('Should navigate between flight pages', () => {
            const firstPage: MockFlightsPage = {
                ...createFlightsPage(),
                totalElements: 3,
                totalPages: 2,
                number: 0
            };

            const secondPage: MockFlightsPage = {
                content: [
                    {
                        id: 3,
                        name: 'Lot konkursowy Berlin',
                        ringNumber: 'PL-0369-26-9999',
                        releaseSite: 'Berlin',
                        ownerName: 'Jan Kowalski',
                        startTime: '2026-09-18T08:00:00Z',
                        endTime: '2026-09-18T09:00:00Z',
                        straightLineDistanceKm: 160.25,
                        durationSeconds: 3600,
                        averageSpeedMetersPerMinute: 1500,
                        totalPoints: 900,
                        timestampsAvailable: true,
                        uploadedAt: '2026-09-18T10:00:00Z',
                        canDelete: true
                    }
                ],
                totalElements: 3,
                totalPages: 2,
                number: 1,
                size: 10
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getFirstFlightsPage');

            cy.reload();
            cy.wait('@getFirstFlightsPage');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('button', 'Poprzednia').should('be.disabled');
            cy.contains('button', 'Następna').should('not.be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '1',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: secondPage
            }).as('getSecondFlightsPage');

            cy.contains('button', 'Następna').click();
            cy.wait('@getSecondFlightsPage');

            cy.contains('Strona 2 z 2').should('be.visible');
            cy.contains('a', 'Lot konkursowy Berlin').should('be.visible');
            cy.contains('a', 'Lot konkursowy Dessau').should('not.exist');
            cy.contains('button', 'Poprzednia').should('not.be.disabled');
            cy.contains('button', 'Następna').should('be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getFirstFlightsPageAgain');

            cy.contains('button', 'Poprzednia').click();
            cy.wait('@getFirstFlightsPageAgain');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('a', 'Lot konkursowy Dessau').should('be.visible');
        });

        // ==========================================
        // DELETE FLIGHT
        // ==========================================

        it('Should cancel flight deletion without removing the flight', () => {
            cy.get('button[aria-label="Usuń lot Lot konkursowy Dessau"]').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń lot').should('be.visible');
                cy.contains('Czy na pewno chcesz usunąć lot "Lot konkursowy Dessau" wraz z zapisaną trasą?').should('be.visible');
                cy.contains('Tej operacji nie można cofnąć.').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('a', 'Lot konkursowy Dessau').should('be.visible');
        });

        it('Should successfully delete a flight', () => {
            cy.intercept('DELETE', '**/api/flights/1', (req) => {
                flightsPage.content = flightsPage.content.filter(flight => flight.id !== 1);
                flightsPage.totalElements = flightsPage.content.length;

                req.reply({
                    statusCode: 204
                });
            }).as('deleteFlight');

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: flightsPage
                });
            }).as('getFlightsAfterDelete');

            cy.get('button[aria-label="Usuń lot Lot konkursowy Dessau"]').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteFlight');
            cy.wait('@getFlightsAfterDelete');

            cy.contains('a', 'Lot konkursowy Dessau').should('not.exist');
            cy.contains('a', 'Lot treningowy').should('be.visible');
        });

        it('Should display an error modal when flight deletion fails', () => {
            cy.intercept('DELETE', '**/api/flights/1', {
                statusCode: 500,
                body: 'Nie udało się usunąć testowego lotu.'
            }).as('deleteFlightError');

            cy.get('button[aria-label="Usuń lot Lot konkursowy Dessau"]').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteFlightError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd').should('be.visible');
                cy.contains('Nie udało się usunąć testowego lotu.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('a', 'Lot konkursowy Dessau').should('be.visible');
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 401,
                body: ''
            });

            cy.visit('/flights');

            cy.location('pathname').should('eq', '/login');
        });
    });
});