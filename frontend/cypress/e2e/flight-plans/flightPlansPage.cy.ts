import { createFakeTokenForUser, TEST_USERS } from '../../support/testUtils';

interface MockFlightPlanSummary {
    id: number;
    year: number;
}

describe('Flight Plans Page Tests', () => {
    const adminToken = createFakeTokenForUser(TEST_USERS.admin);
    const moderatorToken = createFakeTokenForUser(TEST_USERS.moderator);
    const breederToken = createFakeTokenForUser(TEST_USERS.breeder);

    const createPlans = (): MockFlightPlanSummary[] => [
        {
            id: 1,
            year: 2026
        },
        {
            id: 2,
            year: 2025
        }
    ];

    let plans: MockFlightPlanSummary[];

    const mockFlightPlansRequest = () => {
        cy.intercept('GET', '**/api/flight-plans', (req) => {
            req.reply({
                statusCode: 200,
                body: plans
            });
        }).as('getFlightPlans');
    };

    describe('Administrator', () => {
        beforeEach(() => {
            plans = createPlans();

            mockFlightPlansRequest();
            cy.mockNavbarNotifications();

            cy.visitWithToken('/flight-plans', adminToken);
            cy.wait(['@getFlightPlans', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render the flight plans page', () => {
            cy.checkLoggedInNavbar('Admin');

            cy.get('[data-cy="flight-plans-title"]').should('be.visible').and('have.text', 'Plany i wyniki lotów');
            cy.contains('Wybierz rok, aby wyświetlić plan oraz dostępne wyniki lotów.').should('be.visible');

            cy.get('[data-cy="add-flight-plan-button"]').should('be.visible').and('not.be.disabled')
                .and('have.text', '+ Dodaj plan');

            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible').within(() => {
                cy.contains('h2', '2026').should('be.visible');
                cy.get('a').should('have.attr', 'href', '/flight-plans/2026');
            });

            cy.get('[data-cy="flight-plan-card-2025"]').should('be.visible').within(() => {
                cy.contains('h2', '2025').should('be.visible');
                cy.get('a').should('have.attr', 'href', '/flight-plans/2025');
            });

            cy.get('[data-cy="delete-flight-plan-2026"]').should('be.visible')
                .and('have.attr', 'aria-label', 'Usuń plan lotów 2026');

            cy.get('[data-cy="delete-flight-plan-2025"]').should('be.visible');
            cy.checkFooter();
        });

        // ==========================================
        // CREATE MODAL
        // ==========================================

        it('Should correctly render and close the create flight plan modal', () => {
            cy.get('[data-cy="add-flight-plan-button"]').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Dodaj plan lotów').should('be.visible');
                cy.contains('label', 'Rok').should('be.visible');

                cy.get('[data-cy="flight-plan-year-input"]').should('be.visible')
                    .and('have.attr', 'type', 'number')
                    .and('have.attr', 'min', '2000')
                    .and('have.attr', 'max', '2100')
                    .and('have.attr', 'required');

                cy.get('[data-cy="save-flight-plan-button"]').should('be.visible').and('not.be.disabled')
                    .and('have.text', 'Dodaj plan');

                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
        });

        // ==========================================
        // VALIDATION
        // ==========================================

        it('Should validate the flight plan year before sending the request', () => {
            cy.get('[data-cy="add-flight-plan-button"]').click();
            cy.get('[role="dialog"] form').invoke('attr', 'novalidate', '');
            cy.get('[data-cy="flight-plan-year-input"]').clear().type('1999');

            cy.get('[data-cy="save-flight-plan-button"]').click();
            cy.get('[data-cy="flight-plan-form-error"]').should('be.visible')
                .and('have.text', 'Rok planu musi mieścić się w zakresie od 2000 do 2100.');

            cy.get('[role="dialog"]').should('be.visible');
        });

        // ==========================================
        // CREATE PLAN
        // ==========================================

        it('Should successfully create a flight plan', () => {
            cy.intercept('POST', '**/api/flight-plans', (req) => {
                expect(req.body).to.deep.equal({
                    year: 2027
                });

                plans = [
                    {
                        id: 3,
                        year: 2027
                    },
                    ...plans
                ];

                req.reply({
                    statusCode: 201,
                    body: {
                        id: 3
                    }
                });
            }).as('createFlightPlan');

            cy.get('[data-cy="add-flight-plan-button"]').click();
            cy.get('[data-cy="flight-plan-year-input"]').clear().type('2027');
            cy.get('[data-cy="save-flight-plan-button"]').click();

            cy.wait('@createFlightPlan');
            cy.wait('@getFlightPlans');

            cy.get('[role="dialog"]').should('not.exist');

            cy.get('[data-cy="flight-plan-card-2027"]').should('be.visible').within(() => {
                    cy.contains('h2', '2027').should('be.visible');
                    cy.get('a').should('have.attr', 'href', '/flight-plans/2027');
            });
        });

        // ==========================================
        // CREATE ERROR
        // ==========================================

        it('Should display an API error when creating a flight plan fails', () => {
            cy.intercept('POST', '**/api/flight-plans', {
                statusCode: 400,
                body: 'Plan lotów dla roku 2027 już istnieje.'
            }).as('createFlightPlanError');

            cy.get('[data-cy="add-flight-plan-button"]').click();
            cy.get('[data-cy="flight-plan-year-input"]').clear().type('2027');

            cy.get('[data-cy="save-flight-plan-button"]').click();
            cy.wait('@createFlightPlanError');

            cy.get('[data-cy="flight-plan-form-error"]').should('be.visible')
                .and('have.text', 'Plan lotów dla roku 2027 już istnieje.');

            cy.get('[role="dialog"]').should('be.visible');
        });

        // ==========================================
        // CANCEL DELETE
        // ==========================================

        it('Should cancel flight plan deletion', () => {
            cy.get('[data-cy="delete-flight-plan-2026"]').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                    cy.contains('h2', 'Usuń plan lotów').should('be.visible');
                    cy.contains('Czy na pewno chcesz usunąć plan lotów na rok 2026?').should('be.visible');
                    cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible');
        });

        // ==========================================
        // DELETE PLAN
        // ==========================================

        it('Should successfully delete an empty flight plan', () => {
            cy.intercept('DELETE', '**/api/flight-plans/2025', (req) => {
                plans = plans.filter((plan) => plan.year !== 2025);

                req.reply({
                    statusCode: 204
                });
            }).as('deleteFlightPlan');

            cy.get('[data-cy="delete-flight-plan-2025"]').click();

            cy.get('[role="dialog"]').within(() => {
                cy.contains('h2', 'Usuń plan lotów').should('be.visible');
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteFlightPlan');
            cy.wait('@getFlightPlans');

            cy.get('[role="dialog"]').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2025"]').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible');
        });

        // ==========================================
        // DELETE ERROR
        // ==========================================

        it('Should display an error modal when flight plan deletion fails', () => {
            cy.intercept('DELETE', '**/api/flight-plans/2026', {
                statusCode: 409,
                body: 'Nie można usunąć planu lotów, który zawiera loty. Najpierw usuń wszystkie pozycje planu.'
            }).as('deleteFlightPlanError');

            cy.get('[data-cy="delete-flight-plan-2026"]').click();

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteFlightPlanError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                    cy.contains('h2', 'Nie udało się usunąć planu').should('be.visible');
                    cy.contains('Nie można usunąć planu lotów, który zawiera loty. Najpierw usuń wszystkie pozycje planu.').should('be.visible');

                    cy.contains('button', 'OK').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible');
        });

        // ==========================================
        // EMPTY STATE
        // ==========================================

        it('Should display an empty state when there are no flight plans', () => {
            cy.intercept('GET', '**/api/flight-plans', {
                statusCode: 200,
                body: []
            }).as('getEmptyFlightPlans');

            cy.reload();
            cy.wait('@getEmptyFlightPlans');

            cy.contains('Brak dostępnych planów lotów.').should('be.visible');
            cy.get('[data-cy^="flight-plan-card-"]').should('not.exist');
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading flight plans', () => {
            let shouldFail = true;

            cy.intercept('GET', '**/api/flight-plans', (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać testowych planów lotów.'
                    });

                    return;
                }

                req.reply({
                    statusCode: 200,
                    body: plans
                });
            }).as('getFlightPlansRetry');

            cy.reload();
            cy.wait('@getFlightPlansRetry');

            cy.contains('Nie udało się pobrać testowych planów lotów.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait('@getFlightPlansRetry');

            cy.contains('Nie udało się pobrać testowych planów lotów.').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible');
        });
    });

    // ==========================================
    // READ-ONLY ROLES
    // ==========================================

    describe('Read-only roles', () => {
        beforeEach(() => {
            plans = createPlans();
            mockFlightPlansRequest();
        });

        it('Should hide flight plan management controls from a Moderator', () => {
            cy.mockNavbarNotifications();
            cy.visitWithToken('/flight-plans', moderatorToken);

            cy.wait(['@getFlightPlans', '@getNotifications', '@getUnreadCount']);
            cy.checkLoggedInNavbar('Moderator');

            cy.get('[data-cy="add-flight-plan-button"]').should('not.exist');
            cy.get('[data-cy^="delete-flight-plan-"]').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible');
            cy.get('[data-cy="flight-plan-card-2025"]').should('be.visible');
        });

        it('Should hide flight plan management controls from a Breeder', () => {
            cy.mockNavbarNotifications();
            cy.visitWithToken('/flight-plans', breederToken);

            cy.wait(['@getFlightPlans', '@getNotifications', '@getUnreadCount']);
            cy.checkLoggedInNavbar('Hodowca');

            cy.get('[data-cy="add-flight-plan-button"]').should('not.exist');
            cy.get('[data-cy^="delete-flight-plan-"]').should('not.exist');
            cy.get('[data-cy="flight-plan-card-2026"]').should('be.visible');
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept('GET', '**/api/flight-plans', {
                statusCode: 401,
                body: ''
            });

            cy.visit('/flight-plans');

            cy.location('pathname').should('eq', '/login');
        });
    });
});