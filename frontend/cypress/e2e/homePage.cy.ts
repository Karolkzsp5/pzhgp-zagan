import { createFakeTokenForUser, TEST_USERS } from '../support/testUtils';

describe('Home Page - Announcements Tests', () => {
    const adminToken = createFakeTokenForUser(TEST_USERS.admin);
    const moderatorToken = createFakeTokenForUser(TEST_USERS.moderator);
    const breederToken = createFakeTokenForUser(TEST_USERS.breeder);

    const createFirstPage = () => ({
        content: [
            {
                id: 1,
                title: 'Ważne ogłoszenie oddziału',
                content: '<p>Ważna przypięta treść ogłoszenia.</p>',
                authorName: 'Admin Testowy',
                isPinned: true,
                createdAt: '2026-09-25T10:00:00',
                updatedAt: '2026-09-26T12:30:00',
                canEdit: true,
                canDelete: true
            },
            {
                id: 2,
                title: 'Ogłoszenie moderatora',
                content: '<p>Treść ogłoszenia dodanego przez moderatora.</p>',
                authorName: 'Moderator Testowy',
                isPinned: false,
                createdAt: '2026-09-20T10:00:00',
                updatedAt: null,
                canEdit: false,
                canDelete: true
            },
            {
                id: 3,
                title: 'Ogłoszenie innego administratora',
                content: '<p>Treść ogłoszenia innego administratora.</p>',
                authorName: 'Inny Administrator',
                isPinned: false,
                createdAt: '2026-09-15T10:00:00',
                updatedAt: null,
                canEdit: false,
                canDelete: false
            }
        ],
        totalElements: 3,
        totalPages: 1,
        number: 0,
        size: 10
    });

    const createSecondPage = () => ({
        content: [
            {
                id: 4,
                title: 'Starsze ogłoszenie',
                content: '<p>Treść starszego ogłoszenia.</p>',
                authorName: 'Moderator Testowy',
                isPinned: false,
                createdAt: '2026-08-01T10:00:00',
                updatedAt: null,
                canEdit: false,
                canDelete: true
            }
        ],
        totalElements: 4,
        totalPages: 2,
        number: 1,
        size: 10
    });

    let firstPage = createFirstPage();
    let secondPage = createSecondPage();
    let announcementsShouldFail = false;

    const getAnnouncementByTitle = (title: string) => {
        return cy.contains('article h3', title).closest('article');
    };

    const getAnnouncementEditor = () => {
        return cy.get('[role="textbox"][aria-label="Treść ogłoszenia"]');
    };

    const openAddAnnouncementModal = () => {
        cy.contains('button', '+ Dodaj ogłoszenie').click();

        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Dodaj nowe ogłoszenie').should('be.visible');
        });
    };

    beforeEach(() => {
        firstPage = createFirstPage();
        secondPage = createSecondPage();
        announcementsShouldFail = false;

        cy.viewport(1280, 900);

        // ==========================
        // Announcements
        // ==========================

        cy.intercept('GET', '**/api/announcements*', (req) => {
            if (announcementsShouldFail) {
                req.reply({
                    statusCode: 500,
                    body: 'Internal Server Error'
                });

                return;
            }

            const url = new URL(req.url);
            const page = Number(url.searchParams.get('page') ?? '0');

            req.reply({
                statusCode: 200,
                body: page === 1 ? secondPage : firstPage
            });
        }).as('getAnnouncements');

        cy.mockNavbarNotifications();
        cy.visitWithToken('/', adminToken);

        cy.wait(['@getAnnouncements', '@getNotifications', '@getUnreadCount']);
    });

    // ==========================================
    // PAGE RENDERING
    // ==========================================

    it('Should correctly render all visible elements of the home page and announcements section', () => {
        // ==========================
        // Navbar
        // ==========================

        cy.checkLoggedInNavbar('Admin');

        // ==========================
        // Page header
        // ==========================

        cy.get('h1').should('be.visible').and('have.text', 'Oddział 0369 Żagań');
        cy.contains('Oficjalny portal Polskiego Związku Hodowców Gołębi Pocztowych.').should('be.visible');

        // ==========================
        // Announcements header
        // ==========================

        cy.contains('h2', 'Najnowsze ogłoszenia').should('be.visible');
        cy.contains('button', '+ Dodaj ogłoszenie').should('be.visible').and('not.be.disabled');

        // ==========================
        // Announcement cards
        // ==========================

        cy.get('article').should('have.length', 3).and('be.visible');

        // Own pinned announcement
        getAnnouncementByTitle('Ważne ogłoszenie oddziału').should('be.visible').within(() => {
            cy.contains('h3', 'Ważne ogłoszenie oddziału').should('be.visible');
            cy.contains('Ważna przypięta treść ogłoszenia.').should('be.visible');
            cy.contains('25.09.2026').should('be.visible');
            cy.contains('Dodał:').should('be.visible');
            cy.contains('Admin Testowy').should('be.visible');
            cy.contains('Edytowano:').should('be.visible').and('contain.text', '26.09.2026');
            cy.get('svg').should('be.visible');
            cy.contains('button', 'Edytuj').should('be.visible');
            cy.contains('button', 'Usuń').should('be.visible');
        });

        // Announcement created by moderator
        getAnnouncementByTitle('Ogłoszenie moderatora').should('be.visible').within(() => {
            cy.contains('Treść ogłoszenia dodanego przez moderatora.').should('be.visible');
            cy.contains('Moderator Testowy').should('be.visible');
            cy.contains('button', 'Edytuj').should('not.exist');
            cy.contains('button', 'Usuń').should('be.visible');
        });

        // Announcement created by another administrator
        getAnnouncementByTitle('Ogłoszenie innego administratora').should('be.visible').within(() => {
            cy.contains('Treść ogłoszenia innego administratora.').should('be.visible');
            cy.contains('Inny Administrator').should('be.visible');
            cy.get('button').should('not.exist');
        });

        // ==========================
        // Footer
        // ==========================

        cy.checkFooter();
    });

    // ==========================================
    // PERMISSIONS
    // ==========================================

    it('Should display edit and delete actions according to announcement permissions', () => {
        getAnnouncementByTitle('Ważne ogłoszenie oddziału').within(() => {
            cy.contains('button', 'Edytuj').should('be.visible');
            cy.contains('button', 'Usuń').should('be.visible');
        });

        getAnnouncementByTitle('Ogłoszenie moderatora').within(() => {
            cy.contains('button', 'Edytuj').should('not.exist');
            cy.contains('button', 'Usuń').should('be.visible');
        });

        getAnnouncementByTitle('Ogłoszenie innego administratora').within(() => {
            cy.contains('button', 'Edytuj').should('not.exist');
            cy.contains('button', 'Usuń').should('not.exist');
        });
    });

    // ==========================================
    // EMPTY STATE
    // ==========================================

    it('Should display an empty state when there are no announcements', () => {
        firstPage = {
            ...firstPage,
            content: [],
            totalElements: 0,
            totalPages: 0
        };

        cy.reload();
        cy.wait('@getAnnouncements');

        cy.contains('Brak aktualnych ogłoszeń.').should('be.visible');
        cy.get('article').should('not.exist');
        cy.contains('button', 'Poprzednia').should('not.exist');
        cy.contains('button', 'Następna').should('not.exist');
    });

    // ==========================================
    // LOADING ERROR AND RETRY
    // ==========================================

    it('Should display an error and successfully retry loading announcements', () => {
        cy.then(() => {
            announcementsShouldFail = true;
        });

        cy.reload();
        cy.wait('@getAnnouncements');
        cy.contains('Nie udało się pobrać ogłoszeń.').should('be.visible');
        cy.contains('button', 'Spróbuj ponownie').should('be.visible');
        cy.get('article').should('not.exist');

        cy.then(() => {
            announcementsShouldFail = false;
        });

        cy.contains('button', 'Spróbuj ponownie').click();
        cy.wait('@getAnnouncements');
        getAnnouncementByTitle('Ważne ogłoszenie oddziału').should('be.visible');
        cy.contains('Nie udało się pobrać ogłoszeń.').should('not.exist');
    });

    // ==========================================
    // PAGINATION
    // ==========================================

    it('Should correctly navigate between announcement pages', () => {
        firstPage = {
            ...firstPage,
            totalElements: 4,
            totalPages: 2
        };

        secondPage = {
            ...secondPage,
            totalElements: 4,
            totalPages: 2
        };

        cy.reload();
        cy.wait('@getAnnouncements');
        cy.contains('Strona 1 z 2').should('be.visible');
        cy.contains('button', 'Poprzednia').should('be.visible').and('be.disabled');

        cy.intercept(
            {
                method: 'GET',
                pathname: '/api/announcements',
                query: {
                    page: '1',
                    size: '10'
                }
            },
            {
                statusCode: 200,
                body: secondPage
            }
        ).as('getAnnouncementsPage1');

        cy.contains('button', 'Następna').should('be.visible').and('not.be.disabled').click();
        cy.wait('@getAnnouncementsPage1');

        cy.contains('Strona 2 z 2').should('be.visible');
        getAnnouncementByTitle('Starsze ogłoszenie').should('be.visible');
        cy.contains('button', 'Poprzednia').should('not.be.disabled');
        cy.contains('button', 'Następna').should('be.disabled');

        cy.intercept(
            {
                method: 'GET',
                pathname: '/api/announcements',
                query: {
                    page: '0',
                    size: '10'
                }
            },
            {
                statusCode: 200,
                body: firstPage
            }
        ).as('getAnnouncementsPage0');

        cy.contains('button', 'Poprzednia').click();
        cy.wait('@getAnnouncementsPage0');
        cy.contains('Strona 1 z 2').should('be.visible');
        getAnnouncementByTitle('Ważne ogłoszenie oddziału').should('be.visible');
    });

    // ==========================================
    // HTML SANITIZATION
    // ==========================================

    it('Should sanitize unsafe HTML before displaying announcement content', () => {
        firstPage = {
            ...firstPage,
            content: firstPage.content.map((announcement) =>
                announcement.id === 1
                    ? {
                        ...announcement,
                        content:
                            '<p>Bezpieczna treść.</p>' +
                            '<script>window.__announcementXss = true;</script>'
                    }
                    : announcement
            )
        };

        cy.reload();
        cy.wait('@getAnnouncements');

        getAnnouncementByTitle('Ważne ogłoszenie oddziału').within(() => {
            cy.contains('Bezpieczna treść.').should('be.visible');
            cy.get('script').should('not.exist');
        });

        cy.window().then((win) => {
            expect((win as Window & { __announcementXss?: boolean }).__announcementXss).to.be.undefined;
        });
    });

    // ==========================================
    // ADD ANNOUNCEMENT MODAL
    // ==========================================

    it('Should correctly open and close the add announcement modal', () => {
        openAddAnnouncementModal();

        cy.get('[role="dialog"]').within(() => {
            cy.contains('label', 'Tytuł ogłoszenia').should('be.visible');
            cy.get('#announcement-title').should('be.visible').and('have.attr', 'placeholder', 'Wpisz tytuł (min. 3 znaki)')
                .and('have.attr', 'required');

            cy.contains('label', 'Treść ogłoszenia').should('be.visible');
            getAnnouncementEditor().should('be.visible').and('have.attr', 'contenteditable', 'true');
            cy.get('#isPinned').should('be.visible').and('not.be.checked');
            cy.contains('label', 'Przypnij ogłoszenie').should('be.visible');

            cy.contains('button', 'Anuluj').should('be.visible');
            cy.contains('button', 'Opublikuj').should('be.visible').and('not.be.disabled');
            cy.contains('button', 'Anuluj').click();
        });

        cy.get('[role="dialog"]').should('not.exist');
    });

    // ==========================================
    // ADD ANNOUNCEMENT VALIDATION
    // ==========================================

    it('Should validate announcement title and content before submitting', () => {
        openAddAnnouncementModal();
        cy.get('[role="dialog"] form').invoke('attr', 'novalidate', '');

        // Title too short
        cy.get('#announcement-title').type('AB');
        cy.contains('button', 'Opublikuj').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Tytuł musi mieć od 3 do 150 znaków.');

        // Empty announcement content
        cy.get('#announcement-title').clear().type('Poprawny tytuł ogłoszenia');
        cy.contains('button', 'Opublikuj').click();
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Treść ogłoszenia nie może być pusta.');
    });

    // ==========================================
    // CREATE ANNOUNCEMENT
    // ==========================================

    it('Should successfully create a new pinned announcement', () => {
        cy.intercept('POST', '**/api/announcements', (req) => {
            const body = req.body as {
                title: string;
                content: string;
                isPinned: boolean;
            };

            firstPage = {
                ...firstPage,
                totalElements: firstPage.totalElements + 1,
                content: [
                    {
                        id: 99,
                        title: body.title,
                        content: body.content,
                        authorName: 'Admin Testowy',
                        isPinned: body.isPinned,
                        createdAt: '2026-09-28T18:00:00',
                        updatedAt: null,
                        canEdit: true,
                        canDelete: true
                    },
                    ...firstPage.content
                ]
            };

            req.reply({
                statusCode: 201,
                body: ''
            });
        }).as('createAnnouncement');

        openAddAnnouncementModal();

        cy.get('#announcement-title').type('Nowe ogłoszenie Cypress');
        getAnnouncementEditor().click().type('Treść nowego ogłoszenia testowego.');
        cy.get('#isPinned').check();
        cy.contains('button', 'Opublikuj').click();

        cy.wait('@createAnnouncement').then((interception) => {
            expect(interception.request.body.title).to.eq('Nowe ogłoszenie Cypress');
            expect(interception.request.body.content).to.contain('Treść nowego ogłoszenia testowego.');
            expect(interception.request.body.isPinned).to.eq(true);
        });

        cy.wait('@getAnnouncements');
        cy.get('[role="dialog"]').should('not.exist');

        getAnnouncementByTitle('Nowe ogłoszenie Cypress').should('be.visible').within(() => {
            cy.contains('Treść nowego ogłoszenia testowego.').should('be.visible');
            cy.contains('Admin Testowy').should('be.visible');
            cy.get('svg').should('be.visible');
            cy.contains('button', 'Edytuj').should('be.visible');
            cy.contains('button', 'Usuń').should('be.visible');
        });
    });

    // ==========================================
    // CREATE ANNOUNCEMENT ERROR
    // ==========================================

    it('Should display an API error when creating an announcement fails', () => {
        cy.intercept('POST', '**/api/announcements', {
            statusCode: 400,
            body: 'Nie udało się zapisać ogłoszenia testowego.'
        }).as('createAnnouncementError');

        openAddAnnouncementModal();

        cy.get('#announcement-title').type('Testowe ogłoszenie');
        getAnnouncementEditor().click().type('Poprawna treść testowego ogłoszenia.');
        cy.contains('button', 'Opublikuj').click();

        cy.wait('@createAnnouncementError');
        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nie udało się zapisać ogłoszenia testowego.');
        cy.get('[role="dialog"]').should('be.visible');
    });

    // ==========================================
    // EDIT ANNOUNCEMENT
    // ==========================================

    it('Should successfully edit an existing announcement', () => {
        cy.intercept('PUT', '**/api/announcements/1', (req) => {
            const body = req.body as {
                title: string;
                content: string;
                isPinned: boolean;
            };

            firstPage = {
                ...firstPage,
                content: firstPage.content.map((announcement) =>
                    announcement.id === 1
                        ? {
                            ...announcement,
                            title: body.title,
                            content: body.content,
                            isPinned: body.isPinned,
                            updatedAt: '2026-09-28T19:00:00'
                        }
                        : announcement
                )
            };

            req.reply({
                statusCode: 200,
                body: ''
            });
        }).as('updateAnnouncement');

        getAnnouncementByTitle('Ważne ogłoszenie oddziału').within(() => {cy.contains('button', 'Edytuj').click();});

        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Edytuj ogłoszenie').should('be.visible');
        });

        cy.get('#announcement-title').should('have.value', 'Ważne ogłoszenie oddziału').clear().type('Zmienione ogłoszenie');

        getAnnouncementEditor().should('contain.text', 'Ważna przypięta treść ogłoszenia.').click()
            .type('{selectall}{backspace}' + 'Zmieniona treść ogłoszenia.');

        cy.get('#isPinned').should('be.checked').uncheck();
        cy.contains('button', 'Zapisz zmiany').click();

        cy.wait('@updateAnnouncement').then((interception) => {
            expect(interception.request.body.title).to.eq('Zmienione ogłoszenie');
            expect(interception.request.body.content).to.contain('Zmieniona treść ogłoszenia.');
            expect(interception.request.body.isPinned).to.eq(false);
        });

        cy.wait('@getAnnouncements');
        cy.get('[role="dialog"]').should('not.exist');
        cy.contains('article h3', 'Ważne ogłoszenie oddziału').should('not.exist');

        getAnnouncementByTitle('Zmienione ogłoszenie').should('be.visible').within(() => {
            cy.contains('Zmieniona treść ogłoszenia.').should('be.visible');
            cy.contains('Edytowano:').should('be.visible').and('contain.text', '28.09.2026');
            cy.get('svg').should('not.exist');
        });
    });

    // ==========================================
    // CANCEL DELETE
    // ==========================================

    it('Should cancel announcement deletion without removing the announcement', () => {
        getAnnouncementByTitle('Ogłoszenie moderatora').within(() => {
            cy.contains('button', 'Usuń').click();
        });

        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Usuń ogłoszenie').should('be.visible');
            cy.contains('Czy na pewno chcesz trwale usunąć to ogłoszenie?').should('be.visible');
            cy.contains('button', 'Anuluj').click();
        });

        cy.get('[role="dialog"]').should('not.exist');
        getAnnouncementByTitle('Ogłoszenie moderatora').should('be.visible');
    });

    // ==========================================
    // DELETE ANNOUNCEMENT
    // ==========================================

    it('Should successfully delete an announcement', () => {
        cy.intercept('DELETE', '**/api/announcements/2', (req) => {
            firstPage = {
                ...firstPage,
                totalElements: firstPage.totalElements - 1,
                content: firstPage.content.filter(
                    (announcement) => announcement.id !== 2
                )
            };

            req.reply({
                statusCode: 204
            });
        }).as('deleteAnnouncement');

        getAnnouncementByTitle('Ogłoszenie moderatora').within(() => {
            cy.contains('button', 'Usuń').click();
        });

        cy.get('[role="dialog"]').within(() => {
            cy.contains('h2', 'Usuń ogłoszenie').should('be.visible');
            cy.contains('button', 'Usuń').click();
        });

        cy.wait('@deleteAnnouncement');
        cy.wait('@getAnnouncements');

        cy.get('[role="dialog"]').should('not.exist');
        cy.contains('article h3', 'Ogłoszenie moderatora').should('not.exist');
        getAnnouncementByTitle('Ważne ogłoszenie oddziału').should('be.visible');
        getAnnouncementByTitle('Ogłoszenie innego administratora').should('be.visible');
    });

    // ==========================================
    // DELETE ERROR
    // ==========================================

    it('Should display an alert when announcement deletion fails', () => {
        cy.intercept('DELETE', '**/api/announcements/2', {
            statusCode: 500,
            body: 'Internal Server Error'
        }).as('deleteAnnouncementError');

        cy.window().then((win) => {
            cy.stub(win, 'alert').as('windowAlert');
        });

        getAnnouncementByTitle('Ogłoszenie moderatora').within(() => {
            cy.contains('button', 'Usuń').click();
        });

        cy.get('[role="dialog"]').within(() => {
            cy.contains('button', 'Usuń').click();
        });

        cy.wait('@deleteAnnouncementError');
        cy.get('@windowAlert').should('have.been.calledWith', 'Wystąpił błąd podczas usuwania ogłoszenia.');
        getAnnouncementByTitle('Ogłoszenie moderatora').should('be.visible');
        cy.get('[role="dialog"]').should('be.visible');
    });

    // ==========================================
    // DELETE LAST ITEM ON PAGE
    // ==========================================

    it('Should return to the previous page after deleting the last announcement on the current page', () => {
        firstPage = {
            ...firstPage,
            totalElements: 4,
            totalPages: 2
        };

        secondPage = {
            ...secondPage,
            totalElements: 4,
            totalPages: 2
        };

        cy.reload();
        cy.wait('@getAnnouncements');

        cy.contains('button', 'Następna').click();
        cy.wait('@getAnnouncements');
        cy.contains('Strona 2 z 2').should('be.visible');
        getAnnouncementByTitle('Starsze ogłoszenie').should('be.visible');

        cy.intercept('DELETE', '**/api/announcements/4', (req) => {
                firstPage = {
                    ...firstPage,
                    totalElements: 3,
                    totalPages: 1
                };

                secondPage = {
                    ...secondPage,
                    content: [],
                    totalElements: 3,
                    totalPages: 1
                };

                req.reply({
                    statusCode: 204
                });
            }
        ).as('deleteLastAnnouncement');

        getAnnouncementByTitle('Starsze ogłoszenie').within(() => {
            cy.contains('button', 'Usuń').click();
        });

        cy.get('[role="dialog"]').within(() => {
            cy.contains('button', 'Usuń').click();
        });

        cy.wait('@deleteLastAnnouncement');
        cy.wait('@getAnnouncements').its('request.url').should('include', 'page=0');
        getAnnouncementByTitle('Ważne ogłoszenie oddziału').should('be.visible');

        cy.contains('Strona 2 z 2').should('not.exist');
        cy.contains('button', 'Następna').should('not.exist');
        cy.contains('button', 'Poprzednia').should('not.exist');
    });

    // ==========================================
    // PUBLIC USER
    // ==========================================

    it('Should hide announcement management actions from an unauthenticated user', () => {
        firstPage = {
            ...firstPage,
            content: firstPage.content.map((announcement) => ({
                ...announcement,
                canEdit: false,
                canDelete: false
            }))
        };

        cy.window().then((win) => {
            win.localStorage.removeItem('jwt_token');
            win.sessionStorage.removeItem('jwt_token');

            expect(win.localStorage.getItem('jwt_token')).to.be.null;
            expect(win.sessionStorage.getItem('jwt_token')).to.be.null;
        });

        cy.intercept('GET', '**/api/announcements*', (req) => {
            expect(req.headers.authorization).to.be.undefined;
            req.reply({
                statusCode: 200,
                body: firstPage
            });
        }).as('getPublicAnnouncements');

        cy.reload();
        cy.wait('@getPublicAnnouncements');
        cy.contains('button', '+ Dodaj ogłoszenie').should('not.exist');
        cy.get('article button').should('not.exist');
        cy.contains('a', 'Zaloguj się').should('be.visible');
    });

    // ==========================================
    // BREEDER
    // ==========================================

    it('Should prevent a breeder from seeing the add announcement button', () => {
        firstPage = {
            ...firstPage,
            content: firstPage.content.map((announcement) => ({
                ...announcement,
                canEdit: false,
                canDelete: false
            }))
        };

        cy.window().then((win) => {
            win.localStorage.setItem('jwt_token', breederToken);
            win.sessionStorage.removeItem('jwt_token');
        });

        cy.reload();
        cy.wait('@getAnnouncements');

        cy.contains('button', '+ Dodaj ogłoszenie').should('not.exist');
        cy.contains('Witaj,').should('be.visible');
        cy.contains('strong', 'Hodowca').should('be.visible');
    });

    // ==========================================
    // MODERATOR
    // ==========================================

    it('Should allow a moderator to see the add announcement button', () => {
        cy.window().then((win) => {
            win.localStorage.setItem('jwt_token', moderatorToken);
            win.sessionStorage.removeItem('jwt_token');
        });

        cy.reload();
        cy.wait('@getAnnouncements');

        cy.contains('button', '+ Dodaj ogłoszenie').should('be.visible').and('not.be.disabled');
        cy.contains('Witaj,').should('be.visible');
        cy.contains('strong', 'Moderator').should('be.visible');
    });
});