# Testy

Projekt ma trzy rodzaje testów automatycznych:

| Rodzaj | Narzędzia | Liczba | Co sprawdza |
|---|---|---|---|
| Testy jednostkowe backendu | JUnit 6, Mockito | 342 | logikę serwisów, analizę GPX, narzędzia, w oderwaniu od bazy i sieci |
| Testy integracyjne backendu | Spring Boot Test, MockMvc, PostgreSQL | 287 | całe żądania HTTP: bezpieczeństwo, walidację, uprawnienia, zapis w bazie, kody odpowiedzi |
| Testy end-to-end frontendu | Cypress | 284 (18 plików) | strony w przeglądarce: wygląd, formularze, komunikaty, nawigację, uprawnienia w interfejsie |

Dodatkowo kod frontendu sprawdzają ESLint (`npm run lint`) i kompilator TypeScript (`npm run typecheck`).

## Backend

### Uruchomienie

```bash
cd backend
./mvnw test       # wszystkie testy (Windows: mvnw.cmd test)
./mvnw verify     # testy i raport pokrycia kodu: target/site/jacoco/index.html
```

Testy można też uruchamiać w IntelliJ IDEA (prawy przycisk na katalogu `src/test/java` → **Run 'All Tests'**). Katalogiem roboczym musi być `backend`, żeby został wczytany plik `.env`.

### Baza testowa

Testy integracyjne działają na **prawdziwym PostgreSQL**, na osobnej bazie `pzhgp_zagan_test`. Bazę trzeba utworzyć raz:

```sql
CREATE DATABASE pzhgp_zagan_test;
```

| Mechanizm | Działanie |
|---|---|
| Konfiguracja | `backend/src/test/resources/application-test.properties`: adres z `TEST_DB_URL` (domyślnie `jdbc:postgresql://localhost:5432/pzhgp_zagan_test`), login i hasło z `TEST_DB_USERNAME` i `TEST_DB_PASSWORD` albo z `DB_USERNAME` i `DB_PASSWORD` |
| Czysta baza przy każdym uruchomieniu | `support/TestFlywayConfig.java`: przed testami Flyway czyści bazę testową i wykonuje wszystkie migracje od zera, więc testy sprawdzają przy okazji same migracje |
| Izolacja testów | klasy testów integracyjnych mają adnotację `@Transactional`, więc zmiany każdego testu są na końcu wycofywane |
| Ochrona prawdziwych danych | `support/TestDatabaseGuard.java` zatrzymuje testy jeszcze przed połączeniem z bazą, jeśli nazwa bazy nie kończy się na `_test`. Czyszczenie bazy jest dozwolone tylko w profilu testowym (`spring.flyway.clean-disabled=false`) |

### Testy jednostkowe

Testy jednostkowe zastępują repozytoria i inne zależności atrapami Mockito i sprawdzają logikę bez bazy danych.

| Obszar | Klasy testów |
|---|---|
| Konta i logowanie | `BreederServiceTest`, `AdminServiceTest`, `JwtServiceTest`, `PasswordResetServiceTest`, `AuthRateLimiterTest`, `AuthCookieServiceTest` |
| Treści | `AnnouncementServiceTest`, `ForumCategoryServiceTest`, `ForumThreadServiceTest`, `ForumPostServiceTest`, `RichTextSanitizerTest` |
| Loty | `FlightPlanServiceTest`, `FlightResultServiceTest`, `PigeonFlightServiceTest`, testy w pakiecie `service/gpx` |
| Pozostałe | `BoardMemberServiceTest`, `FoundPigeonServiceTest`, `RingNumberNormalizerTest`, `NotificationServiceTest`, `MailServiceTest`, `TurnstileServiceTest`, `TestDatabaseGuardTest` |

**Analiza plików GPX** ma osobne testy parsera, analizy lotu, obliczeń geograficznych i upraszczania trasy. Korzystają z plików w `backend/src/test/resources/gpx/`:
- prawdziwy zapis lotu konkursowego z nadajnika SkyLeader;
- pliki bez punktów i bez znaczników czasu;
- plik z próbą ataku XXE.

Osobne testy sprawdzają, że analiza bardzo dużych plików z powtarzającymi się znacznikami czasu kończy się szybko.

**Zewnętrzne API Cloudflare** testuje `TurnstileServiceTest` z `MockRestServiceServer`, który udaje serwer Cloudflare (rozdział [Integracja z Cloudflare Turnstile](integracja-turnstile.md#testy-integracji)).

### Testy integracyjne

Każdy obszar API ma klasę testów integracyjnych (`controller/*IntegrationTest.java`). Testy uruchamiają pełny kontekst Spring Boot z prawdziwą bazą i wysyłają żądania HTTP przez MockMvc. Sprawdzają m.in.:

- **uprawnienia:** czy każda rola dostaje właściwy kod (`200`, `401`, `403`) dla każdej operacji, także dla cudzych treści;
- **walidację:** komunikaty dla błędnych danych, przycinanie spacji, limity długości;
- **bezpieczeństwo:** wymaganie tokenu CSRF, unieważnianie sesji po zmianie hasła, blokadę konta w trakcie sesji, czas życia ciasteczka;
- **zapis i konflikty:** unikalność e-maila, telefonu i roku planu, zmiany statusów zgłoszeń, usuwanie z zależnościami;
- **obsługę błędów HTTP:** nieznane adresy (`404`), złe metody (`405`), złe typy treści (`415`) i to, że komunikaty błędów są zawsze zwykłym tekstem.

Usługi zewnętrzne są zastąpione atrapami:
- weryfikacja CAPTCHA przez `@MockitoBean` na `TurnstileService`;
- wysyłka e-maili przez atrapę `MailService`.

Testy limitów prób korzystają z przesuwanego zegara (`support/MutableClock.java`), żeby nie czekać godzinę.

### Pokrycie kodu

`./mvnw verify` tworzy raport JaCoCo w `backend/target/site/jacoco/index.html`. Raport pokazuje, które linie i gałęzie kodu zostały wykonane przez testy.

## Frontend (Cypress)

### Uruchomienie

```bash
cd frontend
npm run dev              # w osobnym terminalu: frontend musi działać na http://localhost:3000
npm run cypress:open     # testy w oknie Cypress, z podglądem przeglądarki
npm run cypress:run      # wszystkie testy w trybie bez okna
```

**Backend nie musi działać.** Testy podstawiają odpowiedzi API przez `cy.intercept`, więc sprawdzają frontend niezależnie od danych w bazie.

### Zakres

| Plik | Strona |
|---|---|
| `auth/login.cy.ts`, `auth/register.cy.ts`, `auth/forgotPassword.cy.ts`, `auth/resetPassword.cy.ts` | logowanie, rejestracja, odzyskiwanie hasła |
| `homePage.cy.ts` | strona główna i ogłoszenia |
| `adminPanel.cy.ts` | panel administratora |
| `settingsPage.cy.ts` | ustawienia konta |
| `forum/forumPage.cy.ts`, `forum/forumCategoryPage.cy.ts`, `forum/forumThreadPage.cy.ts` | forum |
| `flight-plans/flightPlansPage.cy.ts`, `flight-plans/flightPlanDetailsPage.cy.ts` | plany i wyniki lotów, podgląd wyników |
| `flights/flightsPage.cy.ts`, `flights/flightDetailsPage.cy.ts` | mapy lotów GPX |
| `found-pigeons/foundPigeonsPage.cy.ts`, `found-pigeons/foundPigeonsAdminPage.cy.ts` | formularz i panel zgłoszeń znalezionych gołębi |
| `boardPage.cy.ts` | zarząd |
| `weatherPage.cy.ts` | pogoda |

### Własne polecenia (`cypress/support/commands.ts`)

| Polecenie | Działanie |
|---|---|
| `cy.visitAsUser(url, user)` | otwiera stronę jako zalogowany użytkownik o podanej roli (podstawia `GET /api/auth/me`) |
| `cy.mockTurnstile(token)` | podstawia widżet Turnstile, który od razu zwraca podany token albo żadnego |
| `cy.mockNavbarNotifications()` | podstawia powiadomienia w pasku nawigacji |
| `cy.waitForHydration()` | czeka, aż React przejmie stronę, zanim test zacznie klikać |
| `cy.checkLoggedInNavbar()`, `cy.checkLoggedOutNavbar()`, `cy.checkFooter()` | sprawdzają pasek nawigacji i stopkę |

## Przed wysłaniem zmian

Polecenia wykonuje się z głównego katalogu repozytorium:

```bash
(cd backend && ./mvnw verify)
(cd frontend && npm run lint && npm run typecheck && npm run build)
(cd frontend && npm run cypress:run)     # przy działającym npm run dev
```
