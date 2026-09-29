import { createFakeToken } from '../../support/testUtils';

interface MockFlightTrackPoint {
    latitude: number;
    longitude: number;
    elevation: number | null;
    time: string | null;
    speedMetersPerMinute: number | null;
}

interface MockFlightStatistics {
    straightLineDistanceKm: number;
    trackDistanceKm: number;
    durationSeconds: number;
    averageSpeedMetersPerMinute: number;
    straightLineSpeedMetersPerMinute: number;
    maxSpeedMetersPerMinute: number;
    minElevationMeters: number | null;
    maxElevationMeters: number | null;
    elevationGainMeters: number | null;
    totalPoints: number;
    timestampsAvailable: boolean;
}

interface MockFlightDetails {
    id: number;
    name: string;
    ringNumber: string | null;
    releaseSite: string | null;
    ownerName: string;
    originalFileName: string;
    startTime: string | null;
    endTime: string | null;
    startLatitude: number;
    startLongitude: number;
    endLatitude: number;
    endLongitude: number;
    statistics: MockFlightStatistics;
    trackPoints: MockFlightTrackPoint[];
    returnedPoints: number;
    uploadedAt: string;
    canDelete: boolean;
}

describe('Flight Details Page Tests', () => {
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const createFlightDetails = (): MockFlightDetails => ({
        id: 1,
        name: 'Lot konkursowy Dessau',
        ringNumber: 'PL-0369-26-1234',
        releaseSite: 'Dessau',
        ownerName: 'Jan Kowalski',
        originalFileName: 'dessau-2026.gpx',
        startTime: '2026-09-20T08:00:00Z',
        endTime: '2026-09-20T08:04:00Z',
        startLatitude: 51.6200,
        startLongitude: 15.3100,
        endLatitude: 51.6600,
        endLongitude: 15.3900,
        statistics: {
            straightLineDistanceKm: 5.25,
            trackDistanceKm: 5.8,
            durationSeconds: 240,
            averageSpeedMetersPerMinute: 1100,
            straightLineSpeedMetersPerMinute: 1050,
            maxSpeedMetersPerMinute: 1300,
            minElevationMeters: 100,
            maxElevationMeters: 140,
            elevationGainMeters: 40,
            totalPoints: 120,
            timestampsAvailable: true
        },
        trackPoints: [
            {
                latitude: 51.6200,
                longitude: 15.3100,
                elevation: 100,
                time: '2026-09-20T08:00:00Z',
                speedMetersPerMinute: 900
            },
            {
                latitude: 51.6300,
                longitude: 15.3300,
                elevation: 110,
                time: '2026-09-20T08:01:00Z',
                speedMetersPerMinute: 1000
            },
            {
                latitude: 51.6400,
                longitude: 15.3500,
                elevation: 120,
                time: '2026-09-20T08:02:00Z',
                speedMetersPerMinute: 1100
            },
            {
                latitude: 51.6500,
                longitude: 15.3700,
                elevation: 130,
                time: '2026-09-20T08:03:00Z',
                speedMetersPerMinute: 1200
            },
            {
                latitude: 51.6600,
                longitude: 15.3900,
                elevation: 140,
                time: '2026-09-20T08:04:00Z',
                speedMetersPerMinute: 1300
            }
        ],
        returnedPoints: 5,
        uploadedAt: '2026-09-20T10:00:00Z',
        canDelete: true
    });

    const expectedTime = (value: string | null) => {
        if (!value) return '—';

        return new Date(value).toLocaleTimeString('pl-PL', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    };

    describe('Authenticated User', () => {
        let flight: MockFlightDetails;

        beforeEach(() => {
            flight = createFlightDetails();

            cy.intercept('GET', '**/api/flights/1', (req) => {
                req.reply({
                    statusCode: 200,
                    body: flight
                });
            }).as('getFlight');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/flights/1', breederToken);

            cy.wait(['@getFlight', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render all visible elements of the flight details page', () => {
            cy.checkLoggedInNavbar('Hodowca');

            cy.contains('a', 'Wszystkie loty').should('be.visible').and('have.attr', 'href', '/flights');

            cy.get('h1').should('be.visible').and('have.text', 'Lot konkursowy Dessau');
            cy.contains('Obrączka').should('be.visible');
            cy.contains('strong', 'PL-0369-26-1234').should('be.visible');
            cy.contains('Hodowca: Jan Kowalski').should('be.visible');
            cy.contains('Wypuszczenie: Dessau').should('be.visible');
            cy.contains('button', 'Usuń lot').should('be.visible');

            cy.contains('p', /^Dystans$/).parent().should('contain.text', '5.25').and('contain.text', 'km');
            cy.contains('p', /^Czas nagrania$/).parent().should('contain.text', '4 min 0 s');
            cy.contains('p', /^Prędkość średnia$/).parent().should('contain.text', 'm/min');
            cy.contains('p', /^Prędkość maksymalna$/).parent().should('contain.text', 'm/min');

            cy.get('.leaflet-container').should('be.visible');
            cy.get('.leaflet-marker-icon').should('have.length', 2);

            cy.contains('Prędkość na trasie [m/min]').should('be.visible');
            cy.contains('< 900').should('be.visible');
            cy.contains('900–1000').should('be.visible');
            cy.contains('1000–1100').should('be.visible');
            cy.contains('1100–1200').should('be.visible');
            cy.contains('> 1200').should('be.visible');

            cy.contains('label', 'Linia prosta start-meta').find('input[type="checkbox"]').should('be.checked');

            cy.get('button[aria-label="Odtwórz trasę"]').should('be.visible');
            cy.get('input[aria-label="Pozycja na trasie lotu"]').should('be.visible')
                .and('have.attr', 'min', '0')
                .and('have.attr', 'max', '4')
                .and('have.value', '0');

            cy.get('select[aria-label="Prędkość odtwarzania"]').should('be.visible').and('have.value', '25');
            cy.get('select[aria-label="Prędkość odtwarzania"] option').should('have.length', 3);
            cy.contains('option', 'wolno').should('exist');
            cy.contains('option', 'normalnie').should('exist');
            cy.contains('option', 'szybko').should('exist');

            cy.contains(`Godzina: ${expectedTime(flight.trackPoints[0].time)}`).should('be.visible');
            cy.contains('Prędkość:').parent().should('contain.text', '900 m/min');
            cy.contains('Wysokość:').parent().should('contain.text', '100 m n.p.m.');
            cy.contains('punkt 1 z 5').should('be.visible');

            cy.contains('h2', 'Przebieg lotu').should('be.visible');
            cy.get('svg[role="img"][aria-label^="Profile wysokości i prędkości"]').should('be.visible')
                .and('contain.text', 'Prędkość')
                .and('contain.text', 'Wysokość n.p.m.');

            cy.contains('h2', 'Szczegóły lotu').should('be.visible');

            cy.contains('span', 'Początek nagrania').parent().parent().should('contain.text', expectedTime(flight.startTime));
            cy.contains('span', 'Koniec nagrania').parent().parent().should('contain.text', expectedTime(flight.endTime));
            cy.contains('span', 'Pokonany dystans').parent().parent().should('contain.text', '5.80 km');
            cy.contains('span', 'Prędkość w linii prostej').parent().parent().should('contain.text', 'm/min');
            cy.contains('span', 'Wysokość lotu').parent().parent()
                .should('contain.text', '100–140 m n.p.m.')
                .and('contain.text', 'suma wznosów 40 m');

            cy.contains('span', 'Punkty trasy').parent().parent()
                .should('contain.text', '120')
                .and('contain.text', 'na mapie wyświetlono 5');

            cy.contains('span', 'Plik źródłowy').parent().parent().should('contain.text', 'dessau-2026.gpx');

            cy.checkFooter();
        });

        // ==========================================
        // MAP
        // ==========================================

        it('Should display the start and end markers on the flight map', () => {
            cy.get('.leaflet-marker-icon').should('have.length', 2);

            cy.get('.leaflet-marker-icon').eq(0).click({ force: true });
            cy.get('.leaflet-popup-content').should('be.visible')
                .and('contain.text', 'Początek trasy')
                .and('contain.text', 'Dessau')
                .and('contain.text', expectedTime(flight.startTime));

            cy.get('.leaflet-marker-icon').eq(1).click({ force: true });
            cy.get('.leaflet-popup-content').should('be.visible')
                .and('contain.text', 'Koniec trasy')
                .and('contain.text', expectedTime(flight.endTime));
        });

        it('Should allow toggling the straight start-to-finish line', () => {
            cy.contains('label', 'Linia prosta start-meta').find('input[type="checkbox"]').as('straightLine');

            cy.get('@straightLine').should('be.checked');
            cy.get('@straightLine').uncheck().should('not.be.checked');
            cy.get('@straightLine').check().should('be.checked');
        });

        // ==========================================
        // ROUTE PLAYBACK
        // ==========================================

        it('Should update the active route point using the playback slider', () => {
            cy.get('input[aria-label="Pozycja na trasie lotu"]').then(($input) => {
                const input = $input[0] as HTMLInputElement;
                const win = input.ownerDocument.defaultView!;

                const nativeValueSetter = Object.getOwnPropertyDescriptor(
                    win.HTMLInputElement.prototype,
                    'value'
                )?.set;

                expect(nativeValueSetter).to.exist;

                nativeValueSetter!.call(input, '2');
                input.dispatchEvent(new win.Event('input', { bubbles: true }));
            });

            cy.get('input[aria-label="Pozycja na trasie lotu"]').should('have.value', '2');
            cy.contains(`Godzina: ${expectedTime(flight.trackPoints[2].time)}`).should('be.visible');
            cy.contains('Prędkość:').parent().should('contain.text', '1100 m/min');
            cy.contains('Wysokość:').parent().should('contain.text', '120 m n.p.m.');
            cy.contains('punkt 3 z 5').should('be.visible');
        });

        it('Should allow changing the playback speed', () => {
            cy.get('select[aria-label="Prędkość odtwarzania"]').should('have.value', '25');

            cy.get('select[aria-label="Prędkość odtwarzania"]').select('60').should('have.value', '60');
            cy.get('select[aria-label="Prędkość odtwarzania"]').select('8').should('have.value', '8');
        });

        it('Should play and pause the recorded route', () => {
            cy.get('select[aria-label="Prędkość odtwarzania"]').select('8');

            cy.clock();

            cy.get('button[aria-label="Odtwórz trasę"]').click();
            cy.get('button[aria-label="Zatrzymaj odtwarzanie trasy"]').should('be.visible');

            cy.tick(130);

            cy.contains('punkt 2 z 5').should('be.visible');

            cy.get('button[aria-label="Zatrzymaj odtwarzanie trasy"]').click();
            cy.get('button[aria-label="Odtwórz trasę"]').should('be.visible');
        });

        // ==========================================
        // FLIGHT PROFILE
        // ==========================================

        it('Should synchronize the playback position after selecting a point on the flight profile', () => {
            cy.get('svg[role="img"][aria-label^="Profile wysokości i prędkości"]').click('center');

            cy.get('input[aria-label="Pozycja na trasie lotu"]').should('have.value', '2');
            cy.contains('punkt 3 z 5').should('be.visible');
        });

        // ==========================================
        // FLIGHT WITHOUT TIMESTAMPS
        // ==========================================

        it('Should correctly render a flight without timestamps or speed data', () => {
            const flightWithoutTimestamps: MockFlightDetails = {
                ...createFlightDetails(),
                startTime: null,
                endTime: null,
                statistics: {
                    ...createFlightDetails().statistics,
                    durationSeconds: 0,
                    averageSpeedMetersPerMinute: 0,
                    straightLineSpeedMetersPerMinute: 0,
                    maxSpeedMetersPerMinute: 0,
                    timestampsAvailable: false
                },
                trackPoints: createFlightDetails().trackPoints.map(point => ({
                    ...point,
                    time: null,
                    speedMetersPerMinute: null
                }))
            };

            cy.intercept('GET', '**/api/flights/1', {
                statusCode: 200,
                body: flightWithoutTimestamps
            }).as('getFlightWithoutTimestamps');

            cy.reload();
            cy.wait('@getFlightWithoutTimestamps');

            cy.contains('p', /^Czas nagrania$/).parent()
                .should('contain.text', '—')
                .and('contain.text', 'plik bez znaczników czasu');

            cy.contains('p', /^Prędkość średnia$/).parent().should('contain.text', '—');
            cy.contains('p', /^Prędkość maksymalna$/).parent().should('contain.text', '—');

            cy.contains('span', 'Początek nagrania').parent().parent().should('contain.text', '—');
            cy.contains('span', 'Koniec nagrania').parent().parent().should('contain.text', '—');
            cy.contains('span', 'Prędkość w linii prostej').parent().parent().should('contain.text', '—');

            cy.get('svg[role="img"][aria-label^="Profile wysokości i prędkości"]')
                .should('contain.text', 'Wysokość n.p.m.')
                .and('not.contain.text', 'Prędkość [m/min]');
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading flight details', () => {
            let shouldFail = true;

            cy.intercept('GET', '**/api/flights/1', (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać testowego lotu.'
                    });
                } else {
                    req.reply({
                        statusCode: 200,
                        body: flight
                    });
                }
            }).as('getFlightRetry');

            cy.reload();
            cy.wait('@getFlightRetry');

            cy.contains('Nie udało się pobrać testowego lotu.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait('@getFlightRetry');

            cy.contains('Nie udało się pobrać testowego lotu.').should('not.exist');
            cy.get('h1').should('have.text', 'Lot konkursowy Dessau');
            cy.get('.leaflet-container').should('be.visible');
        });

        // ==========================================
        // DELETE FLIGHT
        // ==========================================

        it('Should cancel flight deletion without removing the flight', () => {
            cy.contains('button', 'Usuń lot').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń lot').should('be.visible');
                cy.contains('Czy na pewno chcesz usunąć lot "Lot konkursowy Dessau" wraz z zapisaną trasą?').should('be.visible');
                cy.contains('Tej operacji nie można cofnąć.').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.get('h1').should('have.text', 'Lot konkursowy Dessau');
        });

        it('Should successfully delete a flight and return to the flights list', () => {
            cy.intercept('DELETE', '**/api/flights/1', {
                statusCode: 204
            }).as('deleteFlight');

            cy.intercept({
                method: 'GET',
                pathname: '/api/flights',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: {
                    content: [],
                    totalElements: 0,
                    totalPages: 0,
                    number: 0,
                    size: 10
                }
            }).as('getFlightsAfterDelete');

            cy.contains('button', 'Usuń lot').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteFlight');

            cy.location('pathname').should('eq', '/flights');
            cy.wait('@getFlightsAfterDelete');
        });

        it('Should display an error modal when flight deletion fails', () => {
            cy.intercept('DELETE', '**/api/flights/1', {
                statusCode: 500,
                body: 'Nie udało się usunąć testowego lotu.'
            }).as('deleteFlightError');

            cy.contains('button', 'Usuń lot').click();
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
            cy.get('h1').should('have.text', 'Lot konkursowy Dessau');
        });
    });

    // ==========================================
    // INVALID FLIGHT ID
    // ==========================================

    describe('Invalid Flight ID', () => {
        it('Should display an error for an invalid flight identifier', () => {
            cy.mockNavbarNotifications();
            cy.visitWithToken('/flights/abc', breederToken);

            cy.wait(['@getNotifications', '@getUnreadCount']);

            cy.checkLoggedInNavbar('Hodowca');

            cy.get('h1').should('be.visible').and('have.text', 'Nie udało się otworzyć lotu');
            cy.contains('Nieprawidłowy identyfikator lotu.').should('be.visible');
            cy.contains('a', 'Wróć do listy lotów').should('be.visible').and('have.attr', 'href', '/flights');

            cy.checkFooter();
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept('GET', '**/api/flights/1', {
                statusCode: 401,
                body: ''
            });

            cy.visit('/flights/1');

            cy.location('pathname').should('eq', '/login');
        });
    });
});