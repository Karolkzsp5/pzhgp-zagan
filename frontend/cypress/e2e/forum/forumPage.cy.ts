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

describe('Forum Page Tests', () => {
    const adminToken = createFakeToken('ADMINISTRATOR', 'admin@pzhgp.pl', 'Admin');
    const moderatorToken = createFakeToken('MODERATOR', 'moderator@pzhgp.pl', 'Moderator');
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const createCategories = (): MockForumCategory[] => [
        {
            id: 1,
            name: 'Loty i treningi',
            description: 'Dyskusje o lotach, treningach i przygotowaniu gołębi.',
            sortOrder: 1,
            createdAt: '2026-09-20T10:00:00',
            canEdit: true,
            canDelete: true
        },
        {
            id: 2,
            name: 'Zdrowie gołębi',
            description: 'Profilaktyka, żywienie i zdrowie stada.',
            sortOrder: 2,
            createdAt: '2026-09-19T10:00:00',
            canEdit: false,
            canDelete: false
        }
    ];

    const getCategoryCard = (name: string) => {
        return cy.contains('h2', name).closest('article');
    };

    describe('Administrator', () => {
        let categories: MockForumCategory[];

        beforeEach(() => {
            categories = createCategories();

            cy.intercept('GET', '**/api/forum/categories', (req) => {
                req.reply({
                    statusCode: 200,
                    body: categories
                });
            }).as('getCategories');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum', adminToken);

            cy.wait(['@getCategories', '@getNotifications', '@getUnreadCount']);
        });

        // ==========================================
        // PAGE RENDERING
        // ==========================================

        it('Should correctly render all visible elements of the forum page', () => {
            cy.checkLoggedInNavbar('Admin');

            cy.get('h1').should('be.visible').and('have.text', 'Forum Hodowców');
            cy.contains('Wybierz kategorię, aby przeglądać tematy lub rozpocząć nową dyskusję.').should('be.visible');
            cy.contains('button', '+ Dodaj kategorię').should('be.visible');

            getCategoryCard('Loty i treningi').should('be.visible').within(() => {
                cy.contains('a', 'Loty i treningi').should('have.attr', 'href', '/forum/1');
                cy.contains('Dyskusje o lotach, treningach i przygotowaniu gołębi.').should('be.visible');
                cy.get('button[aria-label="Edytuj kategorię Loty i treningi"]').should('be.visible');
                cy.get('button[aria-label="Usuń kategorię Loty i treningi"]').should('be.visible');
            });

            getCategoryCard('Zdrowie gołębi').should('be.visible').within(() => {
                cy.contains('a', 'Zdrowie gołębi').should('have.attr', 'href', '/forum/2');
                cy.contains('Profilaktyka, żywienie i zdrowie stada.').should('be.visible');
                cy.get('button[aria-label="Edytuj kategorię Zdrowie gołębi"]').should('not.exist');
                cy.get('button[aria-label="Usuń kategorię Zdrowie gołębi"]').should('not.exist');
            });

            cy.checkFooter();
        });

        // ==========================================
        // CREATE CATEGORY
        // ==========================================

        it('Should correctly render the add category modal', () => {
            cy.contains('button', '+ Dodaj kategorię').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Dodaj nową kategorię').should('be.visible');

                cy.contains('label', 'Nazwa kategorii').should('be.visible');
                cy.get('#category-name').should('be.visible').and('have.attr', 'minlength', '3').and('have.attr', 'maxlength', '100').and('have.attr', 'placeholder', 'np. Wystawy i Loty');

                cy.contains('label', 'Opis kategorii').should('be.visible');
                cy.get('#category-description').should('be.visible').and('have.attr', 'placeholder', 'Krótki opis tego, o czym dyskutuje się w tym dziale...');

                cy.contains('label', 'Kolejność wyświetlania').should('be.visible');
                cy.get('#category-sort-order').should('be.visible').and('have.attr', 'type', 'number').and('have.attr', 'min', '1').and('have.value', '1');
                cy.contains('Mniejsza liczba oznacza wyższą pozycję kategorii na liście.').should('be.visible');

                cy.contains('button', 'Anuluj').should('be.visible');
                cy.contains('button', 'Dodaj kategorię').should('be.visible').and('not.be.disabled');
            });
        });

        it('Should close the add category modal without creating a category', () => {
            cy.contains('button', '+ Dodaj kategorię').click();
            cy.get('#category-name').type('Nowa kategoria');
            cy.contains('button', 'Anuluj').click();

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('h2', 'Nowa kategoria').should('not.exist');
        });

        it('Should validate the category name after trimming whitespace', () => {
            cy.contains('button', '+ Dodaj kategorię').click();
            cy.get('#category-name').type('     ');

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Dodaj kategorię').click();
            });

            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nazwa kategorii musi mieć od 3 do 100 znaków.');
        });

        it('Should successfully create a category', () => {
            cy.intercept('POST', '**/api/forum/categories', (req) => {
                expect(req.body).to.deep.equal({
                    name: 'Wystawy',
                    description: 'Rozmowy o wystawach gołębi.',
                    sortOrder: 3
                });

                categories.push({
                    id: 3,
                    name: 'Wystawy',
                    description: 'Rozmowy o wystawach gołębi.',
                    sortOrder: 3,
                    createdAt: '2026-09-21T10:00:00',
                    canEdit: true,
                    canDelete: true
                });

                req.reply({ statusCode: 201 });
            }).as('createCategory');

            cy.contains('button', '+ Dodaj kategorię').click();
            cy.get('#category-name').type('  Wystawy  ');
            cy.get('#category-description').type('  Rozmowy o wystawach gołębi.  ');
            cy.get('#category-sort-order').clear().type('3');

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Dodaj kategorię').click();
            });

            cy.wait('@createCategory');
            cy.wait('@getCategories');

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('h2', 'Wystawy').should('be.visible');
            cy.contains('Rozmowy o wystawach gołębi.').should('be.visible');
        });

        it('Should display an API error when category creation fails', () => {
            cy.intercept('POST', '**/api/forum/categories', {
                statusCode: 400,
                body: 'Nie udało się utworzyć testowej kategorii.'
            }).as('createCategoryError');

            cy.contains('button', '+ Dodaj kategorię').click();
            cy.get('#category-name').type('Wystawy');

            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Dodaj kategorię').click();
            });

            cy.wait('@createCategoryError');

            cy.get('[role="dialog"]').should('be.visible');
            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Nie udało się utworzyć testowej kategorii.');
        });

        // ==========================================
        // EDIT CATEGORY
        // ==========================================

        it('Should preload category data in the edit modal and allow cancellation', () => {
            cy.get('button[aria-label="Edytuj kategorię Loty i treningi"]').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Edytuj kategorię').should('be.visible');
                cy.get('#category-name').should('have.value', 'Loty i treningi');
                cy.get('#category-description').should('have.value', 'Dyskusje o lotach, treningach i przygotowaniu gołębi.');
                cy.get('#category-sort-order').should('have.value', '1');
                cy.contains('button', 'Zapisz zmiany').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
        });

        it('Should successfully update a category', () => {
            cy.intercept('PUT', '**/api/forum/categories/1', (req) => {
                expect(req.body).to.deep.equal({
                    name: 'Loty konkursowe',
                    description: 'Zaktualizowany opis kategorii.',
                    sortOrder: 2
                });

                categories = categories.map(category => category.id === 1 ? {
                    ...category,
                    name: 'Loty konkursowe',
                    description: 'Zaktualizowany opis kategorii.',
                    sortOrder: 2
                } : category);

                req.reply({ statusCode: 200 });
            }).as('updateCategory');

            cy.get('button[aria-label="Edytuj kategorię Loty i treningi"]').click();
            cy.get('#category-name').clear().type('Loty konkursowe');
            cy.get('#category-description').clear().type('Zaktualizowany opis kategorii.');
            cy.get('#category-sort-order').clear().type('2');
            cy.contains('button', 'Zapisz zmiany').click();

            cy.wait('@updateCategory');
            cy.wait('@getCategories');

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('h2', 'Loty konkursowe').should('be.visible');
            cy.contains('Zaktualizowany opis kategorii.').should('be.visible');
            cy.contains('h2', 'Loty i treningi').should('not.exist');
        });

        it('Should display an API error when category update fails', () => {
            cy.intercept('PUT', '**/api/forum/categories/1', {
                statusCode: 403,
                body: 'Brak uprawnień do edycji tej kategorii.'
            }).as('updateCategoryError');

            cy.get('button[aria-label="Edytuj kategorię Loty i treningi"]').click();
            cy.get('#category-name').clear().type('Nowa nazwa');
            cy.contains('button', 'Zapisz zmiany').click();

            cy.wait('@updateCategoryError');

            cy.get('[role="dialog"]').should('be.visible');
            cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Brak uprawnień do edycji tej kategorii.');
        });

        // ==========================================
        // DELETE CATEGORY
        // ==========================================

        it('Should cancel category deletion without removing the category', () => {
            cy.get('button[aria-label="Usuń kategorię Loty i treningi"]').click();

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Usuń kategorię').should('be.visible');
                cy.contains('Czy na pewno chcesz usunąć tę kategorię?').should('be.visible');
                cy.contains('Upewnij się, że nie zawiera ona żadnych wątków.').should('be.visible');
                cy.contains('button', 'Anuluj').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('h2', 'Loty i treningi').should('be.visible');
        });

        it('Should successfully delete a category', () => {
            cy.intercept('DELETE', '**/api/forum/categories/1', (req) => {
                categories = categories.filter(category => category.id !== 1);
                req.reply({ statusCode: 204 });
            }).as('deleteCategory');

            cy.get('button[aria-label="Usuń kategorię Loty i treningi"]').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteCategory');
            cy.wait('@getCategories');

            cy.contains('h2', 'Loty i treningi').should('not.exist');
            cy.contains('h2', 'Zdrowie gołębi').should('be.visible');
        });

        it('Should display an error modal when category deletion fails', () => {
            cy.intercept('DELETE', '**/api/forum/categories/1', {
                statusCode: 409,
                body: 'Nie można usunąć kategorii, która zawiera wątki.'
            }).as('deleteCategoryError');

            cy.get('button[aria-label="Usuń kategorię Loty i treningi"]').click();
            cy.get('[role="dialog"]').within(() => {
                cy.contains('button', 'Potwierdź').click();
            });

            cy.wait('@deleteCategoryError');

            cy.get('[role="dialog"]').should('be.visible').within(() => {
                cy.contains('h2', 'Błąd').should('be.visible');
                cy.contains('Nie można usunąć kategorii, która zawiera wątki.').should('be.visible');
                cy.contains('button', 'OK').click();
            });

            cy.get('[role="dialog"]').should('not.exist');
            cy.contains('h2', 'Loty i treningi').should('be.visible');
        });

        // ==========================================
        // EMPTY STATE AND ERRORS
        // ==========================================

        it('Should display an empty state when no forum categories exist', () => {
            cy.intercept('GET', '**/api/forum/categories', {
                statusCode: 200,
                body: []
            }).as('getEmptyCategories');

            cy.reload();
            cy.wait('@getEmptyCategories');

            cy.contains('Brak dostępnych kategorii forum.').should('be.visible');
            cy.get('article').should('not.exist');
        });

        it('Should display an error and successfully retry loading forum categories', () => {
            let shouldFail = true;

            cy.intercept('GET', '**/api/forum/categories', (req) => {
                if (shouldFail) {
                    req.reply({
                        statusCode: 500,
                        body: 'Internal Server Error'
                    });
                } else {
                    req.reply({
                        statusCode: 200,
                        body: categories
                    });
                }
            }).as('getCategoriesRetry');

            cy.reload();
            cy.wait('@getCategoriesRetry');

            cy.contains('Nie udało się pobrać danych forum. Sprawdź połączenie.').should('be.visible');
            cy.contains('button', 'Spróbuj ponownie').should('be.visible');

            cy.then(() => {
                shouldFail = false;
            });

            cy.contains('button', 'Spróbuj ponownie').click();
            cy.wait('@getCategoriesRetry');

            cy.contains('Nie udało się pobrać danych forum. Sprawdź połączenie.').should('not.exist');
            cy.contains('h2', 'Loty i treningi').should('be.visible');
            cy.contains('h2', 'Zdrowie gołębi').should('be.visible');
        });
    });

    // ==========================================
    // ROLE PERMISSIONS
    // ==========================================

    describe('Role Permissions', () => {
        it('Should allow a moderator to create forum categories', () => {
            cy.intercept('GET', '**/api/forum/categories', {
                statusCode: 200,
                body: createCategories()
            }).as('getModeratorCategories');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum', moderatorToken);

            cy.wait(['@getModeratorCategories', '@getNotifications', '@getUnreadCount']);

            cy.checkLoggedInNavbar('Moderator');
            cy.contains('button', '+ Dodaj kategorię').should('be.visible');
        });

        it('Should hide category creation controls from a breeder', () => {
            const breederCategories = createCategories().map(category => ({
                ...category,
                canEdit: false,
                canDelete: false
            }));

            cy.intercept('GET', '**/api/forum/categories', {
                statusCode: 200,
                body: breederCategories
            }).as('getBreederCategories');

            cy.mockNavbarNotifications();
            cy.visitWithToken('/forum', breederToken);

            cy.wait(['@getBreederCategories', '@getNotifications', '@getUnreadCount']);

            cy.checkLoggedInNavbar('Hodowca');
            cy.contains('button', '+ Dodaj kategorię').should('not.exist');
            cy.get('button[aria-label^="Edytuj kategorię"]').should('not.exist');
            cy.get('button[aria-label^="Usuń kategorię"]').should('not.exist');
            cy.contains('h2', 'Loty i treningi').should('be.visible');
        });
    });

    // ==========================================
    // ACCESS CONTROL
    // ==========================================

    describe('Access Control', () => {
        it('Should redirect an unauthenticated user to the login page', () => {
            cy.intercept('GET', '**/api/forum/categories', {
                statusCode: 401,
                body: ''
            });

            cy.visit('/forum');

            cy.location('pathname').should('eq', '/login');
        });
    });
});