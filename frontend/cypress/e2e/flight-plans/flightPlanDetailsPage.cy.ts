import {createFakeTokenForUser, TEST_USERS} from '../../support/testUtils';

type MockPigeonAgeGroup = 'ADULT' | 'YOUNG';
type MockFlightResultScope = 'BRANCH' | 'SECTION';

interface MockFlightResultSummary {
    id: number;
    scope: MockFlightResultScope;
    sectionId: number | null;
    sectionName: string | null;
    sectionSortOrder: number | null;
    originalFileName: string;
}

interface MockFlightPlanEntry {
    id: number;
    pigeonAgeGroup: MockPigeonAgeGroup;
    scheduledDate: string;
    location: string;
    distanceKm: number;
    category: string | null;
    listType: string;
    results: MockFlightResultSummary[];
}

interface MockFlightPlanDetails {
    id: number;
    year: number;
    adultFlights: MockFlightPlanEntry[];
    youngFlights: MockFlightPlanEntry[];
}

interface MockSection {
    id: number;
    name: string;
}

describe('Flight Plan Details Page Tests', () => {
    const adminToken = createFakeTokenForUser(TEST_USERS.admin);
    const moderatorToken = createFakeTokenForUser(TEST_USERS.moderator);
    const breederToken = createFakeTokenForUser(TEST_USERS.breeder);

    const sections: MockSection[] = [
        {id: 1, name: 'Żagań'},
        {id: 2, name: 'Wymiarki'},
        {id: 3, name: 'Chotków'},
        {id: 4, name: 'Kożuchów'}
    ];

    const createPlan = (): MockFlightPlanDetails => ({
        id: 1,
        year: 2026,
        adultFlights: [
            {
                id: 101,
                pigeonAgeGroup: 'ADULT',
                scheduledDate: '2026-04-26',
                location: 'Dahme',
                distanceKm: 130,
                category: 'A',
                listType: 'Oddziałowa',
                results: [
                    {
                        id: 1001,
                        scope: 'BRANCH',
                        sectionId: null,
                        sectionName: null,
                        sectionSortOrder: null,
                        originalFileName: 'oddzial-dahme.txt'
                    },
                    {
                        id: 1002,
                        scope: 'SECTION',
                        sectionId: 1,
                        sectionName: 'Żagań',
                        sectionSortOrder: 1,
                        originalFileName: 'sekcja1-dahme.txt'
                    }
                ]
            },
            {
                id: 102,
                pigeonAgeGroup: 'ADULT',
                scheduledDate: '2026-05-03',
                location: 'Jessen',
                distanceKm: 170,
                category: 'A',
                listType: 'Oddziałowa',
                results: []
            }
        ],
        youngFlights: [
            {
                id: 201,
                pigeonAgeGroup: 'YOUNG',
                scheduledDate: '2026-08-09',
                location: 'Luckau',
                distanceKm: 150,
                category: null,
                listType: 'Oddziałowa',
                results: []
            }
        ]
    });

    let plan: MockFlightPlanDetails;

    const mockPlanRequest = () => {
        cy.intercept('GET', '**/api/flight-plans/2026', (req) => {
            req.reply({
                statusCode: 200,
                body: plan
            });
        }).as('getFlightPlan');
    };

    const mockSectionsRequest = () => {
        cy.intercept('GET', '**/api/sections', {
            statusCode: 200,
            body: sections
        }).as('getSections');
    };

    const getFlightSection = (title: string) => {
        return cy.contains('h2', title).closest('section');
    };

    const getFlightRow = (location: string) => {
        return cy.contains('td', location).closest('tr');
    };

    const openFlightActions = (flightId: number) => {
        cy.get(`[data-cy="flight-actions-button-${flightId}"]`).click();
        cy.get(`[data-cy="flight-actions-menu-${flightId}"]`).should('be.visible');
    };

    const openResultsModal = (flightId: number, buttonLabel: 'Dodaj wyniki' | 'Zarządzaj wynikami') => {
        openFlightActions(flightId);

        cy.get(`[data-cy="flight-actions-menu-${flightId}"]`).contains('button', buttonLabel).click();
        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Wyniki lotu').should('be.visible');
        });

        cy.wait('@getSections');
    };

    const getUploadedResult = (fileName: string) => {
        return cy.contains('p', fileName).parent().parent();
    };

    const updateFlight = (flightId: number, update: (flight: MockFlightPlanEntry) => MockFlightPlanEntry) => {
        plan = {
            ...plan,
            adultFlights: plan.adultFlights.map((flight) => flight.id === flightId ? update(flight) : flight),
            youngFlights: plan.youngFlights.map((flight) => flight.id === flightId ? update(flight) : flight)
        };
    };

    describe('Administrator', () => {
        beforeEach(() => {
            plan = createPlan();

            mockPlanRequest();
            mockSectionsRequest();
            cy.mockNavbarNotifications();

            cy.visitWithToken('/flight-plans/2026', adminToken);
            cy.wait(['@getFlightPlan', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render the flight plan and both flight tables', () => {
            cy.checkLoggedInNavbar('Admin');

            cy.contains('a', 'Wróć do planów').should('be.visible').and('have.attr', 'href', '/flight-plans');
            cy.get('[data-cy="flight-plan-year-title"]').should('be.visible').and('have.text', 'Plan lotów 2026');
            cy.contains('Plan oraz dostępne wyniki lotów oddziałowych i sekcyjnych.').should('be.visible');

            getFlightSection('Gołębie dorosłe').within(() => {
                cy.contains('button', '+ Dodaj lot').should('be.visible');

                cy.contains('th', 'Lp.').should('be.visible');
                cy.contains('th', 'Data').should('be.visible');
                cy.contains('th', 'Miejscowość').should('be.visible');
                cy.contains('th', 'Dystans').should('be.visible');
                cy.contains('th', 'Kategoria').should('be.visible');
                cy.contains('th', 'Rodzaj listy').should('be.visible');
                cy.contains('th', 'Wyniki').should('be.visible');
                cy.contains('th', 'Akcje').should('be.visible');
            });

            getFlightRow('Dahme').within(() => {
                cy.get('td').eq(0).should('have.text', '1');
                cy.contains('26.04.2026').should('be.visible');
                cy.contains('Dahme').should('be.visible');
                cy.contains('130 km').should('be.visible');
                cy.contains('A').should('be.visible');
                cy.contains('Oddziałowa').should('be.visible');

                cy.get('select[aria-label="Wyniki lotu Dahme"]').should('be.visible').find('option[value=""]').should('be.selected');
                cy.get('select[aria-label="Wyniki lotu Dahme"] option').should('have.length', 3);

                cy.contains('option', 'Oddział').should('exist');
                cy.contains('option', 'Sekcja 1 Żagań').should('exist');
            });

            getFlightRow('Jessen').within(() => {
                cy.get('td').eq(0).should('have.text', '2');
                cy.contains('03.05.2026').should('be.visible');
                cy.contains('170 km').should('be.visible');
                cy.contains('Brak wyników').should('be.visible');
            });

            getFlightSection('Gołębie młode').within(() => {
                cy.contains('button', '+ Dodaj lot').should('be.visible');
            });

            getFlightRow('Luckau').within(() => {
                cy.get('td').eq(0).should('have.text', '1');
                cy.contains('09.08.2026').should('be.visible');
                cy.contains('150 km').should('be.visible');
                cy.get('td').eq(4).should('have.text', '—');
                cy.contains('Brak wyników').should('be.visible');
            });

            cy.checkFooter();
        });

        // ==========================================
        // RESULT SELECT PLACEHOLDER
        // ==========================================

        it('Should keep "Wybierz wyniki" as a disabled hidden placeholder', () => {
            cy.get('select[aria-label="Wyniki lotu Dahme"]').within(() => {
                cy.get('option[value=""]').should('be.disabled');
                cy.get('option[value=""]').should('have.attr', 'hidden');
                cy.get('option[value=""]').should('have.text', 'Wybierz wyniki');
                cy.get('option[value=""]').should('be.selected');

                cy.get('option[value="1001"]').should('have.text', 'Oddział');
                cy.get('option[value="1002"]').should('have.text', 'Sekcja 1 Żagań');
            });
        });

        // ==========================================
        // ACTION MENU
        // ==========================================

        it('Should correctly render the flight ActionMenu', () => {
            openFlightActions(101);

            cy.get('[data-cy="flight-actions-menu-101"]').should('have.class', 'w-48')
                .and('have.class', 'whitespace-nowrap').within(() => {
                cy.contains('button', 'Edytuj lot').should('be.visible');
                cy.contains('button', 'Zarządzaj wynikami').should('be.visible');
                cy.contains('button', 'Usuń lot').should('be.visible');
            });

            cy.get('body').type('{esc}');
            cy.get('[data-cy="flight-actions-menu-101"]').should('not.exist');

            openFlightActions(102);

            cy.get('[data-cy="flight-actions-menu-102"]').within(() => {
                cy.contains('button', 'Dodaj wyniki').should('be.visible');
            });
        });

        // ==========================================
        // ADD FLIGHT MODAL
        // ==========================================

        it('Should correctly render and close the add flight modal', () => {
            getFlightSection('Gołębie dorosłe').contains('button', '+ Dodaj lot').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Dodaj lot').should('be.visible');
                cy.contains('Gołębie dorosłe').should('be.visible');

                cy.get('#flight-date').should('be.visible').and('have.attr', 'min', '2026-01-01')
                    .and('have.attr', 'max', '2026-12-31').and('have.attr', 'required');

                cy.get('#flight-location').should('be.visible').and('have.attr', 'maxlength', '100');

                cy.get('#flight-distance').should('be.visible').and('have.attr', 'min', '1')
                    .and('have.attr', 'max', '3000');

                cy.get('#flight-category').should('be.visible').and('have.attr', 'maxlength', '30');
                cy.get('#flight-list-type').should('be.visible').and('have.attr', 'maxlength', '100');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
        });

        // ==========================================
        // FLIGHT FORM VALIDATION
        // ==========================================

        it('Should validate flight data before sending the request', () => {
            getFlightSection('Gołębie dorosłe').contains('button', '+ Dodaj lot').click();

            cy.get('[role="dialog"] form').invoke('attr', 'novalidate', '');
            cy.get('[role="dialog"]').contains('button', 'Dodaj lot').click();
            cy.get('[role="alert"]').should('be.visible').and('have.text', 'Data planowanego lotu jest wymagana.');

            cy.get('#flight-date').type('2026-05-10');
            cy.get('#flight-location').type('Dessau');
            cy.get('#flight-distance').type('3001');
            cy.get('#flight-list-type').type('Oddziałowa');

            cy.get('[role="dialog"]').contains('button', 'Dodaj lot').click();

            cy.get('[role="alert"]').should('be.visible')
                .and('have.text', 'Dystans musi mieścić się w zakresie od 1 do 3000 km.');
        });

        // ==========================================
        // CREATE FLIGHT
        // ==========================================

        it('Should successfully add a flight to the adult table', () => {
            cy.intercept('POST', '**/api/flight-plans/2026/entries', (req) => {
                expect(req.body).to.deep.equal({
                    pigeonAgeGroup: 'ADULT',
                    scheduledDate: '2026-05-10',
                    location: 'Dessau',
                    distanceKm: 220,
                    category: 'A/B',
                    listType: 'Oddziałowa'
                });

                plan = {
                    ...plan,
                    adultFlights: [
                        ...plan.adultFlights,
                        {
                            id: 103,
                            pigeonAgeGroup: 'ADULT',
                            scheduledDate: '2026-05-10',
                            location: 'Dessau',
                            distanceKm: 220,
                            category: 'A/B',
                            listType: 'Oddziałowa',
                            results: []
                        }
                    ]
                };

                req.reply({
                    statusCode: 201,
                    body: {
                        id: 103
                    }
                });
            }).as('createFlight');

            getFlightSection('Gołębie dorosłe').contains('button', '+ Dodaj lot').click();

            cy.get('#flight-date').type('2026-05-10');
            cy.get('#flight-location').type('Dessau');
            cy.get('#flight-distance').type('220');
            cy.get('#flight-category').type('A/B');
            cy.get('#flight-list-type').type('Oddziałowa');
            cy.get('[role="dialog"]').contains('button', 'Dodaj lot').click();

            cy.wait('@createFlight');
            cy.wait('@getFlightPlan');

            cy.get('[role="dialog"]').should('not.exist');

            getFlightRow('Dessau').should('be.visible').within(() => {
                cy.get('td').eq(0).should('have.text', '3');
                cy.contains('10.05.2026').should('be.visible');
                cy.contains('220 km').should('be.visible');
                cy.contains('A/B').should('be.visible');
            });
        });

        // ==========================================
        // CREATE FLIGHT ERROR
        // ==========================================

        it('Should display an API error when adding a flight fails', () => {
            cy.intercept('POST', '**/api/flight-plans/2026/entries', {
                statusCode: 400,
                body: 'Testowy błąd podczas dodawania lotu.'
            }).as('createFlightError');

            getFlightSection('Gołębie dorosłe').contains('button', '+ Dodaj lot').click();

            cy.get('#flight-date').type('2026-05-10');
            cy.get('#flight-location').type('Dessau');
            cy.get('#flight-distance').type('220');
            cy.get('#flight-list-type').type('Oddziałowa');
            cy.get('[role="dialog"]').contains('button', 'Dodaj lot').click();

            cy.wait('@createFlightError');

            cy.get('[role="alert"]').should('be.visible').and('have.text', 'Testowy błąd podczas dodawania lotu.');
            cy.get('[role="dialog"]').should('be.visible');
        });

        // ==========================================
        // EDIT FLIGHT
        // ==========================================

        it('Should preload and successfully update an existing flight', () => {
            cy.intercept('PUT', '**/api/flight-plans/entries/101', (req) => {
                expect(req.body).to.deep.equal({
                    pigeonAgeGroup: 'ADULT',
                    scheduledDate: '2026-04-27',
                    location: 'Dahme Updated',
                    distanceKm: 135,
                    category: null,
                    listType: 'Okręgowa'
                });

                updateFlight(101, (flight) => ({
                    ...flight,
                    scheduledDate: '2026-04-27',
                    location: 'Dahme Updated',
                    distanceKm: 135,
                    category: null,
                    listType: 'Okręgowa'
                }));

                req.reply({
                    statusCode: 200,
                    body: ''
                });
            }).as('updateFlight');

            openFlightActions(101);

            cy.get('[data-cy="flight-actions-menu-101"]').contains('button', 'Edytuj lot').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Edytuj lot').should('be.visible');
                cy.contains('Gołębie dorosłe').should('be.visible');

                cy.get('#flight-date').should('have.value', '2026-04-26');
                cy.get('#flight-location').should('have.value', 'Dahme');
                cy.get('#flight-distance').should('have.value', '130');
                cy.get('#flight-category').should('have.value', 'A');
                cy.get('#flight-list-type').should('have.value', 'Oddziałowa');
            });

            cy.get('#flight-date').clear().type('2026-04-27');
            cy.get('#flight-location').clear().type('Dahme Updated');
            cy.get('#flight-distance').clear().type('135');
            cy.get('#flight-category').clear();
            cy.get('#flight-list-type').clear().type('Okręgowa');
            cy.get('[role="dialog"]').contains('button', 'Zapisz zmiany').click();

            cy.wait('@updateFlight');
            cy.wait('@getFlightPlan');

            cy.get('[role="dialog"]').should('not.exist');

            getFlightRow('Dahme Updated').within(() => {
                cy.contains('27.04.2026').should('be.visible');
                cy.contains('135 km').should('be.visible');
                cy.get('td').eq(4).should('have.text', '—');
                cy.contains('Okręgowa').should('be.visible');
            });
        });

        // ==========================================
        // CANCEL DELETE FLIGHT
        // ==========================================

        it('Should cancel flight deletion', () => {
            openFlightActions(102);

            cy.get('[data-cy="flight-actions-menu-102"]').contains('button', 'Usuń lot').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń lot').should('be.visible');
                cy.contains('Czy na pewno chcesz usunąć lot Jessen z dnia 03.05.2026?').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            getFlightRow('Jessen').should('be.visible');
        });

        // ==========================================
        // DELETE FLIGHT
        // ==========================================

        it('Should successfully delete a flight without results', () => {
            cy.intercept('DELETE', '**/api/flight-plans/entries/102', (req) => {
                plan = {
                    ...plan,
                    adultFlights: plan.adultFlights.filter(
                        (flight) => flight.id !== 102
                    )
                };

                req.reply({
                    statusCode: 204
                });
            }).as('deleteFlight');

            openFlightActions(102);

            cy.get('[data-cy="flight-actions-menu-102"]').contains('button', 'Usuń lot').click();
            cy.get('[role="dialog"]').contains('button', 'Potwierdź').click();

            cy.wait('@deleteFlight');
            cy.wait('@getFlightPlan');

            getFlightSection('Gołębie dorosłe').find('tbody').should('not.contain.text', 'Jessen');
            getFlightRow('Dahme').should('be.visible');
        });

        // ==========================================
        // DELETE FLIGHT ERROR
        // ==========================================

        it('Should display an error modal when deleting a flight with results fails', () => {
            cy.intercept('DELETE', '**/api/flight-plans/entries/101', {
                statusCode: 409,
                body: 'Nie można usunąć lotu z planu, ponieważ posiada przypisane wyniki. Najpierw usuń wyniki tego lotu.'
            }).as('deleteFlightError');

            openFlightActions(101);

            cy.get('[data-cy="flight-actions-menu-101"]').contains('button', 'Usuń lot').click();
            cy.get('[role="dialog"]').contains('button', 'Potwierdź').click();

            cy.wait('@deleteFlightError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Nie udało się usunąć lotu').should('be.visible');
                cy.contains('Nie można usunąć lotu z planu, ponieważ posiada przypisane wyniki. Najpierw usuń wyniki tego lotu.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            getFlightRow('Dahme').should('be.visible');
        });

        // ==========================================
        // RESULT MODAL
        // ==========================================

        it('Should correctly render the result management modal', () => {
            openResultsModal(101, 'Zarządzaj wynikami');

            cy.get('[role="dialog"]').within(() => {
                cy.contains('Dahme').should('be.visible');
                cy.contains('26.04.2026 · 130 km').should('be.visible');

                cy.contains('h3', 'Wgrane wyniki').should('be.visible');

                cy.contains('Oddział').should('be.visible');
                cy.contains('oddzial-dahme.txt').should('be.visible');

                cy.contains('Sekcja 1 Żagań').should('be.visible');
                cy.contains('sekcja1-dahme.txt').should('be.visible');

                cy.contains('h3', 'Dodaj wyniki').should('be.visible');
                cy.get('#flight-result-scope').should('have.value', 'SECTION');
                cy.get('#flight-result-scope option[value="BRANCH"]').should('be.disabled').and('contain.text', 'wyniki już dodane');
                cy.get('#flight-result-section').should('be.visible');

                cy.get('#flight-result-section option[value=""]').should('be.disabled');
                cy.get('#flight-result-section option[value=""]').should('have.attr', 'hidden');
                cy.get('#flight-result-section option[value=""]').should('have.text', 'Wybierz sekcję');
                cy.get('#flight-result-section option[value=""]').should('be.selected');

                cy.get('#flight-result-section option[value="1"]').should('not.exist');
                cy.get('#flight-result-section option[value="2"]').should('have.text', 'Wymiarki');
                cy.get('#flight-result-section option[value="3"]').should('have.text', 'Chotków');
                cy.get('#flight-result-section option[value="4"]').should('have.text', 'Kożuchów');

                cy.get('#flight-result-file').should('exist').and('have.attr', 'accept', '.txt,text/plain');
                cy.contains('Przeciągnij plik .txt lub kliknij, aby wybrać').should('be.visible');
                cy.contains('Maksymalny rozmiar pliku: 5 MB').should('be.visible');
                cy.contains('button', 'Wgraj wyniki').should('be.disabled');
            });
        });

        // ==========================================
        // RESULT FILE SELECTION
        // ==========================================

        it('Should accept a valid TXT file using drag and drop', () => {
            openResultsModal(102, 'Dodaj wyniki');

            cy.get('#flight-result-file').parent('label').selectFile({
                contents: Cypress.Buffer.alloc(3072),
                fileName: 'wyniki-jessen.txt',
                mimeType: 'text/plain'
            }, {
                action: 'drag-drop'
            });

            cy.contains('wyniki-jessen.txt').should('be.visible');
            cy.contains('3 kB — kliknij, aby wybrać inny plik').should('be.visible');
            cy.contains('button', 'Wgraj wyniki').should('not.be.disabled');
            cy.get('[role="alert"]').should('not.exist');
        });

        it('Should reject a result file with an invalid extension', () => {
            openResultsModal(102, 'Dodaj wyniki');

            cy.get('#flight-result-file').selectFile({
                contents: Cypress.Buffer.from('invalid result'),
                fileName: 'wyniki.pdf',
                mimeType: 'application/pdf'
            }, {
                force: true
            });

            cy.get('[role="alert"]').should('be.visible')
                .and('have.text', 'Dozwolone są wyłącznie pliki z rozszerzeniem .txt.');

            cy.contains('wyniki.pdf').should('not.exist');
            cy.contains('button', 'Wgraj wyniki').should('be.disabled');
        });

        it('Should reject a result file larger than 5 MB', () => {
            openResultsModal(102, 'Dodaj wyniki');

            cy.get('#flight-result-file').selectFile({
                contents: Cypress.Buffer.alloc(5 * 1024 * 1024 + 1),
                fileName: 'large-results.txt',
                mimeType: 'text/plain'
            }, {
                force: true
            });

            cy.get('[role="alert"]').should('be.visible')
                .and('have.text', 'Plik jest za duży. Maksymalny rozmiar pliku z wynikami to 5 MB.');

            cy.contains('large-results.txt').should('not.exist');
            cy.contains('button', 'Wgraj wyniki').should('be.disabled');
        });

        // ==========================================
        // UPLOAD BRANCH RESULT
        // ==========================================

        it('Should successfully upload Branch results', () => {
            cy.intercept('POST', '**/api/flight-results/entries/102', (req) => {
                expect(req.headers['content-type']).to.include('multipart/form-data');
                expect(req.body).to.exist;

                updateFlight(102, (flight) => ({
                    ...flight,
                    results: [
                        {
                            id: 1003,
                            scope: 'BRANCH',
                            sectionId: null,
                            sectionName: null,
                            sectionSortOrder: null,
                            originalFileName: 'jessen-oddzial.txt'
                        }
                    ]
                }));

                req.reply({
                    statusCode: 201,
                    body: {
                        id: 1003
                    }
                });
            }).as('uploadBranchResult');

            openResultsModal(102, 'Dodaj wyniki');

            cy.get('#flight-result-scope').should('have.value', 'BRANCH');
            cy.get('#flight-result-file').selectFile({
                contents: Cypress.Buffer.from('Oficjalne wyniki oddziałowe Jessen'),
                fileName: 'jessen-oddzial.txt',
                mimeType: 'text/plain'
            }, {
                force: true
            });

            cy.contains('button', 'Wgraj wyniki').click();

            cy.wait('@uploadBranchResult');
            cy.wait('@getFlightPlan');

            cy.contains('jessen-oddzial.txt').should('be.visible');
            cy.get('[role="dialog"]').scrollTo('bottom');

            cy.get('[role="dialog"] [role="status"]').should('be.visible').and('have.text', 'Wyniki zostały wgrane.');
        });

        // ==========================================
        // UPLOAD SECTION RESULT
        // ==========================================

        it('Should successfully upload results for a selected Section', () => {
            cy.intercept('POST', '**/api/flight-results/entries/101', (req) => {
                expect(req.headers['content-type']).to.include('multipart/form-data');

                expect(req.body).to.exist;

                updateFlight(101, (flight) => ({
                    ...flight,
                    results: [
                        ...flight.results,
                        {
                            id: 1003,
                            scope: 'SECTION',
                            sectionId: 2,
                            sectionName: 'Wymiarki',
                            sectionSortOrder: 2,
                            originalFileName: 'sekcja2-dahme.txt'
                        }
                    ]
                }));

                req.reply({
                    statusCode: 201,
                    body: {
                        id: 1003
                    }
                });
            }).as('uploadSectionResult');

            openResultsModal(101, 'Zarządzaj wynikami');

            cy.get('#flight-result-scope').should('have.value', 'SECTION');
            cy.get('#flight-result-section').select('2');
            cy.get('#flight-result-section').should('have.value', '2');

            cy.get('#flight-result-file').selectFile({
                contents: Cypress.Buffer.from('Oficjalne wyniki Sekcji 2'),
                fileName: 'sekcja2-dahme.txt',
                mimeType: 'text/plain'
            }, {
                force: true
            });

            cy.contains('button', 'Wgraj wyniki').click();

            cy.wait('@uploadSectionResult');
            cy.wait('@getFlightPlan');

            cy.contains('Sekcja 2 Wymiarki').should('be.visible');
            cy.contains('sekcja2-dahme.txt').should('be.visible');
            cy.get('[role="dialog"]').scrollTo('bottom');

            cy.get('[role="dialog"] [role="status"]').should('be.visible').and('have.text', 'Wyniki zostały wgrane.');
        });

        // ==========================================
        // UPLOAD RESULT ERROR
        // ==========================================

        it('Should display an API error when uploading results fails', () => {
            cy.intercept('POST', '**/api/flight-results/entries/102', {
                statusCode: 400,
                body: 'Testowy błąd wgrywania wyników.'
            }).as('uploadResultError');

            openResultsModal(102, 'Dodaj wyniki');

            cy.get('#flight-result-file').selectFile({
                contents: Cypress.Buffer.from('Test'),
                fileName: 'wyniki.txt',
                mimeType: 'text/plain'
            }, {
                force: true
            });

            cy.contains('button', 'Wgraj wyniki').click();
            cy.wait('@uploadResultError');

            cy.get('[role="alert"]').should('be.visible').and('have.text', 'Testowy błąd wgrywania wyników.');
            cy.contains('wyniki.txt').should('be.visible');
            cy.get('[role="dialog"]').should('be.visible');
        });

        // ==========================================
        // DELETE RESULT
        // ==========================================

        it('Should successfully delete uploaded results', () => {
            cy.intercept('DELETE', '**/api/flight-results/1001',
                (req) => {
                    updateFlight(101, (flight) => ({
                        ...flight,
                        results: flight.results.filter((result) => result.id !== 1001)
                    }));

                    req.reply({
                        statusCode: 204
                    });
                }
            ).as('deleteResult');

            openResultsModal(101, 'Zarządzaj wynikami');
            getUploadedResult('oddzial-dahme.txt').contains('button', 'Usuń').click();

            getUploadedResult('oddzial-dahme.txt').within(() => {
                cy.contains('Usunąć?').should('be.visible');
                cy.contains('button', 'Anuluj').should('be.visible');
                cy.contains('button', 'Usuń').click();
            });

            cy.wait('@deleteResult');
            cy.wait('@getFlightPlan');

            cy.contains('oddzial-dahme.txt').should('not.exist');
            cy.get('[role="status"]').should('be.visible').and('have.text', 'Wyniki zostały usunięte.');
        });

        // ==========================================
        // DELETE RESULT ERROR
        // ==========================================

        it('Should display an API error when deleting results fails', () => {
            cy.intercept('DELETE', '**/api/flight-results/1001', {
                statusCode: 500,
                body: 'Testowy błąd usuwania wyników.'
            }).as('deleteResultError');

            openResultsModal(101, 'Zarządzaj wynikami');

            getUploadedResult('oddzial-dahme.txt').contains('button', 'Usuń').click();
            getUploadedResult('oddzial-dahme.txt').contains('button', 'Usuń').click();

            cy.wait('@deleteResultError');

            cy.get('[role="alert"]').should('be.visible').and('have.text', 'Testowy błąd usuwania wyników.');
            cy.contains('oddzial-dahme.txt').should('be.visible');
        });

        // ==========================================
        // OPEN RESULT
        // ==========================================

        it('Should download the selected result and open it in a new tab', () => {
            cy.intercept('GET', '**/api/flight-results/1001/file', {
                statusCode: 200,
                headers: {'content-type': 'text/plain; charset=UTF-8'},
                body: 'Oficjalne wyniki lotu Dahme'
            }).as('getResultFile');

            let openedWindow: any;

            cy.window().then((win) => {
                openedWindow = {
                    location: {
                        href: ''
                    },
                    close: cy.stub()
                };

                const windowOpenStub = cy.stub(win, 'open').returns(openedWindow);
                const createObjectUrlStub = cy.stub(win.URL, 'createObjectURL').returns('blob:test-flight-result');

                cy.wrap(windowOpenStub).as('windowOpen');
                cy.wrap(createObjectUrlStub).as('createObjectUrl');
            });

            cy.get('select[aria-label="Wyniki lotu Dahme"]').select('1001');

            cy.wait('@getResultFile');

            cy.get('@windowOpen').should('have.been.calledOnceWith', '', '_blank');
            cy.get('@createObjectUrl').should('have.been.calledOnce');

            cy.then(() => {
                expect(openedWindow.location.href).to.eq('blob:test-flight-result');
            });

            cy.get('select[aria-label="Wyniki lotu Dahme"]').should('be.visible').find('option[value=""]').should('be.selected');
        });

        // ==========================================
        // BLOCKED POPUP
        // ==========================================

        it('Should display an alert when the browser blocks the result window', () => {
            cy.window().then((win) => {
                const windowOpenStub = cy.stub(win, 'open').returns(null);
                cy.wrap(windowOpenStub).as('windowOpen');
            });

            cy.get('select[aria-label="Wyniki lotu Dahme"]').select('1001');
            cy.get('@windowOpen').should('have.been.calledOnceWith', '', '_blank');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Nie można otworzyć wyników').should('be.visible');
                cy.contains('Przeglądarka zablokowała otwarcie nowej karty. Zezwól na wyskakujące okna dla tej strony i spróbuj ponownie.')
                    .should('be.visible');

                cy.contains('button', 'OK').click();
            });
        });

        // ==========================================
        // RESULT DOWNLOAD ERROR
        // ==========================================

        it('Should close the new window and display an error when result download fails', () => {
            cy.intercept('GET', '**/api/flight-results/1001/file', {
                statusCode: 500,
                body: 'Nie udało się pobrać testowych wyników.'
            }).as('getResultFileError');

            let openedWindow: any;
            cy.window().then((win) => {
                openedWindow = {
                    location: {
                        href: ''
                    },
                    close: cy.stub().as('closeResultWindow')
                };

                cy.stub(win, 'open').returns(openedWindow);
            });

            cy.get('select[aria-label="Wyniki lotu Dahme"]').select('1001');
            cy.wait('@getResultFileError');
            cy.get('@closeResultWindow').should('have.been.calledOnce');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Nie udało się otworzyć wyników').should('be.visible');
                cy.contains('Nie udało się pobrać testowych wyników.').should('be.visible');
                cy.contains('button', 'OK').click();
            });
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading the flight plan', () => {
            let shouldFail = true;

            cy.intercept('GET', '**/api/flight-plans/2026', (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać testowego planu lotów.'
                    });

                    return;
                }

                req.reply({
                    statusCode: 200,
                    body: plan
                });
            }).as('getFlightPlanRetry');

            cy.reload();
            cy.wait('@getFlightPlanRetry');

            cy.contains('Nie udało się pobrać testowego planu lotów.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();

            cy.wait('@getFlightPlanRetry');

            cy.contains('Nie udało się pobrać testowego planu lotów.').should('not.exist');
            cy.get('[data-cy="flight-plan-year-title"]').should('have.text', 'Plan lotów 2026');
        });
    });

    // ==========================================
    // READ-ONLY ROLES
    // ==========================================

    describe('Read-only roles', () => {
        beforeEach(() => {
            plan = createPlan();

            mockPlanRequest();
            cy.mockNavbarNotifications();
        });

        it('Should allow a Moderator to view the plan without management controls', () => {
            cy.visitWithToken('/flight-plans/2026', moderatorToken);

            cy.wait(['@getFlightPlan', '@getNotifications', '@getUnreadCount']);
            cy.checkLoggedInNavbar('Moderator');

            cy.get('[data-cy="flight-plan-year-title"]').should('have.text', 'Plan lotów 2026');
            cy.contains('button', '+ Dodaj lot').should('not.exist');
            cy.get('[data-cy^="flight-actions-button-"]').should('not.exist');
            cy.get('select[aria-label="Wyniki lotu Dahme"]').should('be.visible');

            cy.contains('Oddział').should('exist');
            cy.contains('Sekcja 1 Żagań').should('exist');
        });

        it('Should allow a Breeder to view the plan without management controls', () => {
            cy.visitWithToken('/flight-plans/2026', breederToken);

            cy.wait(['@getFlightPlan', '@getNotifications', '@getUnreadCount']);
            cy.checkLoggedInNavbar('Hodowca');

            cy.contains('button', '+ Dodaj lot').should('not.exist');
            cy.get('[data-cy^="flight-actions-button-"]').should('not.exist');

            getFlightRow('Dahme').should('be.visible');
            getFlightRow('Jessen').should('be.visible');
            getFlightRow('Luckau').should('be.visible');

            cy.get('select[aria-label="Wyniki lotu Dahme"]').should('be.visible');
        });
    });

    // ==========================================
    // INVALID YEAR
    // ==========================================

    describe('Invalid Year', () => {
        it('Should display an error for an invalid flight plan year', () => {
            cy.mockNavbarNotifications();
            cy.visitWithToken('/flight-plans/abc', breederToken);

            cy.wait(['@getNotifications', '@getUnreadCount']);

            cy.contains('Nieprawidłowy rok planu lotów.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept('GET', '**/api/flight-plans/2026', {
                statusCode: 401,
                body: ''
            });

            cy.visit('/flight-plans/2026');
            cy.location('pathname').should('eq', '/login');
        });
    });
});