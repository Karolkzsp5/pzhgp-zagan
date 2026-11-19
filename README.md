# PZHGP Żagań – aplikacja webowa oddziału

Aplikacja webowa dla **Polskiego Związku Hodowców Gołębi Pocztowych, Oddział Żagań**, przygotowana w ramach pracy dyplomowej. Łączy w jednym miejscu sprawy, które w oddziale załatwia się zwykle telefonicznie, na papierze albo w kilku różnych serwisach:
- ogłoszenia zarządu;
- plany i wyniki lotów;
- analizę tras gołębi z plików GPX;
- forum hodowców;
- zgłoszenia znalezionych gołębi;
- obsługę kont członków.

Oddział obejmuje cztery sekcje: **Żagań, Wymiarki, Chotków i Kożuchów**.

## Dokumentacja

Pełna dokumentacja jest w katalogu [`docs/`](docs/README.md):

| Dokument | Zawartość |
|---|---|
| [Architektura](docs/architektura.md) | warstwy systemu, technologie, struktura kodu, przepływ żądania |
| [Funkcje i reguły biznesowe](docs/funkcje.md) | moduły aplikacji, role, statusy, powiadomienia, analiza lotów GPX |
| [Baza danych](docs/baza-danych.md) | model danych, tabele i relacje, migracje Flyway, kopia zapasowa |
| [API REST](docs/api.md) | konwencje, kody odpowiedzi, lista wszystkich endpointów |
| [Bezpieczeństwo](docs/bezpieczenstwo.md) | JWT w ciasteczku, CSRF, CORS, uprawnienia, limity prób, ochrona danych |
| [Integracja z Cloudflare Turnstile](docs/integracja-turnstile.md) | komunikacja z zewnętrznym API krok po kroku |
| [Testy](docs/testy.md) | testy jednostkowe, integracyjne i end-to-end |
| [Uruchomienie i wdrożenie](docs/uruchomienie.md) | Docker, uruchomienie lokalne, zmienne środowiskowe, wdrożenie na serwer |
| [Instrukcja użytkownika](docs/instrukcja-uzytkownika.md) | obsługa aplikacji przez gościa, hodowcę, moderatora i administratora |

## Funkcje

- **Ogłoszenia zarządu** na stronie głównej, z edytorem tekstu sformatowanego i przypinaniem.
- **Konta użytkowników:**
  - rejestracja z CAPTCHA i akceptacją przez administratora;
  - logowanie z opcją „Zapamiętaj mnie”;
  - odzyskiwanie hasła e-mailem;
  - ustawienia konta, w tym wylogowanie ze wszystkich urządzeń i pobranie swoich danych (RODO).
- **Panel administratora:** akceptowanie, blokowanie i zmiana ról kont, wyszukiwanie hodowców.
- **Plany i wyniki lotów** z podziałem na gołębie dorosłe i młode; wyniki oddziałowe i sekcyjne z podglądem w przeglądarce.
- **Mapy lotów:** analiza tras z plików GPX:
  - dystans, czas oraz prędkości w m/min;
  - wysokość;
  - mapa z odtwarzaniem lotu i wykres przebiegu.
- **Forum** z kategoriami, wątkami, odpowiedziami i moderacją.
- **Znalezione gołębie:** publiczny formularz w trzech językach (PL, EN, DE) i panel zgłoszeń dla administratora.
- **Zarząd** oddziału i sekcji z danymi kontaktowymi oraz mapa **pogody** z serwisu Windy.
- **Powiadomienia** w aplikacji o nowych treściach i zmianach na koncie.

Opis wszystkich funkcji jest w rozdziale [Funkcje i reguły biznesowe](docs/funkcje.md), a role i uprawnienia w jego części [Role i uprawnienia](docs/funkcje.md#role-i-uprawnienia).

## Technologie

| Warstwa | Technologie |
|---|---|
| Backend | Java 25, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Mail, Actuator), Hibernate 7, Flyway, JJWT, jsoup |
| Baza danych | PostgreSQL |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, TipTap, Leaflet, DOMPurify |
| Testy | JUnit 6, Mockito, Spring MockMvc, JaCoCo, Cypress 15 |
| Uruchomienie | Docker Compose (PostgreSQL, backend, frontend, Mailpit) |
| Usługi zewnętrzne | Cloudflare Turnstile (CAPTCHA), serwer SMTP, OpenStreetMap, Windy |

## Szybki start

### Docker (najprostszy sposób)

Wymagany jest Docker Desktop albo Docker Engine z Compose.

```bash
cp .env.example .env          # uzupełnij DB_PASSWORD i JWT_SECRET (min. 32 znaki)
docker compose up --build
```

| Adres | Usługa |
|---|---|
| `http://localhost:3000` | aplikacja |
| `http://localhost:8080` | API (backend) |
| `http://localhost:8025` | Mailpit: podgląd e-maili wysłanych przez aplikację |

Flyway tworzy przy pierwszym starcie pustą bazę z czterema sekcjami oddziału.

### Lokalnie (praca nad kodem)

Wymagane są JDK 25, Node.js 26 LTS i PostgreSQL 16 lub nowszy.

1. **Bazy danych:**

   ```sql
   CREATE DATABASE pzhgp_zagan;
   CREATE DATABASE pzhgp_zagan_test;   -- dla testów backendu
   ```

2. **Backend:**

   ```bash
   cd backend
   cp .env.example .env               # uzupełnij DB_URL, DB_USERNAME, DB_PASSWORD i JWT_SECRET
   ./mvnw spring-boot:run             # Windows: mvnw.cmd spring-boot:run
   ```

3. **Frontend** (w drugim terminalu, z głównego katalogu):

   ```bash
   cd frontend
   cp .env.local.example .env.local
   npm ci
   npm run dev
   ```

Pliki `.env.example` zawierają oficjalne klucze testowe Cloudflare Turnstile, więc CAPTCHA działa bez konta w Cloudflare.

Szczegóły, opis zmiennych środowiskowych i rozwiązywanie problemów są w rozdziale [Uruchomienie i wdrożenie](docs/uruchomienie.md).

### Pierwsze konto administratora

Aplikacja nie tworzy administratora automatycznie. Zarejestruj się przez formularz, a potem nadaj sobie uprawnienia w bazie:

```sql
UPDATE breeders
SET role = 'ADMINISTRATOR', status = 'ACTIVE'
WHERE email = 'twoj.adres@example.com';
```

W Dockerze to samo polecenie wykonuje się przez `docker compose exec` (rozdział [Pierwsze konto administratora](docs/uruchomienie.md#pierwsze-konto-administratora)). Kolejne konta akceptuje już administrator w panelu aplikacji.

## Testy

```bash
(cd backend && ./mvnw verify)                                           # testy JUnit i raport pokrycia JaCoCo
(cd frontend && npm run lint && npm run typecheck && npm run cypress:run) # Cypress przy działającym npm run dev
```

Projekt ma ponad 600 testów backendu (jednostkowych i integracyjnych na osobnej bazie `pzhgp_zagan_test`) oraz ponad 280 testów end-to-end w Cypress. Szczegóły są w rozdziale [Testy](docs/testy.md).

## Struktura repozytorium

```
pzhgp-zagan/
├── backend/              # API REST (Spring Boot), migracje Flyway, testy JUnit
├── frontend/             # interfejs użytkownika (Next.js), testy Cypress
├── docker/db-import/     # opcjonalna kopia bazy wczytywana przy pierwszym starcie w Dockerze
├── docs/                 # dokumentacja techniczna i instrukcja użytkownika
├── docker-compose.yml
└── .env.example          # ustawienia dla Docker Compose
```

## Licencja

Projekt jest udostępniony na licencji MIT – szczegóły w pliku [LICENSE.md](LICENSE.md).
