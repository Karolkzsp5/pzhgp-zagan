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
        selectSection(sectionValue?: string): Chainable<Element>;
        fillEmail(email?: string): Chainable<Element>;
        fillPhoneNumber(phoneNumber?: string): Chainable<Element>;
        fillPassword(password?: string): Chainable<Element>;
        fillHouseNumber(houseNumber?: string): Chainable<Element>;
    }
}

Cypress.Commands.add('fillEmail', (email = 'testcypress1@test.com') => {
    cy.get('input[name="email"]').clear().type(email, { delay: 20 });
});

Cypress.Commands.add('fillPhoneNumber', (phoneNumber = '444444444') => {
    cy.get('input[name="phoneNumber"]').clear().type(phoneNumber, { delay: 20 });
});

Cypress.Commands.add('fillPassword', (password = 'Testcypress1@') => {
    cy.get('input[name="password"]').clear().type(password);
    cy.get('input[name="confirmPassword"]').clear().type(password);
});

Cypress.Commands.add('selectSection', (sectionValue = '1') => {
    cy.get('select[name="sectionId"]').select(sectionValue);
});

Cypress.Commands.add('fillHouseNumber', (houseNumber = '14a') => {
    cy.get('input[name="houseNumber"]').clear().type(houseNumber, { delay: 20 });
});