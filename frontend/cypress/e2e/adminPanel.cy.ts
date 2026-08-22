describe('Panel Administratora - Testy E2E', () => {
    const mockPending = [
        {
            id: 1, name: 'Jan', surname: 'Nowak', email: 'jan.nowak@test.pl', phoneNumber: '111222333',
            dateOfBirth: '1990-01-01', postalCode: '68-100', city: 'Żagań', street: 'Długa', houseNumber: '1',
            sectionId: 1, status: 'PENDING', createdAt: '2026-08-10T10:00:00', role: 'BREEDER', sectionName: 'Żagań'
        }
    ];

    const mockRegistered = [
        {
            id: 2, name: 'Anna', surname: 'Kowalska', email: 'anna.k@test.pl', phoneNumber: '444555666',
            dateOfBirth: '1985-05-15', postalCode: '68-113', city: 'Chotków', street: 'Krótka', houseNumber: '5a',
            sectionId: 3, status: 'ACTIVE', createdAt: '2026-08-01T10:00:00', role: 'BREEDER', sectionName: 'Chotków'
        },
        {
            id: 3, name: 'Michał', surname: 'Admin', email: 'admin@pzhgp.pl', phoneNumber: '000000000',
            dateOfBirth: '1980-01-01', postalCode: '68-120', city: 'Iłowa', street: 'Główna', houseNumber: '10b/14a',
            sectionId: 2, status: 'ACTIVE', createdAt: '2026-07-01T10:00:00', role: 'ADMINISTRATOR', sectionName: 'Wymiarki'
        },
        {
            id: 4, name: 'Piotr', surname: 'Moderator', email: 'piotr.m@test.pl', phoneNumber: '123123123',
            dateOfBirth: '1980-01-01', postalCode: '68-120', city: 'Iłowa', street: 'Główna', houseNumber: '1',
            sectionId: 4, status: 'ACTIVE', createdAt: '2026-06-13T10:00:00',
            role: 'MODERATOR', sectionName: 'Kożuchów'
        }
    ];

    const mockSections = [
        { id: 1, name: 'Żagań' },
        { id: 2, name: 'Wymiarki' },
        { id: 3, name: 'Chotków' },
        { id: 4, name: 'Kożuchów' }
    ];

    const fakeToken = "header.eyJzdWIiOiJhZG1pbkBwemhncC5wbCIsInJvbGUiOiJBRE1JTklTVFJBVE9SIiwiZXhwIjo5OTk5OTk5OTk5fQ.signature";

    beforeEach(() => {
        cy.intercept('GET', 'http://localhost:8080/api/admin/pending', {
            statusCode: 200,
            body: mockPending
        }).as('getPending');

        cy.intercept('GET', 'http://localhost:8080/api/admin/registered', {
            statusCode: 200,
            body: mockRegistered
        }).as('getRegistered');

        cy.intercept('GET', '**/api/sections', {
            statusCode: 200,
            body: mockSections
        }).as('getSections');

        cy.visit('/admin', {
            onBeforeLoad: (win) => {
                win.localStorage.setItem('jwt_token', fakeToken);
            }
        });

        cy.wait(['@getSections', '@getPending', '@getRegistered']);
    });

    it('Should correctly render the entire admin panel', () => {
        cy.get('[data-cy="admin-panel-title"]').should('be.visible').and('have.text', 'Panel Administratora');
        cy.contains('p', 'Zarządzanie kontami hodowców.').should('be.visible');

        cy.get('[data-cy="search-input"]').should('be.visible')
            .and('have.attr', 'placeholder', 'Szukaj po imieniu, nazwisku, emailu lub telefonie...');

        cy.get('[data-cy="section-filter"]').should('be.visible').within(() => {
            cy.get('option').should('have.length', 5);
            cy.get('option').eq(0).should('have.text', 'Wszystkie sekcje');
            cy.get('option').eq(1).should('have.text', 'Żagań');
            cy.get('option').eq(2).should('have.text', 'Wymiarki');
            cy.get('option').eq(3).should('have.text', 'Chotków');
            cy.get('option').eq(4).should('have.text', 'Kożuchów');
        });
        cy.get('[data-cy="section-filter"]').find('option:selected').should('have.text', 'Wszystkie sekcje');

        cy.get('[data-cy="sort-filter"]').should('be.visible').within(() => {
            cy.get('option').should('have.length', 4);
            cy.get('option').eq(0).should('have.text', 'Od najnowszego');
            cy.get('option').eq(1).should('have.text', 'Od najstarszego');
            cy.get('option').eq(2).should('have.text', 'Alfabetycznie (A-Z)');
            cy.get('option').eq(3).should('have.text', 'Alfabetycznie (Z-A)');
        });
        cy.get('[data-cy="sort-filter"]').find('option:selected').should('have.text', 'Od najnowszego');

        // ==========================================
        // TABELA: Oczekujące konta
        // ==========================================

        cy.contains('h2', 'Konta czekające na akceptację').should('be.visible');

        cy.get('[data-cy="pending-table"]').should('be.visible').within(() => {
            cy.get('thead th').eq(0).should('contain.text', 'Hodowca');
            cy.get('thead th').eq(1).should('contain.text', 'Kontakt');
            cy.get('thead th').eq(2).should('contain.text', 'Sekcja');
            cy.get('thead th').eq(3).should('contain.text', 'Data Rejestracji');
            cy.get('thead th').eq(4).should('contain.text', 'Akcje');

            cy.get('[data-cy="pending-row-1"]').within(() => {
                cy.get('td').eq(0).within(() => {
                    cy.contains('Jan Nowak').should('be.visible');
                });

                cy.get('td').eq(1).within(() => {
                    cy.contains('jan.nowak@test.pl').should('be.visible');
                    cy.contains('Tel: 111222333').should('be.visible');
                });

                cy.get('td').eq(2).within(() => {
                    cy.contains('Żagań').should('be.visible');
                });

                cy.get('td').eq(3).should('contain.text', '10.08.2026');
                cy.get('td').eq(4).within(() => {
                    cy.get('[data-cy="approve-btn-1"]').should('be.visible').and('contain.text', 'Akceptuj');
                    cy.get('[data-cy="reject-btn-1"]').should('be.visible').and('contain.text', 'Odrzuć');
                });
            });
        });

        // ========================
        // TABELA: Konta hodowców
        // ========================
        cy.contains('h2', 'Konta hodowców').should('be.visible');

        cy.get('[data-cy="registered-table"]').should('be.visible').within(() => {
            cy.get('thead th').eq(0).should('contain.text', 'Hodowca');
            cy.get('thead th').eq(1).should('contain.text', 'Kontakt');
            cy.get('thead th').eq(2).should('contain.text', 'Status');
            cy.get('thead th').eq(3).should('contain.text', 'Sekcja');
            cy.get('thead th').eq(4).should('contain.text', 'Data Rejestracji');
            cy.get('thead th').eq(5).should('contain.text', 'Akcje');

            ///////////////////////////
            // Hodowca - Anna Kowalska
            ///////////////////////////

            cy.get('[data-cy="registered-row-2"]').within(() => {
                cy.get('td').eq(0).within(() => {
                    cy.contains('Anna Kowalska').should('be.visible');
                });

                cy.get('td').eq(1).within(() => {
                    cy.contains('anna.k@test.pl').should('be.visible');
                    cy.contains('Tel: 444555666').should('be.visible');
                });

                cy.get('td').eq(2).find('[data-cy="status-active"]')
                    .should('be.visible')
                    .and('contain.text', 'Aktywny');

                cy.get('td').eq(3).within(() => {
                    cy.contains('Chotków').should('be.visible');
                });

                cy.get('td').eq(4).should('contain.text', '01.08.2026');
                cy.get('td').eq(5).find('[data-cy="kebab-menu-btn-2"]').should('be.visible');
            });

            ////////////////////////////////
            // Administrator - Michał Admin
            ////////////////////////////////

            cy.get('[data-cy="registered-row-3"]').within(() => {
                cy.get('td').eq(0).within(() => {
                    cy.contains('Michał Admin').should('be.visible');
                    cy.get('[data-cy="badge-admin"]').should('be.visible').and('contain.text', 'ADMIN');
                    cy.get('[data-cy="badge-you"]').should('be.visible').and('contain.text', 'TO TY');
                    cy.get('[data-cy="badge-moderator"]').should('not.exist');
                });

                cy.get('td').eq(1).within(() => {
                    cy.contains('admin@pzhgp.pl').should('be.visible');
                    cy.contains('Tel: 000000000').should('be.visible');
                });

                cy.get('td').eq(2).find('[data-cy="status-active"]')
                    .should('be.visible')
                    .and('contain.text', 'Aktywny');

                cy.get('td').eq(3).within(() => {
                    cy.contains('Wymiarki').should('be.visible');
                });

                cy.get('td').eq(4).should('contain.text', '01.07.2026');
                cy.get('td').eq(5).find('[data-cy="kebab-menu-btn-3"]').should('be.visible');
            });

            ///////////////////////////////
            // Moderator - Piotr moderator
            ///////////////////////////////

            cy.get('[data-cy="registered-row-4"]').within(() => {
                cy.get('td').eq(0).within(() => {
                    cy.contains('Piotr Moderator').should('be.visible');
                    cy.get('[data-cy="badge-moderator"]').should('be.visible').and('contain.text', 'MODERATOR');
                    cy.get('[data-cy="badge-admin"]').should('not.exist');
                    cy.get('[data-cy="badge-you"]').should('not.exist');
                });

                cy.get('td').eq(1).within(() => {
                    cy.contains('piotr.m@test.pl').should('be.visible');
                });

                cy.get('td').eq(2).find('[data-cy="status-active"]')
                    .should('be.visible')
                    .and('contain.text', 'Aktywny');

                cy.get('td').eq(3).within(() => {
                    cy.contains('Kożuchów').should('be.visible');
                });

                cy.get('td').eq(4).should('contain.text', '13.06.2026');
                cy.get('td').eq(5).find('[data-cy="kebab-menu-btn-4"]').should('be.visible');
            });
        });

        // ==================
        // WERYFIKACJA MENU
        // ==================

        cy.get('[data-cy="kebab-menu-btn-2"]').click();
        cy.get('[data-cy="dropdown-menu-2"]').should('be.visible').within(() => {
            cy.get('[data-cy="details-option"]').should('be.visible').and('have.text', 'Szczegóły');
            cy.get('[data-cy="change-role-option"]').should('be.visible').and('have.text', 'Zmień rolę');
            cy.get('[data-cy="block-option"]').should('be.visible').and('have.text', 'Zablokuj');
        });
    });

    it('Should correctly filter both tables using the search function across all attributes', () => {
        const searchInput = () => cy.get('[data-cy="search-input"]');

        // =====================================
        // Wyszukiwanie użytkownika: Jan Nowak
        // =====================================

        searchInput().clear().type('Jan');
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="no-registered-accounts"]').should('be.visible');

        searchInput().clear().type('Nowak');
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="no-registered-accounts"]').should('be.visible');

        searchInput().clear().type('jan.nowak@test.pl');
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="no-registered-accounts"]').should('be.visible');

        searchInput().clear().type('111222333');
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="no-registered-accounts"]').should('be.visible');


        // =========================================
        // Wyszukiwanie użytkownika: Anna Kowalska
        // =========================================

        searchInput().clear().type('Anna');
        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="registered-row-3"]').should('not.exist');
        cy.get('[data-cy="registered-row-4"]').should('not.exist');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');

        searchInput().clear().type('Kowalska');
        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="registered-row-3"]').should('not.exist');
        cy.get('[data-cy="registered-row-4"]').should('not.exist');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');

        searchInput().clear().type('anna.k');
        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');

        searchInput().clear().type('444555666');
        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');


        // ==========================
        // Czyszczenie wyszukiwarki
        // ==========================

        searchInput().clear();
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="registered-row-3"]').should('be.visible');
        cy.get('[data-cy="registered-row-4"]').should('be.visible');
    });

    it('Should correctly filter both tables after a specific section is selected from the list', () => {
        const sectionFilter = () => cy.get('[data-cy="section-filter"]');

        sectionFilter().select('1');
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="pending-row-1"]').find('td').eq(2).should('contain.text', 'Żagań');

        cy.get('[data-cy="no-registered-accounts"]').should('be.visible');
        cy.get('[data-cy="registered-row-2"]').should('not.exist');
        cy.get('[data-cy="registered-row-3"]').should('not.exist');
        cy.get('[data-cy="registered-row-4"]').should('not.exist');

        sectionFilter().select('2');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');
        cy.get('[data-cy="pending-row-1"]').should('not.exist');

        cy.get('[data-cy="registered-row-3"]').should('be.visible');
        cy.get('[data-cy="registered-row-3"]').find('td').eq(3).should('contain.text', 'Wymiarki');
        cy.get('[data-cy="registered-row-2"]').should('not.exist');
        cy.get('[data-cy="registered-row-4"]').should('not.exist');

        sectionFilter().select('3');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');

        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="registered-row-2"]').find('td').eq(3).should('contain.text', 'Chotków');
        cy.get('[data-cy="registered-row-3"]').should('not.exist');
        cy.get('[data-cy="registered-row-4"]').should('not.exist');

        sectionFilter().select('4');
        cy.get('[data-cy="no-pending-accounts"]').should('be.visible');
        cy.get('[data-cy="pending-row-1"]').should('not.exist');

        cy.get('[data-cy="registered-row-4"]').should('be.visible');
        cy.get('[data-cy="registered-row-4"]').find('td').eq(3).should('contain.text', 'Kożuchów');
        cy.get('[data-cy="registered-row-2"]').should('not.exist');
        cy.get('[data-cy="registered-row-3"]').should('not.exist');

        sectionFilter().select('ALL');
        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="registered-row-2"]').should('be.visible');
        cy.get('[data-cy="registered-row-3"]').should('be.visible');
        cy.get('[data-cy="registered-row-4"]').should('be.visible');
    });

    it('Should correctly sort users in the table after a sorting option is selected', () => {
        const sortFilter = () => cy.get('[data-cy="sort-filter"]');
        const getRegisteredRows = () => cy.get('[data-cy="registered-table"] tbody tr');

        cy.get('[data-cy="pending-table"] tbody tr').should('have.length', 1).and('contain.text', 'Jan Nowak');

        sortFilter().select('NEWEST');
        getRegisteredRows().should('have.length', 3);
        getRegisteredRows().eq(0).should('contain.text', 'Anna Kowalska');
        getRegisteredRows().eq(1).should('contain.text', 'Michał Admin');
        getRegisteredRows().eq(2).should('contain.text', 'Piotr Moderator');

        sortFilter().select('OLDEST');
        getRegisteredRows().should('have.length', 3);
        getRegisteredRows().eq(0).should('contain.text', 'Piotr Moderator');
        getRegisteredRows().eq(1).should('contain.text', 'Michał Admin');
        getRegisteredRows().eq(2).should('contain.text', 'Anna Kowalska');

        sortFilter().select('A_Z');
        getRegisteredRows().should('have.length', 3);
        getRegisteredRows().eq(0).should('contain.text', 'Anna Kowalska');
        getRegisteredRows().eq(1).should('contain.text', 'Michał Admin');
        getRegisteredRows().eq(2).should('contain.text', 'Piotr Moderator');

        sortFilter().select('Z_A');
        getRegisteredRows().should('have.length', 3);
        getRegisteredRows().eq(0).should('contain.text', 'Piotr Moderator');
        getRegisteredRows().eq(1).should('contain.text', 'Michał Admin');
        getRegisteredRows().eq(2).should('contain.text', 'Anna Kowalska');

        sortFilter().select('NEWEST');
        getRegisteredRows().eq(0).should('contain.text', 'Anna Kowalska');
        getRegisteredRows().eq(1).should('contain.text', 'Michał Admin');
        getRegisteredRows().eq(2).should('contain.text', 'Piotr Moderator');
    });

    it('Should successfully accept the pending account and move it to the breeders table', () => {
        cy.intercept('PUT', 'http://localhost:8080/api/admin/approve/1', {
            statusCode: 200,
            body: 'Konto zostało pomyślnie zaakceptowane.'
        }).as('approveAccount');

        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="approve-btn-1"]').click();

        cy.contains('h3', 'Potwierdzenie akcji').should('be.visible');
        cy.get('[data-cy="confirm-dialog-btn"]').should('be.visible').click();

        cy.wait('@approveAccount');

        cy.get('[data-cy="pending-row-1"]').should('not.exist');
        cy.get('[data-cy="no-pending-accounts"]')
            .should('be.visible')
            .and('contain.text', 'Brak kont oczekujących na akceptację.');

        cy.get('[data-cy="registered-row-1"]').within(() => {
            cy.get('td').eq(0).should('contain.text', 'Jan Nowak');
            cy.get('td').eq(2).find('[data-cy="status-active"]')
                .should('be.visible')
                .and('contain.text', 'Aktywny');
        });
    });

    it('Should correctly reject the pending account and remove it from the system', () => {
        cy.intercept('DELETE', 'http://localhost:8080/api/admin/reject/1', {
            statusCode: 200,
            body: 'Konto zostało odrzucone i usunięte.'
        }).as('rejectAccount');

        cy.get('[data-cy="pending-row-1"]').should('be.visible');
        cy.get('[data-cy="reject-btn-1"]').click();

        cy.contains('h3', 'Potwierdzenie akcji').should('be.visible');
        cy.get('[data-cy="confirm-dialog-btn"]').should('be.visible').click();

        cy.wait('@rejectAccount');

        cy.get('[data-cy="pending-row-1"]').should('not.exist');
        cy.get('[data-cy="no-pending-accounts"]')
            .should('be.visible')
            .and('contain.text', 'Brak kont oczekujących na akceptację.');

        cy.get('[data-cy="registered-row-1"]').should('not.exist');
        cy.get('[data-cy="registered-table"]').should('not.contain.text', 'Jan Nowak');
    });

    it('Should correctly open the breeder details modal and display the complete data', () => {
        cy.get('[data-cy="kebab-menu-btn-2"]').click();

        cy.get('[data-cy="dropdown-menu-2"]').should('be.visible');
        cy.get('[data-cy="details-option"]').click();

        cy.get('[data-cy="details-modal"]').should('be.visible').within(() => {
            cy.contains('h3', 'Dane hodowcy').should('be.visible');

            cy.contains('p', 'Imię i nazwisko').next().should('have.text', 'Anna Kowalska');
            cy.contains('p', 'Email').next().should('have.text', 'anna.k@test.pl');
            cy.contains('p', 'Telefon').next().should('have.text', '444555666');
            cy.contains('p', 'Data urodzenia').next().should('have.text', '15.05.1985');
            cy.contains('p', 'Adres zamieszkania').next().should('contain.text', 'ul. Krótka 5a').and('contain.text', '68-113 Chotków');
            cy.contains('p', 'Sekcja').next().should('have.text', 'Chotków');
            cy.contains('p', 'Data rejestracji').next().should('contain.text', '01.08.2026');
            cy.contains('p', 'Rola w systemie').next().should('have.text', 'BREEDER');
            cy.contains('p', 'Obecny status').next().should('have.text', 'ACTIVE');

            cy.contains('button', 'Zamknij').click();
        });

        cy.get('[data-cy="details-modal"]').should('not.exist');
    });

    it('Should correctly open the role change modal, display the data, and save the new role', () => {
        cy.intercept('PUT', 'http://localhost:8080/api/admin/2/role', {
            statusCode: 200,
            body: 'Rola została pomyślnie zmieniona'
        }).as('changeRole');

        cy.get('[data-cy="kebab-menu-btn-2"]').click();
        cy.get('[data-cy="change-role-option"]').click();

        cy.get('[data-cy="role-change-modal"]').should('be.visible').within(() => {
            cy.contains('h3', 'Zmień rolę').should('be.visible');
            cy.contains('Wybierz nowe uprawnienia dla użytkownika:').should('be.visible');
            cy.contains('Anna Kowalska').should('be.visible');
            cy.contains('label', 'Nowa rola w systemie').should('be.visible');

            cy.get('[data-cy="role-select"]').should('be.visible').within(() => {
                cy.get('option').should('have.length', 2);
                cy.get('option').eq(0).should('contain.text', 'Hodowca (Podstawowy dostęp)');
                cy.get('option').eq(1).should('contain.text', 'Moderator (Zarządzanie lotami)');
            });

            cy.get('[data-cy="cancel-role-btn"]').should('be.visible');
            cy.get('[data-cy="save-role-btn"]').should('be.visible');

            cy.get('[data-cy="role-select"]').find('option:selected').should('have.value', 'BREEDER');
            cy.get('[data-cy="role-select"]').select('MODERATOR');
            cy.get('[data-cy="save-role-btn"]').click();
        });

        cy.wait('@changeRole');
        cy.get('[data-cy="role-change-modal"]').should('not.exist');

        cy.get('[data-cy="registered-row-2"]').within(() => {
            cy.get('[data-cy="badge-moderator"]')
                .should('be.visible')
                .and('contain.text', 'MODERATOR');
        });
    });

    it('Should correctly block the user and then unblock them', () => {
        cy.intercept('PUT', 'http://localhost:8080/api/admin/block/2', {
            statusCode: 200,
            body: 'Konto zostało zablokowane.'
        }).as('blockAccount');

        cy.intercept('PUT', 'http://localhost:8080/api/admin/unblock/2', {
            statusCode: 200,
            body: 'Konto zostało odblokowane.'
        }).as('unblockAccount');

        // =============
        // Blokowanie
        // =============

        cy.get('[data-cy="registered-row-2"]').within(() => {
            cy.get('[data-cy="status-active"]').should('be.visible').and('contain.text', 'Aktywny');
        });

        cy.get('[data-cy="kebab-menu-btn-2"]').click();
        cy.get('[data-cy="block-option"]').click();

        cy.contains('h3', 'Potwierdzenie akcji').should('be.visible');
        cy.get('[data-cy="confirm-dialog-btn"]').should('be.visible').click();

        cy.wait('@blockAccount');

        cy.get('[data-cy="registered-row-2"]').within(() => {
            cy.get('[data-cy="status-blocked"]').should('be.visible').and('contain.text', 'Zablokowany');
            cy.get('[data-cy="status-active"]').should('not.exist');
        });

        // =================
        // Odblokowywanie
        // =================

        cy.get('[data-cy="kebab-menu-btn-2"]').click();
        cy.get('[data-cy="unblock-option"]').click();

        cy.contains('h3', 'Potwierdzenie akcji').should('be.visible');
        cy.get('[data-cy="confirm-dialog-btn"]').should('be.visible').click();

        cy.wait('@unblockAccount');

        cy.get('[data-cy="registered-row-2"]').within(() => {
            cy.get('[data-cy="status-active"]').should('be.visible').and('contain.text', 'Aktywny');
            cy.get('[data-cy="status-blocked"]').should('not.exist');
        });
    });
});