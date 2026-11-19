# Uruchomienie i wdrożenie

Aplikację można uruchomić na dwa sposoby:

| Sposób | Kiedy | Co trzeba zainstalować |
|---|---|---|
| **A. Docker** | pokaz aplikacji, szybkie uruchomienie całości jednym poleceniem | Docker Desktop (Windows, macOS) albo Docker Engine z Compose (Linux) |
| **B. Lokalnie** | praca nad kodem w IDE (IntelliJ IDEA, WebStorm) | JDK 25, Node.js 26 LTS, PostgreSQL 16 lub nowszy |

W obu przypadkach aplikacja działa pod adresem `http://localhost:3000`, a backend pod `http://localhost:8080`.

## A. Uruchomienie w Dockerze

`docker-compose.yml` w głównym katalogu uruchamia cztery kontenery:

| Kontener | Obraz | Rola | Adres na komputerze |
|---|---|---|---|
| `db` | `postgres:18` | baza danych; dane w wolumenie `db-data` | `localhost:5433` (np. dla pgAdmin) |
| `backend` | budowany z `backend/Dockerfile` | API REST (Java 25) | `http://localhost:8080` |
| `frontend` | budowany z `frontend/Dockerfile` | aplikacja Next.js (Node.js 26) | `http://localhost:3000` |
| `mailpit` | `axllent/mailpit` | przechwytuje e-maile wysyłane przez aplikację | `http://localhost:8025` |

Oba obrazy aplikacji budują się dwuetapowo:
- **backend:** budowanie Mavenem na pełnym JDK, uruchamianie na samym środowisku uruchomieniowym Javy (JRE);
- **frontend:** `next build`, a potem uruchomienie samodzielnego serwera Next.js (tryb `standalone`, bez pełnego `node_modules`).

Obraz końcowy nie zawiera więc narzędzi do budowania. Backend startuje dopiero wtedy, gdy baza zgłosi gotowość (healthcheck).

### Kroki

1. W głównym katalogu projektu skopiuj `.env.example` do `.env` i uzupełnij:
   - `DB_PASSWORD`: dowolne hasło do bazy w kontenerze;
   - `JWT_SECRET`: losowy ciąg, co najmniej 32 znaki;
   - `APP_HOST`: zostaw `localhost` albo wpisz adres IP komputera (patrz [Dostęp z innych urządzeń](#dostęp-z-innych-urządzeń-w-sieci-lokalnej)).
2. (Opcjonalnie) przenieś istniejące dane. Kopię bazy w formacie SQL włóż do `docker/db-import/`:

   ```bash
   pg_dump -U postgres -d pzhgp_zagan --no-owner --no-privileges -f docker/db-import/pzhgp_zagan.sql
   ```

   Plik zostanie wczytany automatycznie przy **pierwszym** starcie pustej bazy w kontenerze. Pliki `.sql` z tego katalogu nie trafiają do repozytorium.
3. Uruchom w głównym katalogu:

   ```bash
   docker compose up --build
   ```

   Pierwsze budowanie trwa kilka minut, bo pobiera zależności. Bez kopii danych Flyway tworzy pustą bazę z czterema sekcjami.
4. Otwórz `http://localhost:3000`.

Przed uruchomieniem zatrzymaj backend i frontend uruchomione lokalnie, bo zajmują te same porty (8080 i 3000).

### Najważniejsze polecenia

| Polecenie | Działanie |
|---|---|
| `docker compose up --build` | buduje obrazy i uruchamia całość (logi w terminalu, `Ctrl+C` zatrzymuje) |
| `docker compose up -d` | uruchamia w tle |
| `docker compose down` | zatrzymuje i usuwa kontenery; **dane bazy zostają** w wolumenie |
| `docker compose down -v` | zatrzymuje kontenery i **usuwa bazę** (np. żeby wczytać nowszą kopię danych) |
| `docker compose logs -f backend` | logi backendu na bieżąco |
| `docker compose restart backend` | restart backendu (zeruje też limity prób) |
| `docker image prune` | usuwa nieużywane obrazy po wielu przebudowach |

Po zmianach w kodzie albo w `APP_HOST` trzeba przebudować obrazy (`docker compose up --build`). Adres backendu jest wbudowywany w aplikację frontendu podczas budowania.

### Hasło bazy w Dockerze

Obraz PostgreSQL ustawia hasło z `DB_PASSWORD` **tylko przy pierwszym utworzeniu bazy**. Późniejsza zmiana `DB_PASSWORD` w `.env` nie zmienia hasła w istniejącej bazie, więc backend nie połączy się z bazą (błąd `password authentication failed for user "pzhgp"`). Naprawa:
- **przywróć poprzednie hasło** w `.env`, albo
- **zmień hasło w bazie** (dane zostają):

  ```bash
  docker compose up -d db
  docker compose exec db psql -U pzhgp -d pzhgp_zagan -c "ALTER USER pzhgp WITH PASSWORD 'nowe_haslo_z_env';"
  docker compose up -d
  ```

- albo **utwórz bazę od nowa:** `docker compose down -v`, potem `docker compose up --build`.

## B. Uruchomienie lokalne

### 1. Baza danych

Utwórz bazę aplikacji i bazę do testów, jako ten sam użytkownik, który będzie w `DB_USERNAME`:

```sql
CREATE DATABASE pzhgp_zagan;
CREATE DATABASE pzhgp_zagan_test;
```

Tabele tworzą migracje Flyway przy pierwszym uruchomieniu backendu. Baza utworzona przed wprowadzeniem Flyway zachowuje dane (rozdział [Baza danych](baza-danych.md#baza-utworzona-przed-wprowadzeniem-flyway-baseline)).

### 2. Backend

```bash
cd backend
cp .env.example .env        # Windows: copy .env.example .env
```

Uzupełnij w `backend/.env` co najmniej `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` i `JWT_SECRET`. Pozostałe wartości są przygotowane do pracy lokalnej. Następnie uruchom:

```bash
./mvnw spring-boot:run      # Linux, macOS
mvnw.cmd spring-boot:run    # Windows
```

Można też uruchomić klasę `BackendApplication` w IntelliJ IDEA:
- SDK projektu: Java 25;
- katalog roboczy: `backend`, żeby został wczytany plik `.env`;
- po zmianach w `pom.xml`: **Reload All Maven Projects**.

### 3. Frontend

```bash
cd frontend
cp .env.local.example .env.local    # Windows: copy .env.local.example .env.local
npm ci
npm run dev
```

Aplikacja działa pod `http://localhost:3000`. Zmienne z `.env.local` są wczytywane przy starcie, więc po ich zmianie trzeba ponownie uruchomić `npm run dev`.

`npm ci` w npm 11 domyślnie nie uruchamia skryptów instalacyjnych paczek. Wyjątkiem jest Cypress, któremu pozwala na to pole `allowScripts` w `package.json`, dzięki czemu Cypress pobiera swój program. Gdyby zgłosił jego brak, uruchom `npx cypress install`.

### 4. E-maile i CAPTCHA

- **CAPTCHA:** pliki przykładowe zawierają oficjalne klucze testowe Cloudflare Turnstile, które zawsze przechodzą weryfikację. Konto w Cloudflare nie jest potrzebne, ale potrzebny jest dostęp do internetu.
- **E-maile:** domyślne ustawienia (`localhost:1025`) pasują do Mailpit. Najprościej uruchomić go w Dockerze:

  ```bash
  docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
  ```

  Wiadomości są widoczne pod `http://localhost:8025`. Bez Mailpit aplikacja działa, tylko e-maile nie zostaną wysłane, a błąd trafi do logu backendu.

## Pierwsze konto administratora

Aplikacja nie tworzy konta administratora automatycznie:
1. Zarejestruj konto przez formularz.
2. Nadaj mu uprawnienia w bazie (pgAdmin albo `psql`):

   ```sql
   UPDATE breeders
   SET role = 'ADMINISTRATOR', status = 'ACTIVE'
   WHERE email = 'twoj.adres@example.com';
   ```

W Dockerze: `docker compose exec db psql -U pzhgp -d pzhgp_zagan -c "UPDATE breeders SET role = 'ADMINISTRATOR', status = 'ACTIVE' WHERE email = 'twoj.adres@example.com';"`.

Kolejne konta akceptuje już administrator w panelu aplikacji.

## Dostęp z innych urządzeń w sieci lokalnej

Żeby otworzyć aplikację np. na telefonie w tej samej sieci Wi-Fi, w miejsce `localhost` wpisz adres IP komputera (np. `192.168.0.100`). Ciasteczka logowania i CSRF działają tylko z jednym adresem, więc **na komputerze też trzeba otwierać aplikację pod tym adresem**, a nie pod `localhost`.

| Tryb | Co ustawić |
|---|---|
| Docker | `APP_HOST=192.168.0.100` w `.env`, potem `docker compose up --build` |
| Lokalnie | `backend/.env`: `CORS_ALLOWED_ORIGINS=http://192.168.0.100:3000` i `FRONTEND_URL=http://192.168.0.100:3000`; `frontend/.env.local`: `NEXT_PUBLIC_API_URL=http://192.168.0.100:8080`; ponowne uruchomienie backendu i frontendu |

Jeśli inne urządzenia nie mogą się połączyć, zapora Windows blokuje porty 3000 i 8080. Zezwól na połączenia przychodzące dla Java i Node.js albo dla „Docker Desktop Backend” w sieciach prywatnych. Adres IP komputera może się zmienić po zmianie sieci; najwygodniej zarezerwować go w ustawieniach routera (DHCP).

## Zmienne środowiskowe

### Backend (`backend/.env`)

| Zmienna | Opis | Wartość domyślna |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | połączenie z bazą PostgreSQL | – |
| `TEST_DB_URL` | baza do testów; nazwa musi kończyć się na `_test` | `jdbc:postgresql://localhost:5432/pzhgp_zagan_test` |
| `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` | dane logowania do bazy testowej | `DB_USERNAME`, `DB_PASSWORD` |
| `JWT_SECRET` | klucz podpisujący tokeny logowania (co najmniej 32 znaki) | – |
| `CORS_ALLOWED_ORIGINS` | adresy frontendu, z których przeglądarka może wysyłać zapytania (po przecinku) | `http://localhost:3000` |
| `AUTH_COOKIE_SECURE` | flaga `Secure` ciasteczka logowania; `true` na serwerze z HTTPS | `false` |
| `FORWARD_HEADERS_STRATEGY` | `native`, gdy backend działa za serwerem proxy (np. nginx) | `none` |
| `TURNSTILE_SECRET_KEY` | klucz tajny Cloudflare Turnstile | klucz testowy w `.env.example` |
| `MAIL_HOST`, `MAIL_PORT` | serwer SMTP | `localhost`, `1025` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | dane logowania do serwera SMTP | puste |
| `MAIL_FROM` | adres nadawcy wiadomości | `no-reply@pzhgp-zagan.local` |
| `FRONTEND_URL` | adres frontendu wstawiany do linków w e-mailach | `http://localhost:3000` |

### Frontend (`frontend/.env.local`)

| Zmienna | Opis |
|---|---|
| `NEXT_PUBLIC_API_URL` | adres backendu widziany z przeglądarki, np. `http://localhost:8080` |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | klucz witryny Cloudflare Turnstile (klucz testowy w `.env.local.example`) |

Zmienne `NEXT_PUBLIC_*` są wbudowywane w kod dla przeglądarki podczas `npm run build`, więc w wersji produkcyjnej trzeba je ustawić przed budowaniem.

### Docker Compose (`.env` w głównym katalogu)

| Zmienna | Opis | Wartość domyślna |
|---|---|---|
| `APP_HOST` | adres, pod którym otwiera się aplikację (`http://APP_HOST:3000`) | `localhost` |
| `DB_PASSWORD` | hasło bazy w kontenerze; ustawiane tylko przy pierwszym utworzeniu bazy | wymagane |
| `JWT_SECRET` | klucz podpisujący tokeny logowania | wymagane |
| `TURNSTILE_SECRET_KEY`, `TURNSTILE_SITE_KEY` | klucze Cloudflare Turnstile | klucze testowe |

Pliki `.env` i `.env.local` nie trafiają do repozytorium. Warto przechowywać ich kopię w bezpiecznym miejscu, np. w menedżerze haseł.

## Wdrożenie na serwer

Przy uruchamianiu aplikacji na serwerze dostępnym z internetu:

- **HTTPS:** aplikacja powinna działać przez HTTPS, z `AUTH_COOKIE_SECURE=true`.
- **Jeden adres hosta dla frontendu i backendu:** frontend odczytuje token CSRF z ciasteczka ustawianego przez backend, a przeglądarka udostępnia je tylko stronom z tego samego hosta.
  - Najlepiej, żeby serwer proxy (np. nginx) przekazywał zapytania `/api/` do backendu, a pozostałe do frontendu. `NEXT_PUBLIC_API_URL` to wtedy publiczny adres aplikacji (np. `https://zagan.example.pl`).
  - Przy backendzie pod osobną domeną (np. `api.zagan.example.pl`) zapisywanie danych nie zadziała.
- **Adresy:** `CORS_ALLOWED_ORIGINS` i `FRONTEND_URL` muszą wskazywać publiczny adres frontendu.
- **Proxy:** za serwerem proxy ustaw `FORWARD_HEADERS_STRATEGY=native`, żeby limity prób widziały adres klienta.
- **CAPTCHA:** zamiast kluczy testowych użyj kluczy własnego widżetu Turnstile i dodaj domenę aplikacji w panelu Cloudflare.
- **Poczta:** ustaw dane prawdziwego serwera SMTP (`MAIL_*`) i adres nadawcy zgodny ze skrzynką.
- **Nagłówki bezpieczeństwa stron:** dodaj w serwerze proxy m.in. `Content-Security-Policy` i `X-Frame-Options` (rozdział [Bezpieczeństwo](bezpieczenstwo.md#znane-ograniczenia)).
- **Monitoring:** `GET /actuator/health` zwraca stan aplikacji.
- **Kopie zapasowe:** regularny `pg_dump` bazy (rozdział [Baza danych](baza-danych.md#kopia-zapasowa-i-odtworzenie)).

## Rozwiązywanie problemów

| Objaw | Przyczyna i rozwiązanie |
|---|---|
| Backend nie startuje: `password authentication failed` (Docker) | hasło w `.env` różni się od hasła bazy w kontenerze; patrz [Hasło bazy w Dockerze](#hasło-bazy-w-dockerze) |
| Backend nie startuje: `Port 8080 was already in use` | działa inny backend (lokalny albo w Dockerze); zatrzymaj go |
| Backend nie startuje: błąd walidacji schematu (`Schema-validation: missing column`) | encja nie zgadza się z bazą; brakuje migracji Flyway dla zmiany w encji |
| Backend nie startuje: `Validate failed: Migrations have failed validation` | zmieniono plik migracji, który został już wykonany; przywróć jego treść, a zmianę zapisz w nowej migracji |
| „Nie można odczytać tokenu CSRF” albo nie da się nic zapisać | aplikacja jest otwarta pod innym adresem niż ustawiony w konfiguracji (np. `localhost` zamiast IP); otwórz ją pod adresem z `NEXT_PUBLIC_API_URL` / `APP_HOST` |
| „Zbyt wiele prób…” (`429`) | przekroczony limit prób; odczekaj albo uruchom backend ponownie, co zeruje limity |
| „Weryfikacja CAPTCHA jest chwilowo niedostępna” | brak internetu albo błędny `TURNSTILE_SECRET_KEY` (szczegóły w logu backendu) |
| Testy backendu nie startują: `database "pzhgp_zagan_test" does not exist` | utwórz bazę testową: `CREATE DATABASE pzhgp_zagan_test;` |
| Testy backendu nie startują: „Testy mogą działać tylko na osobnej bazie…” | `TEST_DB_URL` wskazuje bazę, której nazwa nie kończy się na `_test`; to zabezpieczenie przed skasowaniem prawdziwych danych |
| Docker: `toomanyrequests` przy pobieraniu obrazów | limit pobrań Docker Hub bez logowania; zaloguj się w Docker Desktop darmowym kontem |
| Cypress: brak programu Cypress | uruchom `npx cypress install` |
