import { createFakeToken } from '../../support/testUtils';

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
    categoryName?: string;
}

interface MockForumPost {
    id: number;
    authorName: string;
    authorRole: string;
    body: string;
    createdAt: string;
    editedAt: string | null;
    canEdit: boolean;
    canDelete: boolean;
}

interface MockPostsPage {
    content: MockForumPost[];
    totalPages: number;
    totalElements: number;
    number: number;
}

describe('Forum Thread Page Tests', () => {
    const adminToken = createFakeToken('ADMINISTRATOR', 'admin@pzhgp.pl', 'Admin');
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const createThread = (): MockForumThread => ({
        id: 11,
        categoryId: 1,
        title: 'Przygotowanie do lotu konkursowego',
        authorName: 'Admin Testowy',
        repliesCount: 1,
        views: 35,
        createdAt: '2026-09-20T09:00:00',
        lastPostAt: '2026-09-21T12:30:00',
        isPinned: false,
        isLocked: false,
        canEdit: true,
        canDelete: true,
        canModerate: true,
        categoryName: 'Loty i treningi'
    });

    const createPostsPage = (): MockPostsPage => ({
        content: [
            {
                id: 101,
                authorName: 'Admin Testowy',
                authorRole: 'ADMINISTRATOR',
                body: '<p>Pierwszy wpis w wątku.</p>',
                createdAt: '2026-09-20T09:00:00',
                editedAt: null,
                canEdit: true,
                canDelete: true
            },
            {
                id: 102,
                authorName: 'Jan Kowalski',
                authorRole: 'BREEDER',
                body: '<p>Odpowiedź hodowcy na pierwszy wpis.</p>',
                createdAt: '2026-09-21T12:30:00',
                editedAt: '2026-09-21T13:00:00',
                canEdit: false,
                canDelete: true
            }
        ],
        totalPages: 1,
        totalElements: 2,
        number: 0
    });

    const getPostByContent = (content: string) => {
        return cy.contains('.prose', content).closest('div[class*="sm:flex-row"]');
    };

    describe('Administrator', () => {
        let thread: MockForumThread;
        let postsPage: MockPostsPage;

        beforeEach(() => {
            thread = createThread();
            postsPage = createPostsPage();

            cy.intercept('GET', '**/api/forum/threads/11', (req) => {
                req.reply({
                    statusCode: 200,
                    body: thread
                });
            }).as('getThread');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: postsPage
                });
            }).as('getPosts');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum/thread/11', adminToken);

            cy.wait(['@getThread', '@getPosts', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render all visible elements of the forum thread page', () => {
            cy.checkLoggedInNavbar('Admin');

            cy.get('nav').contains('a', 'Forum').should('be.visible').and('have.attr', 'href', '/forum');
            cy.get('nav').contains('a', 'Wróć do kategorii').should('be.visible').and('have.attr', 'href', '/forum/1');
            cy.get('nav').contains('Przygotowanie do lotu konkursowego').should('be.visible');

            cy.get('h1').should('be.visible').and('contain.text', 'Przygotowanie do lotu konkursowego');
            cy.contains('Rozpoczęte przez').should('be.visible');
            cy.contains('Admin Testowy').should('be.visible');
            cy.get('button[aria-label="Edytuj tytuł wątku"]').should('be.visible');
            cy.contains('button', 'Zarządzaj').should('be.visible').and('have.attr', 'aria-expanded', 'false');

            cy.contains('button', 'Zarządzaj').click();
            cy.get('[role="menu"]').should('be.visible').within(() => {
                cy.contains('[role="menuitem"]', 'Przypnij wątek').should('be.visible');
                cy.contains('[role="menuitem"]', 'Zablokuj wątek').should('be.visible');
                cy.contains('[role="menuitem"]', 'Usuń wątek').should('be.visible');
            });
            cy.contains('button', 'Zarządzaj').click();

            getPostByContent('Pierwszy wpis w wątku.').should('be.visible').within(() => {
                cy.contains('Admin Testowy').should('be.visible');
                cy.contains('Administrator').should('be.visible');
                cy.contains('Pierwszy wpis w wątku.').should('be.visible');
                cy.contains('button', 'Edytuj').should('be.visible');
                cy.contains('button', 'Usuń').should('be.visible');
            });

            getPostByContent('Odpowiedź hodowcy na pierwszy wpis.').should('be.visible').within(() => {
                cy.contains('Jan Kowalski').should('be.visible');
                cy.contains('Hodowca').should('be.visible');
                cy.contains('Odpowiedź hodowcy na pierwszy wpis.').should('be.visible');
                cy.contains('(Edytowano)').should('be.visible');
                cy.contains('button', 'Edytuj').should('not.exist');
                cy.contains('button', 'Usuń').should('be.visible');
            });

            cy.contains('button', 'Poprzednia').should('not.exist');
            cy.contains('button', 'Następna').should('not.exist');

            cy.contains('h3', 'Dodaj odpowiedź').should('be.visible');
            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').should('be.visible');
            cy.get('button[aria-label="Pogrubienie"]').should('be.visible');
            cy.get('button[aria-label="Lista punktowana"]').should('be.visible');
            cy.contains('button', 'Opublikuj odpowiedź').should('be.visible').and('not.be.disabled');

            cy.checkFooter();
        });

        // ==========================================
        // THREAD TITLE
        // ==========================================

        it('Should allow cancelling thread title editing', () => {
            cy.get('button[aria-label="Edytuj tytuł wątku"]').click();

            cy.get('input[aria-label="Tytuł wątku"]').should('be.visible').and('have.value', 'Przygotowanie do lotu konkursowego');
            cy.contains('button', 'Zapisz').should('be.visible');
            cy.contains('button', 'Anuluj').click();

            cy.get('input[aria-label="Tytuł wątku"]').should('not.exist');
            cy.get('h1').should('contain.text', 'Przygotowanie do lotu konkursowego');
        });

        it('Should successfully update the thread title', () => {
            cy.intercept('PUT', '**/api/forum/threads/11/title', (req) => {
                expect(req.body).to.deep.equal({
                    title: 'Zaktualizowany tytuł wątku'
                });

                thread = {
                    ...thread,
                    title: 'Zaktualizowany tytuł wątku'
                };

                req.reply({ statusCode: 200 });
            }).as('updateThreadTitle');

            cy.get('button[aria-label="Edytuj tytuł wątku"]').click();
            cy.get('input[aria-label="Tytuł wątku"]').clear().type('  Zaktualizowany tytuł wątku  ');
            cy.contains('button', 'Zapisz').click();

            cy.wait('@updateThreadTitle');

            cy.get('input[aria-label="Tytuł wątku"]').should('not.exist');
            cy.get('h1').should('contain.text', 'Zaktualizowany tytuł wątku');
        });

        it('Should validate an invalid thread title before sending a request', () => {
            cy.get('button[aria-label="Edytuj tytuł wątku"]').click();
            cy.get('input[aria-label="Tytuł wątku"]').clear().type('abc');
            cy.contains('button', 'Zapisz').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd walidacji').should('be.visible');
                cy.contains('Tytuł wątku musi mieć od 5 do 150 znaków.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.get('input[aria-label="Tytuł wątku"]').should('be.visible');
        });

        it('Should display an error modal when updating the thread title fails', () => {
            cy.intercept('PUT', '**/api/forum/threads/11/title', {
                statusCode: 500,
                body: 'Nie udało się zaktualizować testowego tytułu.'
            }).as('updateThreadTitleError');

            cy.get('button[aria-label="Edytuj tytuł wątku"]').click();
            cy.get('input[aria-label="Tytuł wątku"]').clear().type('Nowy tytuł testowy');
            cy.contains('button', 'Zapisz').click();

            cy.wait('@updateThreadTitleError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd edycji').should('be.visible');
                cy.contains('Nie udało się zaktualizować testowego tytułu.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.get('input[aria-label="Tytuł wątku"]').should('be.visible');
        });

        // ==========================================
        // THREAD MODERATION
        // ==========================================

        it('Should successfully pin and unpin a thread', () => {
            cy.intercept('PUT', '**/api/forum/threads/11/pin', {
                statusCode: 200
            }).as('togglePin');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Przypnij wątek').click();

            cy.wait('@togglePin');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Odepnij wątek').should('be.visible').click();

            cy.wait('@togglePin');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Przypnij wątek').should('be.visible');
        });

        it('Should successfully lock and unlock a thread', () => {
            cy.intercept('PUT', '**/api/forum/threads/11/lock', {
                statusCode: 200
            }).as('toggleLock');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Zablokuj wątek').click();

            cy.wait('@toggleLock');

            cy.contains('h3', 'Wątek zablokowany').should('be.visible');
            cy.contains('Ten wątek został zamknięty przez moderatora. Nie można dodawać nowych odpowiedzi.').should('be.visible');
            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').should('not.exist');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Odblokuj wątek').click();

            cy.wait('@toggleLock');

            cy.contains('h3', 'Wątek zablokowany').should('not.exist');
            cy.contains('h3', 'Dodaj odpowiedź').should('be.visible');
            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').should('be.visible');
        });

        it('Should display an error modal when thread moderation fails', () => {
            cy.intercept('PUT', '**/api/forum/threads/11/pin', {
                statusCode: 403,
                body: 'Brak uprawnień do moderacji tego wątku.'
            }).as('togglePinError');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Przypnij wątek').click();

            cy.wait('@togglePinError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd').should('be.visible');
                cy.contains('Brak uprawnień do moderacji tego wątku.').should('be.visible');
                cy.contains('button', 'OK').click();
            });
        });

        // ==========================================
        // REPLIES
        // ==========================================

        it('Should require non-empty reply content', () => {
            cy.contains('button', 'Opublikuj odpowiedź').click();

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Treść odpowiedzi nie może być pusta.');
        });

        it('Should successfully publish a reply and refresh the discussion', () => {
            cy.intercept('POST', '**/api/forum/threads/11/posts', (req) => {
                expect(req.body.body).to.contain('Nowa odpowiedź testowa.');

                thread = {
                    ...thread,
                    repliesCount: 2,
                    lastPostAt: '2026-09-22T10:00:00'
                };

                postsPage.content.push({
                    id: 103,
                    authorName: 'Admin Testowy',
                    authorRole: 'ADMINISTRATOR',
                    body: '<p>Nowa odpowiedź testowa.</p>',
                    createdAt: '2026-09-22T10:00:00',
                    editedAt: null,
                    canEdit: true,
                    canDelete: true
                });
                postsPage.totalElements += 1;

                req.reply({ statusCode: 201 });
            }).as('createPost');

            cy.intercept('GET', '**/api/forum/threads/11', (req) => {
                req.reply({
                    statusCode: 200,
                    body: thread
                });
            }).as('getThreadAfterReply');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: postsPage
                });
            }).as('getPostsAfterReply');

            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').click().type('Nowa odpowiedź testowa.');
            cy.contains('button', 'Opublikuj odpowiedź').click();

            cy.wait('@createPost');
            cy.wait('@getThreadAfterReply');
            cy.wait('@getThreadAfterReply');
            cy.wait('@getPostsAfterReply');

            cy.contains('.prose', 'Nowa odpowiedź testowa.').should('be.visible');
            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').should('have.text', '');
        });

        it('Should display an API error when publishing a reply fails', () => {
            cy.intercept('POST', '**/api/forum/threads/11/posts', {
                statusCode: 500,
                body: 'Nie udało się opublikować testowej odpowiedzi.'
            }).as('createPostError');

            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').click().type('Treść odpowiedzi.');
            cy.contains('button', 'Opublikuj odpowiedź').click();

            cy.wait('@createPostError');

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nie udało się opublikować testowej odpowiedzi.');
            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').should('contain.text', 'Treść odpowiedzi.');
        });

        // ==========================================
        // EDIT POST
        // ==========================================

        it('Should validate an empty post while editing', () => {
            getPostByContent('Pierwszy wpis w wątku.').within(() => {
                cy.contains('button', 'Edytuj').click();
            });

            cy.get('[role="textbox"][aria-label="Edytowana treść wpisu"]').clear();
            cy.contains('button', 'Zapisz zmiany').click();

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Treść wpisu nie może być pusta.');
        });

        it('Should successfully update an editable post', () => {
            cy.intercept('PUT', '**/api/forum/posts/101', (req) => {
                expect(req.body.body).to.contain('Zmieniona treść pierwszego wpisu.');

                postsPage.content = postsPage.content.map(post => post.id === 101 ? {
                    ...post,
                    body: '<p>Zmieniona treść pierwszego wpisu.</p>',
                    editedAt: '2026-09-22T11:00:00'
                } : post);

                req.reply({ statusCode: 200 });
            }).as('updatePost');

            cy.intercept('GET', '**/api/forum/threads/11', {
                statusCode: 200,
                body: thread
            }).as('getThreadAfterPostUpdate');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: postsPage
                });
            }).as('getPostsAfterPostUpdate');

            getPostByContent('Pierwszy wpis w wątku.').within(() => {
                cy.contains('button', 'Edytuj').click();
            });

            cy.get('[role="textbox"][aria-label="Edytowana treść wpisu"]').clear().type('Zmieniona treść pierwszego wpisu.');
            cy.contains('button', 'Zapisz zmiany').click();

            cy.wait('@updatePost');
            cy.wait(['@getThreadAfterPostUpdate', '@getPostsAfterPostUpdate']);

            cy.contains('.prose', 'Zmieniona treść pierwszego wpisu.').should('be.visible');
            cy.contains('.prose', 'Pierwszy wpis w wątku.').should('not.exist');
            cy.contains('(Edytowano)').should('be.visible');
        });

        it('Should display an API error when updating a post fails', () => {
            cy.intercept('PUT', '**/api/forum/posts/101', {
                statusCode: 500,
                body: 'Nie udało się zaktualizować testowego wpisu.'
            }).as('updatePostError');

            getPostByContent('Pierwszy wpis w wątku.').within(() => {
                cy.contains('button', 'Edytuj').click();
            });

            cy.get('[role="textbox"][aria-label="Edytowana treść wpisu"]').clear().type('Nowa treść wpisu.');
            cy.contains('button', 'Zapisz zmiany').click();

            cy.wait('@updatePostError');

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nie udało się zaktualizować testowego wpisu.');
            cy.get('[role="textbox"][aria-label="Edytowana treść wpisu"]').should('contain.text', 'Nowa treść wpisu.');
        });

        // ==========================================
        // DELETE POST
        // ==========================================

        it('Should cancel post deletion without removing the post', () => {
            getPostByContent('Odpowiedź hodowcy na pierwszy wpis.').within(() => {
                cy.contains('button', 'Usuń').click();
            });

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń wpis').should('be.visible');
                cy.contains('Czy na pewno chcesz usunąć ten wpis?').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('.prose', 'Odpowiedź hodowcy na pierwszy wpis.').should('be.visible');
        });

        it('Should successfully delete a post and refresh the discussion', () => {
            cy.intercept('DELETE', '**/api/forum/posts/102', (req) => {
                postsPage.content = postsPage.content.filter(post => post.id !== 102);
                postsPage.totalElements = postsPage.content.length;
                req.reply({ statusCode: 204 });
            }).as('deletePost');

            cy.intercept('GET', '**/api/forum/threads/11', {
                statusCode: 200,
                body: thread
            }).as('getThreadAfterPostDelete');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, (req) => {
                req.reply({
                    statusCode: 200,
                    body: postsPage
                });
            }).as('getPostsAfterPostDelete');

            getPostByContent('Odpowiedź hodowcy na pierwszy wpis.').within(() => {
                cy.contains('button', 'Usuń').click();
            });

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deletePost');
            cy.wait(['@getThreadAfterPostDelete', '@getPostsAfterPostDelete']);

            cy.contains('.prose', 'Odpowiedź hodowcy na pierwszy wpis.').should('not.exist');
            cy.contains('.prose', 'Pierwszy wpis w wątku.').should('be.visible');
        });

        it('Should display an error modal when post deletion fails', () => {
            cy.intercept('DELETE', '**/api/forum/posts/102', {
                statusCode: 400,
                body: 'Nie można usunąć testowego wpisu.'
            }).as('deletePostError');

            getPostByContent('Odpowiedź hodowcy na pierwszy wpis.').within(() => {
                cy.contains('button', 'Usuń').click();
            });

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deletePostError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Nie można usunąć').should('be.visible');
                cy.contains('Nie można usunąć testowego wpisu.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.contains('.prose', 'Odpowiedź hodowcy na pierwszy wpis.').should('be.visible');
        });

        // ==========================================
        // THREAD PAGINATION
        // ==========================================

        it('Should navigate between post pages', () => {
            const firstPage: MockPostsPage = {
                ...createPostsPage(),
                totalPages: 2,
                totalElements: 3,
                number: 0
            };

            const secondPage: MockPostsPage = {
                content: [
                    {
                        id: 103,
                        authorName: 'Piotr Nowak',
                        authorRole: 'MODERATOR',
                        body: '<p>Wpis z drugiej strony.</p>',
                        createdAt: '2026-09-22T12:00:00',
                        editedAt: null,
                        canEdit: false,
                        canDelete: true
                    }
                ],
                totalPages: 2,
                totalElements: 3,
                number: 1
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getFirstPostsPage');

            cy.reload();
            cy.wait('@getFirstPostsPage');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('button', 'Poprzednia').should('be.disabled');
            cy.contains('button', 'Następna').should('not.be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '1',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: secondPage
            }).as('getSecondPostsPage');

            cy.contains('button', 'Następna').click();
            cy.wait('@getSecondPostsPage');

            cy.contains('Strona 2 z 2').should('be.visible');
            cy.contains('.prose', 'Wpis z drugiej strony.').should('be.visible');
            cy.contains('.prose', 'Pierwszy wpis w wątku.').should('not.exist');
            cy.contains('button', 'Poprzednia').should('not.be.disabled');
            cy.contains('button', 'Następna').should('be.disabled');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: firstPage
            }).as('getFirstPostsPageAgain');

            cy.contains('button', 'Poprzednia').click();
            cy.wait('@getFirstPostsPageAgain');

            cy.contains('Strona 1 z 2').should('be.visible');
            cy.contains('.prose', 'Pierwszy wpis w wątku.').should('be.visible');
        });

        // ==========================================
        // SANITIZATION
        // ==========================================

        it('Should sanitize dangerous HTML before rendering a post', () => {
            const unsafePostsPage: MockPostsPage = {
                content: [
                    {
                        id: 201,
                        authorName: 'Test User',
                        authorRole: 'BREEDER',
                        body: '<p>Bezpieczna treść</p><script>window.__forumXss = true</script><img src="x" onerror="window.__forumXss = true">',
                        createdAt: '2026-09-22T12:00:00',
                        editedAt: null,
                        canEdit: false,
                        canDelete: false
                    }
                ],
                totalPages: 1,
                totalElements: 1,
                number: 0
            };

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: unsafePostsPage
            }).as('getUnsafePost');

            cy.reload();
            cy.wait('@getUnsafePost');

            cy.contains('.prose', 'Bezpieczna treść').should('be.visible');
            cy.get('.prose script').should('not.exist');
            cy.get('.prose img').should('have.length', 1).and('not.have.attr', 'onerror');
            cy.window().then((win) => {
                expect((win as Window & { __forumXss?: boolean }).__forumXss).to.be.undefined;
            });
        });

        // ==========================================
        // LOADING ERROR AND RETRY
        // ==========================================

        it('Should display an error and successfully retry loading the discussion', () => {
            let shouldFail = true;

            cy.intercept('GET', '**/api/forum/threads/11', (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Nie udało się pobrać testowego wątku.'
                    });
                } else {
                    req.reply({
                        statusCode: 200,
                        body: thread
                    });
                }
            }).as('getThreadRetry');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: postsPage
            }).as('getPostsRetry');

            cy.reload();
            cy.wait(['@getThreadRetry', '@getPostsRetry']);

            cy.contains('Nie udało się pobrać testowego wątku.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait(['@getThreadRetry', '@getPostsRetry']);

            cy.contains('Nie udało się pobrać testowego wątku.').should('not.exist');
            cy.get('h1').should('contain.text', 'Przygotowanie do lotu konkursowego');
            cy.contains('.prose', 'Pierwszy wpis w wątku.').should('be.visible');
        });

        // ==========================================
        // DELETE THREAD
        // ==========================================

        it('Should cancel thread deletion without removing the thread', () => {
            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Usuń wątek').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń wątek').should('be.visible');
                cy.contains('Czy na pewno chcesz usunąć ten wątek wraz ze wszystkimi odpowiedziami?').should('be.visible');
                cy.contains('Operacja jest nieodwracalna.').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.get('h1').should('contain.text', 'Przygotowanie do lotu konkursowego');
        });

        it('Should successfully delete a thread and return to its category', () => {
            cy.intercept('DELETE', '**/api/forum/threads/11', {
                statusCode: 204
            }).as('deleteThread');

            cy.intercept('GET', '**/api/forum/categories/1', {
                statusCode: 200,
                body: {
                    id: 1,
                    name: 'Loty i treningi',
                    description: 'Dyskusje o lotach.',
                    sortOrder: 1,
                    createdAt: '2026-09-20T10:00:00',
                    canEdit: false,
                    canDelete: false
                }
            }).as('getCategoryAfterThreadDelete');

            cy.intercept('GET', '**/api/forum/categories/1/threads?page=0&size=15', {
                statusCode: 200,
                body: {
                    content: [],
                    totalPages: 0,
                    totalElements: 0,
                    number: 0
                }
            }).as('getThreadsAfterThreadDelete');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Usuń wątek').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteThread');

            cy.location('pathname').should('eq', '/forum/1');
            cy.wait(['@getCategoryAfterThreadDelete', '@getThreadsAfterThreadDelete']);
        });

        it('Should display an error modal when thread deletion fails', () => {
            cy.intercept('DELETE', '**/api/forum/threads/11', {
                statusCode: 500,
                body: 'Nie udało się usunąć testowego wątku.'
            }).as('deleteThreadError');

            cy.contains('button', 'Zarządzaj').click();
            cy.contains('[role="menuitem"]', 'Usuń wątek').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteThreadError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd').should('be.visible');
                cy.contains('Nie udało się usunąć testowego wątku.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.get('h1').should('contain.text', 'Przygotowanie do lotu konkursowego');
        });
    });

    // ==========================================
    // LOCKED THREAD
    // ==========================================

    describe('Locked Thread', () => {
        it('Should hide the reply form when the thread is locked', () => {
            const lockedThread = {
                ...createThread(),
                isLocked: true
            };

            cy.intercept('GET', '**/api/forum/threads/11', {
                statusCode: 200,
                body: lockedThread
            }).as('getLockedThread');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: createPostsPage()
            }).as('getLockedThreadPosts');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum/thread/11', adminToken);

            cy.wait(['@getLockedThread', '@getLockedThreadPosts', '@getNotifications', '@getUnreadCount']);

            cy.contains('h3', 'Wątek zablokowany').should('be.visible');
            cy.contains('Ten wątek został zamknięty przez moderatora. Nie można dodawać nowych odpowiedzi.').should('be.visible');
            cy.get('[role="textbox"][aria-label="Treść odpowiedzi"]').should('not.exist');
            cy.contains('button', 'Opublikuj odpowiedź').should('not.exist');
        });
    });

    // ==========================================
    // LIMITED PERMISSIONS
    // ==========================================

    describe('Limited Permissions', () => {
        it('Should hide management and edit controls when the user has no permissions', () => {
            const readOnlyThread: MockForumThread = {
                ...createThread(),
                authorName: 'Inny Hodowca',
                canEdit: false,
                canDelete: false,
                canModerate: false
            };

            const readOnlyPosts: MockPostsPage = {
                content: createPostsPage().content.map(post => ({
                    ...post,
                    canEdit: false,
                    canDelete: false
                })),
                totalPages: 1,
                totalElements: 2,
                number: 0
            };

            cy.intercept('GET', '**/api/forum/threads/11', {
                statusCode: 200,
                body: readOnlyThread
            }).as('getReadOnlyThread');

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: readOnlyPosts
            }).as('getReadOnlyPosts');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum/thread/11', breederToken);

            cy.wait(['@getReadOnlyThread', '@getReadOnlyPosts', '@getNotifications', '@getUnreadCount']);

            cy.checkLoggedInNavbar('Hodowca');
            cy.get('button[aria-label="Edytuj tytuł wątku"]').should('not.exist');
            cy.contains('button', 'Zarządzaj').should('not.exist');
            cy.contains('button', 'Edytuj').should('not.exist');
            cy.contains('button', 'Usuń').should('not.exist');
            cy.contains('h3', 'Dodaj odpowiedź').should('be.visible');
        });
    });

    // ==========================================
    // INVALID THREAD ID
    // ==========================================

    describe('Invalid Thread ID', () => {
        it('Should display an error for an invalid thread identifier', () => {
            cy.visit('/forum/thread/abc');

            cy.contains('Nieprawidłowy adres URL. Wątek nie istnieje.').should('be.visible');
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept('GET', '**/api/forum/threads/11', {
                statusCode: 200,
                body: createThread()
            });

            cy.intercept({
                method: 'GET',
                pathname: '/api/forum/threads/11/posts',
                query: {
                    page: '0',
                    size: '20'
                }
            }, {
                statusCode: 200,
                body: createPostsPage()
            });

            cy.visit('/forum/thread/11');

            cy.location('pathname').should('eq', '/login');
        });
    });
});