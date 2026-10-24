import { createTestUser } from '../support/testUtils';

describe('Weather Page Tests', () => {
    const adminUser = createTestUser('ADMINISTRATOR', 'admin@pzhgp.pl', 'Admin');

    // ==========================================
    // PUBLIC USER
    // ==========================================

    it('Should correctly render the weather page for an unauthenticated user', () => {
        cy.intercept('GET', '**/api/auth/me', {
            statusCode: 401,
            body: 'Wymagane jest uwierzytelnienie.'
        }).as('getUnauthenticatedUser');

        cy.visit('/weather');
        cy.wait('@getUnauthenticatedUser').its('response.statusCode').should('eq', 401);

        cy.checkLoggedOutNavbar();

        cy.get('h1').should('be.visible').and('have.text', 'Mapa pogodowa');
        cy.contains('Sprawdź aktualne warunki atmosferyczne przed planowanym lotem.').should('be.visible');

        cy.get('iframe[title="Mapa pogodowa Windy dla Oddziału Żagań"]').should('be.visible')
            .and('have.attr', 'loading', 'lazy')
            .and('have.attr', 'src');

        cy.get('iframe[title="Mapa pogodowa Windy dla Oddziału Żagań"]').invoke('attr', 'src').then((src) => {
            expect(src).to.not.be.undefined;

            const url = new URL(src!);

            expect(url.origin).to.eq('https://embed.windy.com');
            expect(url.pathname).to.eq('/embed.html');
            expect(url.searchParams.get('type')).to.eq('map');
            expect(url.searchParams.get('location')).to.eq('coordinates');
            expect(url.searchParams.get('metricRain')).to.eq('mm');
            expect(url.searchParams.get('metricTemp')).to.eq('°C');
            expect(url.searchParams.get('metricWind')).to.eq('m/s');
            expect(url.searchParams.get('zoom')).to.eq('6');
            expect(url.searchParams.get('overlay')).to.eq('wind');
            expect(url.searchParams.get('product')).to.eq('ecmwf');
            expect(url.searchParams.get('level')).to.eq('surface');
            expect(url.searchParams.get('lat')).to.eq('50.986');
            expect(url.searchParams.get('lon')).to.eq('12.722');
            expect(url.searchParams.get('detailLat')).to.eq('51.619');
            expect(url.searchParams.get('detailLon')).to.eq('15.308');
            expect(url.searchParams.get('detail')).to.eq('true');
            expect(url.searchParams.get('message')).to.eq('true');
        });

        cy.checkFooter();
    });

    // ==========================================
    // AUTHENTICATED USER
    // ==========================================

    it('Should correctly render the weather page for an authenticated user', () => {
        cy.mockNavbarNotifications();
        cy.visitAsUser('/weather', adminUser);

        cy.wait(['@getNotifications', '@getUnreadCount']);

        cy.checkLoggedInNavbar('Admin');

        cy.get('h1').should('be.visible').and('have.text', 'Mapa pogodowa');
        cy.contains('Sprawdź aktualne warunki atmosferyczne przed planowanym lotem.').should('be.visible');

        cy.get('iframe[title="Mapa pogodowa Windy dla Oddziału Żagań"]').should('be.visible')
            .and('have.attr', 'loading', 'lazy')
            .and('have.attr', 'src');

        cy.checkFooter();
    });
});