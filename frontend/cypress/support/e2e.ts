// Plik wczytywany przed każdym testem: rejestruje własne komendy i ustawia token CSRF.
import './commands'

Cypress.on('window:before:load', (win) => {
    win.document.cookie = 'XSRF-TOKEN=cypress-test-csrf; Path=/; SameSite=Lax';
});