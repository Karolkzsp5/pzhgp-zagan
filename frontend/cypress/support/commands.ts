/// <reference types="cypress" />
// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
//
// declare global {
//   namespace Cypress {
//     interface Chainable {
//       login(email: string, password: string): Chainable<void>
//       drag(subject: string, options?: Partial<TypeOptions>): Chainable<Element>
//       dismiss(subject: string, options?: Partial<TypeOptions>): Chainable<Element>
//       visit(originalFn: CommandOriginalFn, url: string, options: Partial<VisitOptions>): Chainable<Element>
//     }
//   }
// }
declare namespace Cypress {
    interface Chainable {
        checkFooter(): Chainable<void>;
        checkLoggedInNavbar(userName: string): Chainable<void>;
        checkLoggedOutNavbar(): Chainable<void>;
        mockNavbarNotifications(): Chainable<void>;
        visitAsUser(url: string, user: import('./testUtils').MockCurrentUser): Chainable<void>;
    }
}

Cypress.Commands.add('checkFooter', () => {
    cy.get('[data-cy="footer"]').should('be.visible');
    cy.get('[data-cy="footer-copyright"]').should('be.visible');
});

Cypress.Commands.add('checkLoggedInNavbar', (userName: string) => {
    cy.get('nav[aria-label="Główna nawigacja"]').should('be.visible');
    cy.contains('a', 'PZHGP Żagań').should('be.visible');
    cy.contains('Witaj,').should('be.visible');
    cy.contains('strong', userName).should('be.visible');
    cy.get('button[aria-label="Powiadomienia"]').should('be.visible');
});

Cypress.Commands.add('checkLoggedOutNavbar', () => {
    cy.get('nav[aria-label="Główna nawigacja"]').should('be.visible');
    cy.contains('a', 'PZHGP Żagań').should('be.visible');
    cy.contains('a', 'Zaloguj się').should('be.visible').and('have.attr', 'href', '/login');
    cy.contains('a', 'Rejestracja').should('be.visible').and('have.attr', 'href', '/register');
});

Cypress.Commands.add('mockNavbarNotifications', () => {
    cy.intercept(
        {
            method: 'GET',
            pathname: '/api/notifications',
            query: {
                page: '0',
                size: '50'
            }
        },
        {
            statusCode: 200,
            body: {
                content: [],
                totalElements: 0,
                totalPages: 0,
                number: 0,
                size: 50
            }
        }
    ).as('getNotifications');

    cy.intercept('GET', '**/api/notifications/unread-count', {
        statusCode: 200,
        body: 0
    }).as('getUnreadCount');
});

Cypress.Commands.add('visitAsUser', (url: string, user: import('./testUtils').MockCurrentUser) => {
    cy.intercept('GET', '**/api/auth/me', {
        statusCode: 200,
        body: user
    }).as('getCurrentUser');

    cy.visit(url, {
        onBeforeLoad: (win) => {
            // Mockowany token CSRF dla żądań POST/PUT/DELETE wykonywanych przez fetchApi.
            win.document.cookie = 'XSRF-TOKEN=cypress-test-csrf; Path=/; SameSite=Lax';
        }
    });
});
