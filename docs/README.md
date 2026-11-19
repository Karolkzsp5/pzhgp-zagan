# Dokumentacja projektu PZHGP Żagań

Dokumentacja aplikacji webowej Polskiego Związku Hodowców Gołębi Pocztowych, Oddział Żagań. Składa się z części technicznej, przeznaczonej dla programisty lub osoby oceniającej projekt, oraz z instrukcji użytkownika.

## Dokumentacja techniczna

1. [Architektura](architektura.md): warstwy systemu, technologie, struktura kodu i przepływ żądania.
2. [Funkcje i reguły biznesowe](funkcje.md): moduły aplikacji, role, statusy kont i zgłoszeń, powiadomienia, analiza lotów GPX.
3. [Baza danych](baza-danych.md): model danych, tabele i relacje, migracje Flyway.
4. [API REST](api.md): konwencje, kody odpowiedzi i lista wszystkich endpointów.
5. [Bezpieczeństwo](bezpieczenstwo.md): uwierzytelnianie JWT, CSRF, CORS, uprawnienia, limity prób i ochrona danych.
6. [Integracja z Cloudflare Turnstile](integracja-turnstile.md): komunikacja z zewnętrznym API krok po kroku.
7. [Testy](testy.md): testy jednostkowe, integracyjne i end-to-end.
8. [Uruchomienie i wdrożenie](uruchomienie.md): uruchomienie lokalne, Docker, zmienne środowiskowe, wdrożenie na serwer.

## Instrukcja użytkownika

9. [Instrukcja użytkownika](instrukcja-uzytkownika.md): obsługa aplikacji przez gościa, hodowcę, moderatora i administratora.

## Konwencje dokumentacji

- **Format:** pliki Markdown; diagramy są zapisane w języku Mermaid i GitHub wyświetla je jako rysunki.
- **Ścieżki w kodzie:** klasy backendu są podawane względem katalogu `backend/src/main/java/com/pzhgp/backend/`, np. `service/TurnstileService.java`, a pliki frontendu względem `frontend/src/app/`, np. `services/authService.ts`. Pozostałe ścieżki są podawane od głównego katalogu repozytorium.
- **Nazwy w interfejsie** (przyciski, zakładki) są zapisane tak, jak widać je w aplikacji, np. **Dodaj ogłoszenie**.
