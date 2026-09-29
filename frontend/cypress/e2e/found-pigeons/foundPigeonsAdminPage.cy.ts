import { createFakeToken } from '../../support/testUtils';

type MockReportLanguage = 'PL' | 'EN' | 'DE';
type MockFoundPigeonStatus = 'PENDING' | 'APPROVED' | 'RESOLVED' | 'REJECTED';

interface MockFoundPigeon {
    id: number;
    ringNumber: string;
    contactPhone: string | null;
    contactEmail: string | null;
    foundLocation: string | null;
    foundCountry: string | null;
    description: string | null;
    preferredLanguage: MockReportLanguage;
    status: MockFoundPigeonStatus;
    adminNote: string | null;
    createdAt: string;
    updatedAt: string | null;
}

interface MockReportsPage {
    content: MockFoundPigeon[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

describe('Found Pigeons Admin Page Tests', () => {
    const adminToken = createFakeToken('ADMINISTRATOR', 'admin@pzhgp.pl', 'Admin');
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const createReportsPage = (): MockReportsPage => ({
        content: [
            {
                id: 1,
                ringNumber: 'PL-0369-26-1234',
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
                ringNumber: 'PL-0369-26-5678',
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
    });

    const createSingleReportPage = (report: MockFoundPigeon): MockReportsPage => ({
        content: [report],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 10
    });

    const createEmptyReportsPage = (): MockReportsPage => ({
        content: [],
        totalElements: 0,
        totalPages: 0,
        number: 0,
        size: 10
    });

    const openReport = (ringNumber: string) => {
        cy.contains('button', ringNumber).click();
        cy.contains('h2', ringNumber).should('be.visible');
    };

    const getReportRow = (ringNumber: string) => {
        return cy.contains('button', ringNumber).closest('tr');
    };

    let reportsPage: MockReportsPage;

    describe('Administrator', () => {
        beforeEach(() => {
            reportsPage = createReportsPage();

            cy.intercept('GET', '**/api/admin/found-pigeons?*', (req) => {
                req.reply({
                    statusCode: 200,
                    body: reportsPage
                });
            }).as('getReports');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/found-pigeons/admin', adminToken);

            cy.wait(['@getReports', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render all visible elements of the admin reports page', () => {
            cy.checkLoggedInNavbar('Admin');

            cy.contains('a', 'Formularz zgłoszeń').should('be.visible').and('have.attr', 'href', '/found-pigeons');

            cy.get('h1').should('be.visible').and('have.text', 'Zgłoszenia znalezionych gołębi');
            cy.contains('Dane kontaktowe znalazców są widoczne wyłącznie na tej stronie.').should('be.visible');

            cy.contains('Status').should('be.visible');

            cy.contains('button', 'Wszystkie').should('be.visible').and('have.attr', 'aria-pressed', 'true');
            cy.contains('button', 'Oczekujące').should('be.visible').and('have.attr', 'aria-pressed', 'false');
            cy.contains('button', 'W trakcie').should('be.visible').and('have.attr', 'aria-pressed', 'false');
            cy.contains('button', 'Zakończone').should('be.visible').and('have.attr', 'aria-pressed', 'false');
            cy.contains('button', 'Odrzucone').should('be.visible').and('have.attr', 'aria-pressed', 'false');

            cy.contains('label', 'Numer obrączki').should('be.visible');
            cy.get('#ring-search').should('be.visible').and('have.attr', 'type', 'search');
            cy.contains('button', 'Szukaj').should('be.visible');

            cy.contains('th', 'Obrączka').should('be.visible');
            cy.contains('th', 'Status').should('be.visible');
            cy.contains('th', 'Miejsce').should('be.visible');
            cy.contains('th', 'Zgłoszono').should('be.visible');

            getReportRow('PL-0369-26-1234').should('be.visible').within(() => {
                cy.contains('Oczekujące').should('be.visible');
                cy.contains('Cottbus, Niemcy').should('be.visible');
            });

            getReportRow('PL-0369-26-5678').should('be.visible').within(() => {
                cy.contains('W trakcie').should('be.visible');
                cy.contains('Żagań, Polska').should('be.visible');
            });

            cy.contains('Zgłoszeń: 2').should('be.visible');

            cy.contains('Wybierz zgłoszenie z listy, aby zobaczyć dane kontaktowe znalazcy.').should('be.visible');

            cy.contains('button', 'Poprzednia').should('not.exist');
            cy.contains('button', 'Następna').should('not.exist');

            cy.checkFooter();
        });

        // ==========================================
        // REPORT DETAILS
        // ==========================================

        it('Should display all report details after selecting a report', () => {
            openReport('PL-0369-26-1234');

            cy.contains('h2', 'PL-0369-26-1234').should('be.visible');
            cy.contains('Oczekujące').should('be.visible');

            cy.contains('dt', 'Język znalazcy').parent().within(() => {
                cy.contains('niemiecki').should('be.visible');
            });

            cy.contains('dt', 'Telefon').parent().within(() => {
                cy.contains('+49 30 12345678').should('be.visible');
                cy.get('a[href="tel:+493012345678"]').should('be.visible');
            });

            cy.contains('dt', 'E-mail').parent().within(() => {
                cy.contains('finder@example.de').should('be.visible');
                cy.get('a[href="mailto:finder@example.de"]').should('be.visible');
            });

            cy.contains('dt', 'Miejsce').parent().within(() => {
                cy.contains('Cottbus, Niemcy').should('be.visible');
            });

            cy.contains('h3', 'Opis od znalazcy').should('be.visible');
            cy.contains('Gołąb siedzi na parapecie od wczoraj.').should('be.visible');

            cy.contains('h3', 'Obsługa zgłoszenia').should('be.visible');
            cy.contains('button', 'Zatwierdź').should('be.visible');
            cy.contains('button', 'Odrzuć').should('be.visible');
            cy.contains('button', 'Oznacz jako zakończone').should('not.exist');

            cy.contains('label', 'Notatka administratora').should('be.visible');
            cy.get('#admin-note').should('be.visible').and('have.value', '');
            cy.contains('button', 'Zapisz notatkę').should('be.visible');

            cy.contains('button', 'Usuń zgłoszenie').should('be.visible');
        });

        it('Should display correct details when optional report values are missing', () => {
            openReport('PL-0369-26-5678');

            cy.contains('h2', 'PL-0369-26-5678').should('be.visible');
            cy.contains('W trakcie').should('be.visible');

            cy.contains('dt', 'Język znalazcy').parent().within(() => {
                cy.contains('polski').should('be.visible');
            });

            cy.contains('dt', 'Telefon').parent().within(() => {
                cy.contains('—').should('be.visible');
            });

            cy.contains('dt', 'E-mail').parent().within(() => {
                cy.contains('znalazca@example.pl').should('be.visible');
            });

            cy.contains('h3', 'Opis od znalazcy').should('not.exist');
            cy.get('#admin-note').should('have.value', 'Ustalono sekcję.');
        });

        // ==========================================
        // FILTERING
        // ==========================================

        it('Should filter reports by status', () => {
            const pendingReport = reportsPage.content[0];

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10',
                    status: 'PENDING'
                }
            }, {
                statusCode: 200,
                body: createSingleReportPage(pendingReport)
            }).as('getPendingReports');

            cy.contains('button', 'Oczekujące').click();

            cy.wait('@getPendingReports');

            cy.contains('button', 'Oczekujące').should('have.attr', 'aria-pressed', 'true');
            cy.contains('button', 'PL-0369-26-1234').should('be.visible');
            cy.contains('button', 'PL-0369-26-5678').should('not.exist');
            cy.contains('Zgłoszeń: 1').should('be.visible');
        });

        it('Should display an empty state when no reports match the selected status', () => {
            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10',
                    status: 'REJECTED'
                }
            }, {
                statusCode: 200,
                body: createEmptyReportsPage()
            }).as('getRejectedReports');

            cy.contains('button', 'Odrzucone').click();

            cy.wait('@getRejectedReports');

            cy.contains('Brak zgłoszeń spełniających wybrane kryteria.').should('be.visible');
        });

        // ==========================================
        // SEARCH
        // ==========================================

        it('Should search reports by ring number', () => {
            const report = reportsPage.content[0];

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10',
                    ringNumber: 'PL-0369-26-1234'
                }
            }, {
                statusCode: 200,
                body: createSingleReportPage(report)
            }).as('searchReports');

            cy.get('#ring-search').type('PL-0369-26-1234');
            cy.contains('button', 'Szukaj').click();

            cy.wait('@searchReports');

            cy.contains('button', 'PL-0369-26-1234').should('be.visible');
            cy.contains('button', 'PL-0369-26-5678').should('not.exist');
            cy.contains('Zgłoszeń: 1').should('be.visible');
        });

        // ==========================================
        // PAGINATION
        // ==========================================

        it('Should navigate between report pages', () => {
            const firstPage: MockReportsPage = {
                ...createReportsPage(),
                totalElements: 3,
                totalPages: 2,
                number: 0
            };

            const secondPageReport: MockFoundPigeon = {
                id: 3,
                ringNumber: 'PL-0369-26-9999',
                contactPhone: '601234567',
                contactEmail: null,
                foundLocation: 'Iłowa',
                foundCountry: 'Polska',
                description: 'Gołąb znaleziony przy posesji.',
                preferredLanguage: 'PL',
                status: 'REJECTED',
                adminNote: null,
                createdAt: '2026-09-18T12:00:00',
                updatedAt: null
            };

            const secondPage: MockReportsPage = {
                content: [secondPageReport],
                totalElements: 3,
                totalPages: 2,
                number: 1,
                size: 10
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getPaginationFirstPage');

            cy.reload();
            cy.wait('@getPaginationFirstPage');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('button', 'Poprzednia').should('be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '1',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: secondPage
            }).as('getPaginationSecondPage');

            cy.contains('button', 'Następna').should('be.visible').and('not.be.disabled').click();

            cy.wait('@getPaginationSecondPage');

            cy.contains('Strona 2 z 2').should('be.visible');
            cy.contains('button', 'PL-0369-26-9999').should('be.visible');
            cy.contains('button', 'Następna').should('be.disabled');
            cy.contains('button', 'Poprzednia').should('not.be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getPaginationBackToFirstPage');

            cy.contains('button', 'Poprzednia').click();

            cy.wait('@getPaginationBackToFirstPage');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('button', 'PL-0369-26-1234').should('be.visible');
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading reports', () => {
            let shouldFail = true;

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons'
            }, (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać zgłoszeń testowych.'
                    });
                } else {
                    req.reply({
                        statusCode: 200,
                        body: reportsPage
                    });
                }
            }).as('getReportsRetry');

            cy.reload();
            cy.wait('@getReportsRetry');

            cy.contains('Nie udało się pobrać zgłoszeń testowych.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait('@getReportsRetry');

            cy.contains('Nie udało się pobrać zgłoszeń testowych.').should('not.exist');
            cy.contains('button', 'PL-0369-26-1234').should('be.visible');
            cy.contains('button', 'PL-0369-26-5678').should('be.visible');
        });

        // ==========================================
        // STATUS TRANSITIONS
        // ==========================================

        it('Should show only allowed status transitions for a pending report', () => {
            openReport('PL-0369-26-1234');

            cy.contains('button', 'Zatwierdź').should('be.visible');
            cy.contains('button', 'Odrzuć').should('be.visible');
            cy.contains('button', 'Oznacz jako zakończone').should('not.exist');
            cy.contains('button', 'Cofnij do oczekujących').should('not.exist');
        });

        it('Should show only allowed status transitions for an approved report', () => {
            openReport('PL-0369-26-5678');

            cy.contains('button', 'Oznacz jako zakończone').should('be.visible');
            cy.contains('button', 'Odrzuć').should('be.visible');
            cy.contains('button', 'Zatwierdź').should('not.exist');
            cy.contains('button', 'Cofnij do oczekujących').should('not.exist');
        });

        it('Should prevent status changes for a resolved report', () => {
            const resolvedReport: MockFoundPigeon = {
                ...reportsPage.content[0],
                status: 'RESOLVED'
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: createSingleReportPage(resolvedReport)
            }).as('getResolvedReport');

            cy.reload();
            cy.wait('@getResolvedReport');

            openReport('PL-0369-26-1234');

            cy.contains('Zakończone').should('be.visible');
            cy.contains('Sprawa jest zamknięta — statusu nie można już zmienić.').should('be.visible');

            cy.contains('button', 'Zatwierdź').should('not.exist');
            cy.contains('button', 'Odrzuć').should('not.exist');
            cy.contains('button', 'Oznacz jako zakończone').should('not.exist');
        });

        it('Should prevent status changes for a rejected report', () => {
            const rejectedReport: MockFoundPigeon = {
                ...reportsPage.content[0],
                status: 'REJECTED'
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/admin/found-pigeons',
                query: {
                    page: '0',
                    size: '10'
                }
            }, {
                statusCode: 200,
                body: createSingleReportPage(rejectedReport)
            }).as('getRejectedReport');

            cy.reload();
            cy.wait('@getRejectedReport');

            openReport('PL-0369-26-1234');

            cy.contains('Odrzucone').should('be.visible');
            cy.contains('Sprawa jest zamknięta — statusu nie można już zmienić.').should('be.visible');
        });

        // ==========================================
        // STATUS UPDATE
        // ==========================================

        it('Should successfully approve a pending report', () => {
            cy.intercept('PATCH', '**/api/admin/found-pigeons/1/status', (req) => {
                expect(req.body).to.deep.equal({
                    status: 'APPROVED'
                });

                const updated: MockFoundPigeon = {
                    ...reportsPage.content[0],
                    status: 'APPROVED',
                    updatedAt: '2026-09-20T11:00:00'
                };

                reportsPage.content = reportsPage.content.map(report => report.id === 1 ? updated : report);

                req.reply({
                    statusCode: 200,
                    body: updated
                });
            }).as('approveReport');

            openReport('PL-0369-26-1234');
            cy.contains('button', 'Zatwierdź').click();

            cy.wait('@approveReport');

            cy.contains('h2', 'PL-0369-26-1234').should('be.visible');
            cy.contains('W trakcie').should('be.visible');
            cy.contains('button', 'Oznacz jako zakończone').should('be.visible');
            cy.contains('button', 'Odrzuć').should('be.visible');
            cy.contains('button', 'Zatwierdź').should('not.exist');

            getReportRow('PL-0369-26-1234').within(() => {
                cy.contains('W trakcie').should('be.visible');
            });
        });

        it('Should display an API error when changing report status fails', () => {
            cy.intercept('PATCH', '**/api/admin/found-pigeons/1/status', {
                statusCode: 400,
                body: 'Nie można zmienić statusu tego zgłoszenia.'
            }).as('updateStatusError');

            openReport('PL-0369-26-1234');
            cy.contains('button', 'Zatwierdź').click();

            cy.wait('@updateStatusError');

            cy.get('[role="alert"]').should('be.visible')
                .and('contain.text', 'Nie można zmienić statusu tego zgłoszenia.');

            cy.contains('Oczekujące').should('be.visible');
            cy.contains('button', 'Zatwierdź').should('be.visible');
        });

        // ==========================================
        // ADMIN NOTE
        // ==========================================

        it('Should successfully save an administrator note', () => {
            cy.intercept('PATCH', '**/api/admin/found-pigeons/1/note', (req) => {
                expect(req.body).to.deep.equal({
                    adminNote: 'Właściciel ustalony.'
                });

                const updated: MockFoundPigeon = {
                    ...reportsPage.content[0],
                    adminNote: 'Właściciel ustalony.',
                    updatedAt: '2026-09-20T11:30:00'
                };

                req.reply({
                    statusCode: 200,
                    body: updated
                });
            }).as('updateNote');

            openReport('PL-0369-26-1234');

            cy.get('#admin-note').type('Właściciel ustalony.');
            cy.contains('button', 'Zapisz notatkę').click();

            cy.wait('@updateNote');

            cy.get('#admin-note').should('have.value', 'Właściciel ustalony.');
            cy.get('[role="alert"]').should('not.exist');
        });

        it('Should display an API error when saving an administrator note fails', () => {
            cy.intercept('PATCH', '**/api/admin/found-pigeons/1/note', {
                statusCode: 500,
                body: 'Nie udało się zapisać notatki testowej.'
            }).as('updateNoteError');

            openReport('PL-0369-26-1234');

            cy.get('#admin-note').type('Testowa notatka');
            cy.contains('button', 'Zapisz notatkę').click();

            cy.wait('@updateNoteError');
            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nie udało się zapisać notatki testowej.');

            cy.get('#admin-note').should('have.value', 'Testowa notatka');
        });

        // ==========================================
        // DELETE REPORT
        // ==========================================

        it('Should cancel report deletion without removing the report', () => {
            openReport('PL-0369-26-1234');

            cy.contains('button', 'Usuń zgłoszenie').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń zgłoszenie').should('be.visible');
                cy.contains('Czy na pewno chcesz trwale usunąć zgłoszenie obrączki PL-0369-26-1234').should('be.visible');
                cy.contains('Tej operacji nie można cofnąć.').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('button', 'PL-0369-26-1234').should('be.visible');
            cy.contains('h2', 'PL-0369-26-1234').should('be.visible');
        });

        it('Should successfully delete a report', () => {
            cy.intercept('DELETE', '**/api/admin/found-pigeons/1', (req) => {
                reportsPage.content = reportsPage.content.filter(report => report.id !== 1);
                reportsPage.totalElements = reportsPage.content.length;

                req.reply({
                    statusCode: 204
                });
            }).as('deleteReport');

            openReport('PL-0369-26-1234');

            cy.contains('button', 'Usuń zgłoszenie').click();

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteReport');

            cy.contains('button', 'PL-0369-26-1234').should('not.exist');
            cy.contains('button', 'PL-0369-26-5678').should('be.visible');
            cy.contains('Zgłoszeń: 1').should('be.visible');
            cy.contains('Wybierz zgłoszenie z listy, aby zobaczyć dane kontaktowe znalazcy.').should('be.visible');
        });

        it('Should display an error modal when report deletion fails', () => {
            cy.intercept('DELETE', '**/api/admin/found-pigeons/1', {
                statusCode: 500,
                body: 'Nie udało się usunąć zgłoszenia testowego.'
            }).as('deleteReportError');

            openReport('PL-0369-26-1234');

            cy.contains('button', 'Usuń zgłoszenie').click();

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteReportError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd').should('be.visible');
                cy.contains('Nie udało się usunąć zgłoszenia testowego.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('button', 'PL-0369-26-1234').should('be.visible');
        });
    });

    // ==========================================
    // ADMIN ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.visit('/found-pigeons/admin');

            cy.location('pathname').should('eq', '/login');
        });

        it('Should redirect a breeder away from the admin reports page', () => {
            cy.intercept('GET', '**/api/announcements*', {
                statusCode: 200,
                body: {
                    content: [],
                    totalElements: 0,
                    totalPages: 0,
                    number: 0,
                    size: 10
                }
            });

            cy.mockNavbarNotifications();
            cy.visitWithToken('/found-pigeons/admin', breederToken);

            cy.location('pathname').should('eq', '/');
        });
    });
});