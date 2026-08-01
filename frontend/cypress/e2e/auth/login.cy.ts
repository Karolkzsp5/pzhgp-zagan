import {exists} from "node:fs";

describe('Proces Logowania Hodowcy', () => {
    beforeEach(() => {
        cy.visit('/login');
    });

    it('Powinien wyświetlać stronę z formularzem i wszystkimi jego elementami', () => {
        cy.get('h1').should("exists").and('be.visible').and('contain.text', 'Logowanie Hodowcy');
        cy.get('form').should("exists").and('be.visible');
        cy.get('input[name="email"]').should("exists").and('be.visible');
        cy.get('input[name="password"]').should("exists").and('be.visible');
        cy.get('input[name="remember-me"]').should("exists").and('be.visible');
        cy.get('label[for="remember-me"]').should("exists").and('be.visible');
        cy.get('button[type="submit"]').should("exists").and('be.visible').and('not.be.disabled');
        cy.contains('Nie masz jeszcze konta?').should("exists").and('be.visible');
        cy.get('a').should('contain.text', 'Zarejestruj się tutaj').and("exists").and('be.visible');
    });
});