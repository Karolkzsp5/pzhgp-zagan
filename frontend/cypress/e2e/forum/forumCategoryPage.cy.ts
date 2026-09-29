import { createFakeToken } from '../../support/testUtils';

interface MockForumCategory {
    id: number;
    name: string;
    description: string;
    sortOrder: number;
    createdAt: string;
    canEdit: boolean;
    canDelete: boolean;
}

interface MockForumThread {
    id: number;
    categoryId: number;
    title: string;
    authorName: string;
    repliesCount: number;
    views: number;
    createdAt: string;
    lastPostAt: string;
    isPinned: boolean;
    isLocked: boolean;
    canEdit: boolean;
    canDelete: boolean;
    canModerate: boolean;
}

interface MockThreadsPage {
    content: MockForumThread[];
    totalPages: number;
    totalElements: number;
    number: number;
}

describe('Forum Category Page Tests', () => {
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const createCategory = (): MockForumCategory => ({
        id: 1,
        name: 'Loty i treningi',
        description: 'Dyskusje o lotach, treningach i przygotowaniu gołębi.',
        sortOrder: 1,
        createdAt: '2026-09-20T10:00:00',
        canEdit: false,
        canDelete: false
    });

    const createThreadsPage = (): MockThreadsPage => ({
        content: [
            {
                id: 11,
                categoryId: 1,
                title: 'Przygotowanie do lotu konkursowego',
                authorName: 'Jan Kowalski',
                repliesCount: 2,
                views: 35,
                createdAt: '2026-09-20T09:00:00',
                lastPostAt: '2026-09-21T12:30:00',
                isPinned: true,
                isLocked: false,
                canEdit: false,
                canDelete: false,
                canModerate: false
            },
            {
                id: 12,
                categoryId: 1,
                title: 'Pierwszy trening młodych gołębi',
                authorName: 'Anna Nowak',
                repliesCount: 5,
                views: 84,
                createdAt: '2026-09-18T08:00:00',
                lastPostAt: '2026-09-20T16:00:00',
                isPinned: false,
                isLocked: true,
                canEdit: false,
                canDelete: false,
                canModerate: false
            }
        ],
        totalPages: 1,
        totalElements: 2,
        number: 0
    });

    const createEmptyThreadsPage = (): MockThreadsPage => ({
        content: [],
        totalPages: 0,
        totalElements: 0,
        number: 0
    });

    const getThreadRow = (title: string) => {
        return cy.contains('a', title).closest('tr');
    };

    describe('Authenticated User', () => {
        let category: MockForumCategory;
        let threadsPage: MockThreadsPage;

        beforeEach(() => {
            category = createCategory();
            threadsPage = createThreadsPage();

            cy.intercept('GET', '**/api/forum/categories/1', (req) => {
                req.reply({
                    statusCode: 200,
                    body: category
                });
            }).as('getCategory');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '0',
                    size: '15'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: threadsPage
                });
            }).as('getThreads');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum/1', breederToken);

            cy.wait(['@getCategory', '@getThreads', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render all visible elements of the forum category page', () => {
            cy.checkLoggedInNavbar('Hodowca');

            cy.get('nav').contains('a', 'Forum').should('be.visible').and('have.attr', 'href', '/forum');
            cy.get('nav').contains('Loty i treningi').should('be.visible');

            cy.get('h1').should('be.visible').and('have.text', 'Loty i treningi');
            cy.contains('Dyskusje o lotach, treningach i przygotowaniu gołębi.').should('be.visible');
            cy.contains('button', '+ Nowy wątek').should('be.visible');

            cy.contains('th', 'Wątek').should('be.visible');
            cy.contains('th', 'Statystyki').should('be.visible');
            cy.contains('th', 'Ostatni wpis').should('be.visible');

            getThreadRow('Przygotowanie do lotu konkursowego').should('be.visible').within(() => {
                cy.contains('a', 'Przygotowanie do lotu konkursowego').should('have.attr', 'href', '/forum/thread/11');
                cy.contains('Autor:').should('be.visible');
                cy.contains('Jan Kowalski').should('be.visible');
                cy.contains('2 odp.').should('be.visible');
                cy.contains('35 wyśw.').should('be.visible');
            });

            getThreadRow('Pierwszy trening młodych gołębi').should('be.visible').within(() => {
                cy.contains('a', 'Pierwszy trening młodych gołębi').should('have.attr', 'href', '/forum/thread/12');
                cy.contains('Anna Nowak').should('be.visible');
                cy.contains('5 odp.').should('be.visible');
                cy.contains('84 wyśw.').should('be.visible');
            });

            cy.contains('button', 'Poprzednia').should('not.exist');
            cy.contains('button', 'Następna').should('not.exist');

            cy.checkFooter();
        });

        // ==========================================
        // NEW THREAD MODAL
        // ==========================================

        it('Should correctly render the new thread modal and text editor controls', () => {
            cy.contains('button', '+ Nowy wątek').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Utwórz nowy wątek').should('be.visible');
                cy.contains('label', 'Tytuł wątku').should('be.visible');
                cy.get('#thread-title').should('be.visible').and('have.attr', 'minlength', '5').and('have.attr', 'maxlength', '150').and('have.attr', 'placeholder', 'Jasno opisz swój problem lub myśl...');
                cy.contains('Treść pierwszej wiadomości').should('be.visible');
                cy.get('[role="textbox"][aria-label="Treść pierwszej wiadomości"]').should('be.visible');

                cy.get('button[aria-label="Cofnij"]').should('be.visible');
                cy.get('button[aria-label="Ponów"]').should('be.visible');
                cy.get('button[aria-label="Pogrubienie"]').should('be.visible');
                cy.get('button[aria-label="Kursywa"]').should('be.visible');
                cy.get('button[aria-label="Podkreślenie"]').should('be.visible');
                cy.get('button[aria-label="Przekreślenie"]').should('be.visible');
                cy.get('button[aria-label="Kolor zaznaczenia"]').should('be.visible');
                cy.get('button[aria-label="Lista punktowana"]').should('be.visible');
                cy.get('button[aria-label="Lista numerowana"]').should('be.visible');
                cy.get('button[aria-label="Wyrównanie tekstu"]').should('be.visible');

                cy.contains('button', 'Anuluj').should('be.visible');
                cy.contains('button', 'Opublikuj wątek').should('be.visible').and('not.be.disabled');
            });
        });

        it('Should close the new thread modal without creating a thread', () => {
            cy.contains('button', '+ Nowy wątek').click();
            cy.get('#thread-title').type('Testowy wątek');
            cy.contains('button', 'Anuluj').click();

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('a', 'Testowy wątek').should('not.exist');
        });

        it('Should validate a thread title after trimming whitespace', () => {
            cy.contains('button', '+ Nowy wątek').click();
            cy.get('#thread-title').type('     ');
            cy.contains('button', 'Opublikuj wątek').click();

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Tytuł wątku musi mieć od 5 do 150 znaków.');
        });

        it('Should require content for the first thread message', () => {
            cy.contains('button', '+ Nowy wątek').click();
            cy.get('#thread-title').type('Testowy wątek');
            cy.contains('button', 'Opublikuj wątek').click();

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Treść pierwszej wiadomości nie może być pusta.');
        });

        it('Should successfully create a new thread', () => {
            cy.intercept('POST', '**/api/forum/threads', (req) => {
                expect(req.body.categoryId).to.eq(1);
                expect(req.body.title).to.eq('Nowy temat testowy');
                expect(req.body.initialPostContent).to.contain('Pierwsza wiadomość testowego wątku.');

                threadsPage.content.unshift({
                    id: 13,
                    categoryId: 1,
                    title: 'Nowy temat testowy',
                    authorName: 'Hodowca Testowy',
                    repliesCount: 0,
                    views: 0,
                    createdAt: '2026-09-22T10:00:00',
                    lastPostAt: '2026-09-22T10:00:00',
                    isPinned: false,
                    isLocked: false,
                    canEdit: true,
                    canDelete: true,
                    canModerate: false
                });
                threadsPage.totalElements += 1;

                req.reply({ statusCode: 201 });
            }).as('createThread');

            cy.contains('button', '+ Nowy wątek').click();
            cy.get('#thread-title').type('  Nowy temat testowy  ');
            cy.get('[role="textbox"][aria-label="Treść pierwszej wiadomości"]').click().type('Pierwsza wiadomość testowego wątku.');
            cy.contains('button', 'Opublikuj wątek').click();

            cy.wait('@createThread');
            cy.wait(['@getCategory', '@getThreads']);

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('a', 'Nowy temat testowy').should('be.visible').and('have.attr', 'href', '/forum/thread/13');
        });

        it('Should display an API error when thread creation fails', () => {
            cy.intercept('POST', '**/api/forum/threads', {
                statusCode: 500,
                body: 'Nie udało się utworzyć testowego wątku.'
            }).as('createThreadError');

            cy.contains('button', '+ Nowy wątek').click();
            cy.get('#thread-title').type('Testowy wątek');
            cy.get('[role="textbox"][aria-label="Treść pierwszej wiadomości"]').click().type('Treść wiadomości.');
            cy.contains('button', 'Opublikuj wątek').click();

            cy.wait('@createThreadError');

            cy.get('[role="dialog"]').should('be.visible');
            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nie udało się utworzyć testowego wątku.');
        });

        // ==========================================
        // EMPTY STATE
        // ==========================================

        it('Should display an empty state when the category has no threads', () => {
            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '0',
                    size: '15'
                }
            }, {
                statusCode: 200,
                body: createEmptyThreadsPage()
            }).as('getEmptyThreads');

            cy.reload();
            cy.wait('@getEmptyThreads');

            cy.contains('Brak wątków w tym dziale. Bądź pierwszy i rozpocznij dyskusję!').should('be.visible');
            cy.get('table').should('not.exist');
        });

        // ==========================================
        // PAGINATION
        // ==========================================

        it('Should navigate between thread pages', () => {
            const firstPage: MockThreadsPage = {
                ...createThreadsPage(),
                totalPages: 2,
                totalElements: 3,
                number: 0
            };

            const secondPage: MockThreadsPage = {
                content: [
                    {
                        id: 13,
                        categoryId: 1,
                        title: 'Starszy temat forum',
                        authorName: 'Piotr Nowak',
                        repliesCount: 1,
                        views: 10,
                        createdAt: '2026-08-15T10:00:00',
                        lastPostAt: '2026-08-16T10:00:00',
                        isPinned: false,
                        isLocked: false,
                        canEdit: false,
                        canDelete: false,
                        canModerate: false
                    }
                ],
                totalPages: 2,
                totalElements: 3,
                number: 1
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '0',
                    size: '15'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getFirstThreadsPage');

            cy.reload();
            cy.wait('@getFirstThreadsPage');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('button', 'Poprzednia').should('be.disabled');
            cy.contains('button', 'Następna').should('not.be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '1',
                    size: '15'
                }
            }, {
                statusCode: 200,
                body: secondPage
            }).as('getSecondThreadsPage');

            cy.contains('button', 'Następna').click();
            cy.wait('@getSecondThreadsPage');

            cy.contains('Strona 2 z 2').should('be.visible');
            cy.contains('a', 'Starszy temat forum').should('be.visible');
            cy.contains('a', 'Przygotowanie do lotu konkursowego').should('not.exist');
            cy.contains('button', 'Poprzednia').should('not.be.disabled');
            cy.contains('button', 'Następna').should('be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '0',
                    size: '15'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getFirstThreadsPageAgain');

            cy.contains('button', 'Poprzednia').click();
            cy.wait('@getFirstThreadsPageAgain');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('a', 'Przygotowanie do lotu konkursowego').should('be.visible');
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading category data', () => {
            let shouldFail = true;

            cy.intercept('GET', '**/api/forum/categories/1', (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać testowej kategorii.'
                    });
                } else {
                    req.reply({
                        statusCode: 200,
                        body: category
                    });
                }
            }).as('getCategoryRetry');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '0',
                    size: '15'
                }
            }, {
                statusCode: 200,
                body: threadsPage
            }).as('getThreadsRetry');

            cy.reload();
            cy.wait(['@getCategoryRetry', '@getThreadsRetry']);

            cy.contains('Nie udało się pobrać testowej kategorii.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait(['@getCategoryRetry', '@getThreadsRetry']);

            cy.contains('Nie udało się pobrać testowej kategorii.').should('not.exist');
            cy.get('h1').should('have.text', 'Loty i treningi');
            cy.contains('a', 'Przygotowanie do lotu konkursowego').should('be.visible');
        });
    });

    // ==========================================
    // INVALID CATEGORY ID
    // ==========================================

    describe('Invalid Category ID', () => {
        it('Should display an error for an invalid category identifier', () => {
            cy.visit('/forum/abc');

            cy.contains('Nieprawidłowy adres URL. Kategoria nie istnieje.').should('be.visible');
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept('GET', '**/api/forum/categories/1', {
                statusCode: 200,
                body: createCategory()
            });

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/categories/1/threads',
                query: {
                    page: '0',
                    size: '15'
                }
            }, {
                statusCode: 200,
                body: createEmptyThreadsPage()
            });

            cy.visit('/forum/1');

            cy.location('pathname').should('eq', '/login');
        });
    });
});