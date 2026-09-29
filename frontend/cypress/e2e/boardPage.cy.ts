import { createFakeToken } from '../support/testUtils';

type MockBoardRole =
    | 'PREZES'
    | 'WICEPREZES_DS_LOTOWYCH'
    | 'WICEPREZES_DS_FINANSOWYCH'
    | 'WICEPREZES_DS_GOSPODARCZYCH'
    | 'SEKRETARZ'
    | 'SKARBNIK'
    | 'CZLONEK_ZARZADU';

interface MockBoardMember {
    id: number;
    role: MockBoardRole;
    managedSectionId: number | null;
    managedSectionName: string | null;
    firstName: string;
    lastName: string;
    publicPhoneNumber: string | null;
    breederId: number | null;
}

describe('Board Page Tests', () => {
    const adminToken = createFakeToken('ADMINISTRATOR', 'admin@pzhgp.pl', 'Admin');
    const breederToken = createFakeToken('BREEDER', 'breeder@pzhgp.pl', 'Hodowca');

    const mockSections = [
        { id: 1, name: 'Żagań' },
        { id: 2, name: 'Wymiarki' },
        { id: 3, name: 'Chotków' }
    ];

    const mockBreeders = [
        { id: 10, name: 'Jan', surname: 'Kowalski', status: 'ACTIVE', sectionId: 1 },
        { id: 11, name: 'Anna', surname: 'Nowak', status: 'ACTIVE', sectionId: 1 },
        { id: 12, name: 'Piotr', surname: 'Testowy', status: 'ACTIVE', sectionId: 2 },
        { id: 13, name: 'Blokowany', surname: 'Hodowca', status: 'BLOCKED', sectionId: 1 },
        { id: 14, name: 'Marek', surname: 'Testowy', status: 'ACTIVE', sectionId: 3 }
    ];

    const createBoardMembers = (): MockBoardMember[] => [
        {
            id: 6,
            role: 'SEKRETARZ',
            managedSectionId: 1,
            managedSectionName: 'Żagań',
            firstName: 'Celina',
            lastName: 'Adamczyk',
            publicPhoneNumber: null,
            breederId: null
        },
        {
            id: 3,
            role: 'CZLONEK_ZARZADU',
            managedSectionId: null,
            managedSectionName: null,
            firstName: 'Zbigniew',
            lastName: 'Wiśniewski',
            publicPhoneNumber: null,
            breederId: null
        },
        {
            id: 7,
            role: 'PREZES',
            managedSectionId: 2,
            managedSectionName: 'Wymiarki',
            firstName: 'Piotr',
            lastName: 'Testowy',
            publicPhoneNumber: '222333444',
            breederId: 12
        },
        {
            id: 1,
            role: 'PREZES',
            managedSectionId: null,
            managedSectionName: null,
            firstName: 'Jan',
            lastName: 'Kowalski',
            publicPhoneNumber: '111222333',
            breederId: 10
        },
        {
            id: 5,
            role: 'SKARBNIK',
            managedSectionId: 1,
            managedSectionName: 'Żagań',
            firstName: 'Barbara',
            lastName: 'Kowalska',
            publicPhoneNumber: '555666777',
            breederId: null
        },
        {
            id: 2,
            role: 'WICEPREZES_DS_LOTOWYCH',
            managedSectionId: null,
            managedSectionName: null,
            firstName: 'Adam',
            lastName: 'Nowak',
            publicPhoneNumber: null,
            breederId: null
        },
        {
            id: 4,
            role: 'PREZES',
            managedSectionId: 1,
            managedSectionName: 'Żagań',
            firstName: 'Anna',
            lastName: 'Zielińska',
            publicPhoneNumber: '444555666',
            breederId: 11
        }
    ];

    let boardMembers: MockBoardMember[] = createBoardMembers();

    const getMemberCard = (fullName: string) => {
        return cy.contains('h3', fullName).parent();
    };

    const getBoardSection = (heading: string) => {
        return cy.contains('h2', heading).closest('section');
    };

    const getBreederSelect = () => {
        return cy.get('[role="dialog"] select').eq(2);
    };

    const openAddMemberModal = () => {
        cy.contains('button', '+ Dodaj członka').click();
        cy.contains('h2', 'Dodaj członka zarządu').should('be.visible');
    };

    beforeEach(() => {
        boardMembers = createBoardMembers();

        cy.intercept('GET', '**/api/board', (req) => {
            req.reply({
                statusCode: 200,
                body: boardMembers
            });
        }).as('getBoard');

        cy.intercept('GET', '**/api/sections', {
            statusCode: 200,
            body: mockSections
        }).as('getSections');

        cy.intercept('GET', '**/api/admin/registered', {
            statusCode: 200,
            body: mockBreeders
        }).as('getRegisteredBreeders');

        cy.mockNavbarNotifications();
        cy.visitWithToken('/board', adminToken);

        cy.wait(['@getBoard', '@getSections', '@getNotifications', '@getUnreadCount']);
    });

    // ==========================================
    // PAGE RENDERING
    // ==========================================

    it('Should correctly render all visible elements of the board page', () => {
        cy.checkLoggedInNavbar('Admin');

        cy.get('h1').should('be.visible').and('have.text', 'Zarząd');
        cy.contains('Struktura organizacyjna oddziału i sekcji.').should('be.visible');
        cy.contains('button', '+ Dodaj członka').should('be.visible');

        cy.contains('h2', 'Zarząd Oddziału:').should('be.visible');
        cy.contains('h2', 'Zarząd Sekcji: Żagań').should('be.visible');
        cy.contains('h2', 'Zarząd Sekcji: Wymiarki').should('be.visible');
        cy.contains('h2', 'Zarząd Sekcji: Chotków').should('be.visible');

        getMemberCard('Jan Kowalski').should('be.visible').within(() => {
            cy.contains('Prezes').should('be.visible');
            cy.contains('111 222 333').should('be.visible');
            cy.get('a[href="tel:+48111222333"]').should('be.visible');
            cy.get('button[aria-label="Edytuj Jan Kowalski"]').should('be.visible');
            cy.get('button[aria-label="Usuń Jan Kowalski"]').should('be.visible');
        });

        getMemberCard('Adam Nowak').should('be.visible').within(() => {
            cy.contains('V-ce Prezes ds. lotowych').should('be.visible');
            cy.get('a[href^="tel:"]').should('not.exist');
        });

        getMemberCard('Zbigniew Wiśniewski').should('be.visible').within(() => {
            cy.contains('Członek Zarządu').should('be.visible');
        });

        getMemberCard('Anna Zielińska').should('be.visible').within(() => {
            cy.contains('Prezes').should('be.visible');
            cy.contains('444 555 666').should('be.visible');
        });

        getMemberCard('Barbara Kowalska').should('be.visible').within(() => {
            cy.contains('Skarbnik').should('be.visible');
            cy.contains('555 666 777').should('be.visible');
        });

        getMemberCard('Celina Adamczyk').should('be.visible').within(() => {
            cy.contains('Sekretarz').should('be.visible');
        });

        getMemberCard('Piotr Testowy').should('be.visible').within(() => {
            cy.contains('Prezes').should('be.visible');
            cy.contains('222 333 444').should('be.visible');
        });

        getBoardSection('Zarząd Sekcji: Chotków').within(() => {
            cy.contains('Brak dodanych członków zarządu sekcji.').should('be.visible');
        });

        cy.get('[role="dialog"]').should('not.exist');
        cy.checkFooter();
    });

    // ==========================================
    // SORTING
    // ==========================================

    it('Should correctly sort board members according to their roles', () => {
        getBoardSection('Zarząd Oddziału:').within(() => {
            cy.get('h3').should('have.length', 3);
            cy.get('h3').eq(0).should('have.text', 'Jan Kowalski');
            cy.get('h3').eq(1).should('have.text', 'Adam Nowak');
            cy.get('h3').eq(2).should('have.text', 'Zbigniew Wiśniewski');
        });

        getBoardSection('Zarząd Sekcji: Żagań').within(() => {
            cy.get('h3').should('have.length', 3);
            cy.get('h3').eq(0).should('have.text', 'Anna Zielińska');
            cy.get('h3').eq(1).should('have.text', 'Barbara Kowalska');
            cy.get('h3').eq(2).should('have.text', 'Celina Adamczyk');
        });
    });

    // ==========================================
    // EMPTY STATE
    // ==========================================

    it('Should display empty states when there are no board members', () => {
        boardMembers = [];

        cy.reload();
        cy.wait('@getBoard');

        getBoardSection('Zarząd Oddziału:').within(() => {
            cy.contains('Brak dodanych członków zarządu oddziału.').should('be.visible');
        });

        getBoardSection('Zarząd Sekcji: Żagań').within(() => {
            cy.contains('Brak dodanych członków zarządu sekcji.').should('be.visible');
        });

        getBoardSection('Zarząd Sekcji: Wymiarki').within(() => {
            cy.contains('Brak dodanych członków zarządu sekcji.').should('be.visible');
        });

        getBoardSection('Zarząd Sekcji: Chotków').within(() => {
            cy.contains('Brak dodanych członków zarządu sekcji.').should('be.visible');
        });
    });

    // ==========================================
    // ERROR AND RETRY
    // ==========================================

    it('Should display an error and successfully retry loading board data', () => {
        let shouldFail = true;

        cy.intercept('GET', '**/api/board', (req) => {
            if (shouldFail) {
                req.reply({
                    statusCode: 500,
                    body: 'Internal Server Error'
                });
            } else {
                req.reply({
                    statusCode: 200,
                    body: boardMembers
                });
            }
        }).as('getBoardRetry');

        cy.reload();
        cy.wait('@getBoardRetry');

        cy.contains('Nie udało się pobrać danych zarządu.').should('be.visible');
        cy.contains('button', 'Spróbuj ponownie').should('be.visible');

        cy.then(() => {
            shouldFail = false;
        });

        cy.contains('button', 'Spróbuj ponownie').click();
        cy.wait('@getBoardRetry');

        cy.contains('Nie udało się pobrać danych zarządu.').should('not.exist');
        cy.contains('h2', 'Zarząd Oddziału:').should('be.visible');
        getMemberCard('Jan Kowalski').should('be.visible');
    });

    // ==========================================
    // PUBLIC ACCESS
    // ==========================================

    it('Should allow an unauthenticated user to view the board without administration controls', () => {
        let adminDictionaryRequests = 0;

        cy.intercept('GET', '**/api/admin/registered', (req) => {
            adminDictionaryRequests++;
            req.reply({
                statusCode: 403,
                body: 'Forbidden'
            });
        });

        cy.intercept('GET', '**/api/board', (req) => {
            expect(req.headers.authorization).to.be.undefined;

            req.reply({
                statusCode: 200,
                body: boardMembers
            });
        }).as('getPublicBoard');

        cy.window().then((win) => {
            win.localStorage.removeItem('jwt_token');
            win.sessionStorage.removeItem('jwt_token');
        });

        cy.reload();
        cy.wait('@getPublicBoard');

        cy.checkLoggedOutNavbar();

        cy.get('h1').should('have.text', 'Zarząd');
        getMemberCard('Jan Kowalski').should('be.visible');

        cy.contains('button', '+ Dodaj członka').should('not.exist');
        cy.get('button[aria-label^="Edytuj "]').should('not.exist');
        cy.get('button[aria-label^="Usuń "]').should('not.exist');

        cy.then(() => {
            expect(adminDictionaryRequests).to.eq(0);
        });

        cy.checkFooter();
    });

    // ==========================================
    // BREEDER ACCESS
    // ==========================================

    it('Should hide administration controls from a breeder', () => {
        cy.window().then((win) => {
            win.localStorage.setItem('jwt_token', breederToken);
            win.sessionStorage.removeItem('jwt_token');
        });

        cy.reload();
        cy.wait('@getBoard');

        cy.checkLoggedInNavbar('Hodowca');

        cy.contains('button', '+ Dodaj członka').should('not.exist');
        cy.get('button[aria-label^="Edytuj "]').should('not.exist');
        cy.get('button[aria-label^="Usuń "]').should('not.exist');

        getMemberCard('Jan Kowalski').should('be.visible');
    });

    // ==========================================
    // DICTIONARY LOADING
    // ==========================================

    it('Should load administrator form dictionaries only after the modal is opened', () => {
        let registeredBreedersRequests = 0;

        cy.intercept('GET', '**/api/admin/registered', (req) => {
            registeredBreedersRequests++;

            req.reply({
                statusCode: 200,
                body: mockBreeders
            });
        }).as('getModalBreeders');

        cy.reload();
        cy.wait('@getBoard');

        cy.then(() => {
            expect(registeredBreedersRequests).to.eq(0);
        });

        cy.contains('button', '+ Dodaj członka').click();
        cy.wait('@getModalBreeders');

        cy.then(() => {
            expect(registeredBreedersRequests).to.eq(1);
        });

        cy.contains('h2', 'Dodaj członka zarządu').should('be.visible');
    });

    // ==========================================
    // ADD MEMBER MODAL
    // ==========================================

    it('Should correctly render and close the add board member modal', () => {
        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Dodaj członka zarządu').should('be.visible');

            cy.contains('label', 'Zarząd (Oddział czy Sekcja)').should('be.visible');
            cy.get('#board-type').should('be.visible').and('have.value', '0');

            cy.contains('label', 'Stanowisko').should('be.visible');
            cy.get('#board-role').should('be.visible').and('have.value', 'CZLONEK_ZARZADU');

            cy.contains('Dane osoby pełniącej funkcję').should('be.visible');
            cy.contains('Konto w systemie').should('be.visible');
            cy.contains('Osoba bez konta').should('be.visible');

            cy.get('#board-breeder').should('be.checked');
            cy.contains('option', '-- Wybierz hodowcę --').should('exist');

            cy.contains('label', 'Publiczny nr telefonu (opcjonalny)').should('be.visible');
            cy.get('#contact-phone').should('be.visible').and('have.attr', 'placeholder', 'np. 123 456 789');

            cy.contains('button', 'Anuluj').should('be.visible');
            cy.contains('button', 'Zapisz').should('be.visible').and('not.be.disabled');

            cy.contains('button', 'Anuluj').click();
        });

        cy.get('[role="dialog"]').should('not.exist');
    });

    // ==========================================
    // ROLE AND SECTION FILTERING
    // ==========================================

    it('Should correctly change available roles and breeders after selecting a board section', () => {
        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        cy.get('#board-role').within(() => {
            cy.get('option').should('have.length', 6);
            cy.contains('option', 'Prezes').should('exist');
            cy.contains('option', 'V-ce Prezes ds. lotowych').should('exist');
            cy.contains('option', 'V-ce Prezes ds. finansowych').should('exist');
            cy.contains('option', 'V-ce Prezes ds. gospodarczych').should('exist');
            cy.contains('option', 'Sekretarz').should('exist');
            cy.contains('option', 'Członek Zarządu').should('exist');
            cy.contains('option', 'Skarbnik').should('not.exist');
        });

        getBreederSelect().select('10');

        cy.get('#board-type').select('1');
        cy.get('#board-role').should('have.value', 'PREZES');

        cy.get('#board-role').within(() => {
            cy.get('option').should('have.length', 3);
            cy.contains('option', 'Prezes').should('exist');
            cy.contains('option', 'Skarbnik').should('exist');
            cy.contains('option', 'Sekretarz').should('exist');
            cy.contains('option', 'Członek Zarządu').should('not.exist');
        });

        getBreederSelect().within(() => {
            cy.contains('option', 'Jan Kowalski').should('exist');
            cy.contains('option', 'Anna Nowak').should('exist');
            cy.contains('option', 'Piotr Testowy').should('not.exist');
            cy.contains('option', 'Blokowany Hodowca').should('not.exist');
        });

        cy.get('#board-role').select('SKARBNIK');
        cy.get('#board-type').select('2');

        getBreederSelect().should(($select) => {
            expect($select.val()).to.be.null;
        });

        getBreederSelect().within(() => {
            cy.contains('option', 'Piotr Testowy').should('exist');
            cy.contains('option', 'Jan Kowalski').should('not.exist');
        });

        cy.get('#board-type').select('0');
        cy.get('#board-role').should('have.value', 'PREZES');
        cy.get('#board-role').find('option[value="SKARBNIK"]').should('not.exist');
    });

    // ==========================================
    // PERSON SOURCE
    // ==========================================

    it('Should switch between a registered breeder and a person without an account', () => {
        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        cy.get('#board-breeder').should('be.checked');
        getBreederSelect().should('be.visible');
        cy.get('#custom-name').should('not.exist');
        cy.get('#custom-surname').should('not.exist');

        cy.contains('label', 'Osoba bez konta').click();

        cy.get('#board-breeder').should('not.be.checked');
        getBreederSelect().should('not.exist');
        cy.get('#custom-name').should('be.visible').and('have.attr', 'required');
        cy.get('#custom-surname').should('be.visible').and('have.attr', 'required');

        cy.contains('label', 'Konto w systemie').click();

        cy.get('#board-breeder').should('be.checked');
        getBreederSelect().should('be.visible');
        cy.get('#custom-name').should('not.exist');
        cy.get('#custom-surname').should('not.exist');
    });

    // ==========================================
    // PHONE NUMBER
    // ==========================================

    it('Should correctly format and validate the public phone number', () => {
        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        getBreederSelect().select('10');

        cy.get('#contact-phone').type('123abc456789').should('have.value', '123 456 789');

        cy.get('#contact-phone').clear().type('123');
        cy.contains('button', 'Zapisz').click();

        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'Numer telefonu musi składać się dokładnie z 9 cyfr.');

        cy.get('[role="dialog"]').should('be.visible');
    });

    // ==========================================
    // CREATE REGISTERED MEMBER
    // ==========================================

    it('Should successfully add a registered breeder to a section board', () => {
        cy.intercept('POST', '**/api/board', (req) => {
            expect(req.body).to.deep.equal({
                role: 'PREZES',
                managedSectionId: 3,
                breederId: 14,
                customName: null,
                customSurname: null,
                contactPhone: '555666777'
            });

            boardMembers = [
                ...boardMembers,
                {
                    id: 99,
                    role: 'PREZES',
                    managedSectionId: 3,
                    managedSectionName: 'Chotków',
                    firstName: 'Marek',
                    lastName: 'Testowy',
                    publicPhoneNumber: '555666777',
                    breederId: 14
                }
            ];

            req.reply({
                statusCode: 201,
                body: ''
            });
        }).as('createBoardMember');

        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        cy.get('#board-type').select('3');
        cy.get('#board-role').should('have.value', 'PREZES');
        getBreederSelect().select('14');
        cy.get('#contact-phone').type('555666777');
        cy.contains('button', 'Zapisz').click();

        cy.wait('@createBoardMember');
        cy.wait('@getBoard');

        cy.get('[role="dialog"]').should('not.exist');

        getBoardSection('Zarząd Sekcji: Chotków').within(() => {
            cy.contains('h3', 'Marek Testowy').should('be.visible');
            cy.contains('Prezes').should('be.visible');
            cy.contains('555 666 777').should('be.visible');
        });
    });

    // ==========================================
    // CREATE EXTERNAL MEMBER
    // ==========================================

    it('Should successfully add a person without an account to the branch board', () => {
        cy.intercept('POST', '**/api/board', (req) => {
            expect(req.body).to.deep.equal({
                role: 'CZLONEK_ZARZADU',
                managedSectionId: null,
                breederId: null,
                customName: 'Tomasz',
                customSurname: 'Zewnętrzny',
                contactPhone: null
            });

            boardMembers = [
                ...boardMembers,
                {
                    id: 100,
                    role: 'CZLONEK_ZARZADU',
                    managedSectionId: null,
                    managedSectionName: null,
                    firstName: 'Tomasz',
                    lastName: 'Zewnętrzny',
                    publicPhoneNumber: null,
                    breederId: null
                }
            ];

            req.reply({
                statusCode: 201,
                body: ''
            });
        }).as('createExternalBoardMember');

        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        cy.contains('label', 'Osoba bez konta').click();
        cy.get('#custom-name').type('Tomasz');
        cy.get('#custom-surname').type('Zewnętrzny');
        cy.contains('button', 'Zapisz').click();

        cy.wait('@createExternalBoardMember');
        cy.wait('@getBoard');

        cy.get('[role="dialog"]').should('not.exist');

        getBoardSection('Zarząd Oddziału:').within(() => {
            cy.contains('h3', 'Tomasz Zewnętrzny').should('be.visible');
            cy.contains('Członek Zarządu').should('be.visible');
        });
    });

    // ==========================================
    // CREATE ERROR
    // ==========================================

    it('Should display an API error when adding a board member fails', () => {
        cy.intercept('POST', '**/api/board', {
            statusCode: 400,
            body: 'To stanowisko (PREZES) jest już zajęte w tym zarządzie.'
        }).as('createBoardMemberError');

        openAddMemberModal();
        cy.wait('@getRegisteredBreeders');

        cy.get('#board-role').select('PREZES');
        getBreederSelect().select('11');
        cy.contains('button', 'Zapisz').click();

        cy.wait('@createBoardMemberError');

        cy.get('[role="alert"]').should('be.visible').and('contain.text', 'To stanowisko (PREZES) jest już zajęte w tym zarządzie.');

        cy.get('[role="dialog"]').should('be.visible');
    });

    // ==========================================
    // EDIT REGISTERED MEMBER
    // ==========================================

    it('Should correctly preload and successfully edit a registered board member', () => {
        cy.intercept('PUT', '**/api/board/1', (req) => {
            expect(req.body).to.deep.equal({
                role: 'WICEPREZES_DS_FINANSOWYCH',
                managedSectionId: null,
                breederId: 10,
                customName: null,
                customSurname: null,
                contactPhone: '999888777'
            });

            boardMembers = boardMembers.map((member) => member.id === 1
                ? {
                    ...member,
                    role: 'WICEPREZES_DS_FINANSOWYCH',
                    publicPhoneNumber: '999888777'
                }
                : member
            );

            req.reply({
                statusCode: 200,
                body: ''
            });
        }).as('updateBoardMember');

        cy.get('button[aria-label="Edytuj Jan Kowalski"]').click();
        cy.wait('@getRegisteredBreeders');

        cy.contains('h2', 'Edytuj członka zarządu').should('be.visible');
        cy.get('#board-type').should('have.value', '0');
        cy.get('#board-role').should('have.value', 'PREZES');
        cy.get('#board-breeder').should('be.checked');
        getBreederSelect().should('have.value', '10');
        cy.get('#contact-phone').should('have.value', '111 222 333');

        cy.get('#board-role').select('WICEPREZES_DS_FINANSOWYCH');
        cy.get('#contact-phone').clear().type('999888777');
        cy.contains('button', 'Zapisz').click();

        cy.wait('@updateBoardMember');
        cy.wait('@getBoard');

        cy.get('[role="dialog"]').should('not.exist');

        getMemberCard('Jan Kowalski').within(() => {
            cy.contains('V-ce Prezes ds. finansowych').should('be.visible');
            cy.contains('999 888 777').should('be.visible');
        });
    });

    // ==========================================
    // EDIT EXTERNAL MEMBER
    // ==========================================

    it('Should correctly preload a person without an account when editing', () => {
        cy.get('button[aria-label="Edytuj Adam Nowak"]').click();
        cy.wait('@getRegisteredBreeders');

        cy.contains('h2', 'Edytuj członka zarządu').should('be.visible');

        cy.get('#board-breeder').should('not.be.checked');
        getBreederSelect().should('not.exist');

        cy.get('#custom-name').should('be.visible').and('have.value', 'Adam');
        cy.get('#custom-surname').should('be.visible').and('have.value', 'Nowak');
        cy.get('#board-role').should('have.value', 'WICEPREZES_DS_LOTOWYCH');

        cy.contains('button', 'Anuluj').click();
        cy.get('[role="dialog"]').should('not.exist');
    });

    // ==========================================
    // CANCEL DELETE
    // ==========================================

    it('Should cancel board member deletion without removing the member', () => {
        cy.get('button[aria-label="Usuń Adam Nowak"]').click();

        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Usuń członka zarządu').should('be.visible');
            cy.contains('Czy na pewno chcesz usunąć tę osobę ze stanowiska w zarządzie?').should('be.visible');
            cy.contains('button', 'Anuluj').click();
        });

        cy.get('[role="dialog"]').should('not.exist');
        getMemberCard('Adam Nowak').should('be.visible');
    });

    // ==========================================
    // DELETE MEMBER
    // ==========================================

    it('Should successfully delete a board member', () => {
        cy.intercept('DELETE', '**/api/board/2', (req) => {
            boardMembers = boardMembers.filter((member) => member.id !== 2);

            req.reply({
                statusCode: 204
            });
        }).as('deleteBoardMember');

        cy.get('button[aria-label="Usuń Adam Nowak"]').click();

        cy.get('[role="dialog"]').within(() => {
            cy.contains('h2', 'Usuń członka zarządu').should('be.visible');
            cy.contains('button', 'Potwierdź').click();
        });

        cy.wait('@deleteBoardMember');
        cy.wait('@getBoard');

        cy.get('[role="dialog"]').should('not.exist');
        cy.contains('h3', 'Adam Nowak').should('not.exist');

        getMemberCard('Jan Kowalski').should('be.visible');
        getMemberCard('Zbigniew Wiśniewski').should('be.visible');
    });

    // ==========================================
    // DELETE ERROR
    // ==========================================

    it('Should display an error modal when board member deletion fails', () => {
        cy.intercept('DELETE', '**/api/board/2', {
            statusCode: 500,
            body: 'Internal Server Error'
        }).as('deleteBoardMemberError');

        cy.get('button[aria-label="Usuń Adam Nowak"]').click();

        cy.get('[role="dialog"]').within(() => {
            cy.contains('button', 'Potwierdź').click();
        });

        cy.wait('@deleteBoardMemberError');

        cy.get('[role="dialog"]').should('be.visible').within(() => {
            cy.contains('h2', 'Błąd').should('be.visible');
            cy.contains('Wystąpił błąd podczas usuwania. Spróbuj ponownie.').should('be.visible');
            cy.contains('button', 'OK').click();
        });

        cy.get('[role="dialog"]').should('not.exist');
        getMemberCard('Adam Nowak').should('be.visible');
    });
});