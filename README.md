# PZHGP Żagań – aplikacja webowa oddziału

Aplikacja webowa dla **Polskiego Związku Hodowców Gołębi Pocztowych, Oddział Żagań**, przygotowana w ramach pracy dyplomowej. Łączy w jednym miejscu sprawy, które w oddziale załatwia się zwykle telefonicznie, na papierze albo w kilku różnych serwisach: ogłoszenia zarządu, plany i wyniki lotów, analizę tras gołębi z plików GPX, forum hodowców, zgłoszenia znalezionych gołębi i obsługę kont członków.

Oddział obejmuje cztery sekcje: **Żagań, Wymiarki, Chotków i Kożuchów**.

## Spis treści

- [Funkcje](#funkcje)
- [Role i uprawnienia](#role-i-uprawnienia)
- [Bezpieczeństwo](#bezpieczeństwo)
- [Integracje z zewnętrznymi usługami](#integracje-z-zewnętrznymi-usługami)
- [Technologie](#technologie)
- [Struktura projektu](#struktura-projektu)
- [Uruchomienie lokalne](#uruchomienie-lokalne)
- [Zmienne środowiskowe](#zmienne-środowiskowe)
- [Testy](#testy)
- [Wdrożenie produkcyjne](#wdrożenie-produkcyjne)
- [Licencja](#licencja)

## Funkcje

### Strona główna i ogłoszenia
- Ogłoszenia zarządu ze stronicowaniem, dostępne bez logowania.
- Edytor tekstu sformatowanego (pogrubienia, listy, nagłówki, linki, wyróżnienia, wyrównanie).
- Przypinanie ważnych ogłoszeń na górze listy.

### Konta użytkowników
- **Rejestracja** z weryfikacją CAPTCHA. Nowe konto czeka na akceptację administratora, który potwierdza tożsamość hodowcy.
- **Logowanie** z opcją „Zapamiętaj mnie” i powrotem na stronę, z której użytkownik został przekierowany.
- **Odzyskiwanie hasła**: link wysyłany e-mailem, ważny 30 minut i jednorazowy.
- **Ustawienia konta**:
  - podgląd danych z rejestracji, roli, daty założenia konta, stażu i funkcji w zarządzie;
  - samodzielna zmiana telefonu i adresu;
  - zmiana hasła z potwierdzeniem obecnego hasła i powiadomieniem e-mail;
  - wylogowanie ze wszystkich urządzeń;
  - pobranie kopii swoich danych w pliku JSON (prawo dostępu do danych z RODO).

### Panel administratora
- Akceptowanie i odrzucanie nowych kont.
- Blokowanie i odblokowywanie kont oraz zmiana ról użytkowników.
- Wyszukiwanie i sortowanie hodowców (alfabetycznie z uwzględnieniem polskich znaków, według daty).

### Powiadomienia
- Dzwonek w pasku nawigacji z licznikiem nieprzeczytanych powiadomień.
- Powiadomienia m.in. o akceptacji konta, zmianie roli, nowym ogłoszeniu, wątku, odpowiedzi na forum, planie lotów, wynikach i zgłoszeniu znalezionego gołębia.

### Plany i wyniki lotów
- Plany lotów na kolejne sezony, z podziałem na gołębie dorosłe i młode: data, miejscowość wypuszczenia, dystans, kategoria i rodzaj listy.
- Wyniki lotów w plikach `.txt`, osobno dla całego oddziału i dla każdej sekcji.
- Plany i wyniki dodaje administrator; przeglądać je mogą zalogowani hodowcy.

### Mapy lotów
- Wgrywanie tras z plików **GPX** zapisanych przez elektroniczne obrączki (np. SkyLeader).
- Analiza lotu:
  - dystans po linii prostej i po trasie;
  - czas lotu oraz prędkość średnia i maksymalna (w m/min);
  - wysokość i przewyższenia.
- Trasa na mapie z punktem startu i końca oraz wykres profilu lotu.
- Loty są prywatne: oglądać i usuwać je może tylko hodowca, który je wgrał, oraz administrator.

### Forum
- Kategorie, wątki i odpowiedzi z edytorem tekstu sformatowanego.
- Przypinanie i zamykanie wątków przez moderatorów, edycja własnych wpisów, licznik wyświetleń.

### Znalezione gołębie
- Publiczny formularz dla osób, które znalazły gołębia z obrączką oddziału, w trzech językach: **polskim, angielskim i niemieckim**.
- Zabezpieczony CAPTCHA i limitem zgłoszeń.
- Dane kontaktowe znalazcy widzi wyłącznie administrator w panelu zgłoszeń, gdzie prowadzi zgłoszenie przez kolejne statusy i dodaje notatki.

### Zarząd i pogoda
- **Zarząd:** skład zarządu oddziału i zarządów sekcji z danymi kontaktowymi. Skład ustala administrator.
- **Pogoda:** mapa pogodowa z serwisu Windy (windy.com), pomocna przy planowaniu lotów.

## Role i uprawnienia

| Funkcja | Gość | Hodowca | Moderator | Administrator |
|---|:---:|:---:|:---:|:---:|
| Ogłoszenia, zarząd, pogoda, zgłoszenie znalezionego gołębia | ✓ | ✓ | ✓ | ✓ |
| Plany i wyniki lotów, mapy lotów, forum, ustawienia konta | | ✓ | ✓ | ✓ |
| Dodawanie ogłoszeń, tworzenie i edycja kategorii forum, przypinanie i zamykanie wątków | | | ✓ | ✓ |
| Usuwanie treści na forum | | własne | własne i hodowców | wszystkie |
| Konta użytkowników, zarząd, plany i wyniki lotów, zgłoszenia znalezionych gołębi, usuwanie kategorii forum | | | | ✓ |

## Bezpieczeństwo

- **Uwierzytelnianie:**
  - token JWT w ciasteczku `HttpOnly`;
  - ochrona CSRF;
  - hasła szyfrowane algorytmem BCrypt.
- **Sesje:**
  - rola, status i wersja sesji konta są sprawdzane w bazie przy każdym żądaniu;
  - zablokowanie konta, zmiana hasła i wylogowanie ze wszystkich urządzeń działają od razu.
- **Ochrona przed nadużyciami:**
  - limity prób logowania, rejestracji, odzyskiwania hasła i zgłoszeń;
  - CAPTCHA w formularzach dostępnych bez logowania;
  - stały czas odpowiedzi logowania, który nie zdradza, czy konto istnieje.
- **Odzyskiwanie hasła:**
  - w bazie zapisywany jest tylko skrót SHA-256 tokenu;
  - odpowiedź jest taka sama dla istniejących i nieistniejących adresów e-mail.
- **Treści użytkowników:** HTML z edytora jest oczyszczany na serwerze (jsoup) i ponownie w przeglądarce (DOMPurify).
- **Dane osobowe:** adresy e-mail i dane kontaktowe znalazców nie trafiają do logów aplikacji.

## Integracje z zewnętrznymi usługami

| Usługa | Zastosowanie |
|---|---|
| **Cloudflare Turnstile** | CAPTCHA w rejestracji, zgłoszeniu znalezionego gołębia i odzyskiwaniu hasła. Przeglądarka pobiera token z widżetu, a backend weryfikuje go w API `siteverify` Cloudflare (REST, `RestClient`). |
| **Serwer SMTP** | Wysyłka e-maili z linkiem do zmiany hasła i z potwierdzeniem zmiany hasła (Spring Mail). |
| **OpenStreetMap** | Podkład mapy tras lotów (Leaflet). |
| **Windy** | Mapa pogodowa osadzona na stronie „Pogoda”. |

## Technologie

| Warstwa | Technologie |
|---|---|
| Backend | Java 26, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation, Mail, Actuator), Hibernate, JJWT, jsoup |
| Baza danych | PostgreSQL |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4, TipTap, Leaflet, DOMPurify |
| Testy | JUnit 5, Mockito, Spring MockMvc, H2, JaCoCo, Cypress 15 |

## Struktura projektu

```
pzhgp-zagan/
├── backend/                     # API REST (Spring Boot)
│   ├── src/main/java/com/pzhgp/backend/
│   │   ├── config/              # bezpieczeństwo, JWT, CORS, konfiguracja
│   │   ├── controller/          # endpointy REST
│   │   ├── dto/                 # obiekty żądań i odpowiedzi
│   │   ├── entity/              # encje JPA
│   │   ├── exception/           # wyjątki i ich obsługa
│   │   ├── repository/          # repozytoria Spring Data
│   │   ├── service/             # logika biznesowa
│   │   └── utils/               # funkcje pomocnicze
│   └── src/test/                # testy jednostkowe i integracyjne
└── frontend/                    # interfejs użytkownika (Next.js)
    ├── cypress/                 # testy end-to-end
    └── src/app/
        ├── (auth)/              # logowanie, rejestracja, odzyskiwanie hasła
        ├── (main)/              # pozostałe podstrony z paskiem nawigacji i stopką
        ├── components/          # komponenty wielokrotnego użytku
        ├── services/            # komunikacja z API
        ├── types/               # typy danych
        └── utils/               # funkcje pomocnicze
```

## Uruchomienie lokalne

### Wymagania

- **JDK 26**
- **Node.js 20.9** lub nowszy (z npm)
- **PostgreSQL** (testowane na wersji 16)
- opcjonalnie lokalny serwer pocztowy do testów, np. [Mailpit](https://mailpit.axllent.org/)

### 1. Baza danych

Utwórz pustą bazę, np.:

```sql
CREATE DATABASE pzhgp_zagan;
```

Tabele tworzą się automatycznie przy pierwszym uruchomieniu backendu, razem z czterema sekcjami oddziału.

### 2. Backend

```bash
cd backend
cp .env.example .env
```

Uzupełnij w pliku `.env` co najmniej dane bazy (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) i `JWT_SECRET`, czyli losowy ciąg o długości min. 32 znaków. Pozostałe wartości mają ustawienia odpowiednie do pracy lokalnej. Opis wszystkich zmiennych jest w sekcji [Zmienne środowiskowe](#zmienne-środowiskowe).

Uruchomienie (lub z poziomu IntelliJ IDEA):

```bash
./mvnw spring-boot:run        # Linux, macOS
mvnw.cmd spring-boot:run      # Windows
```

Backend działa pod adresem `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
```

Aplikacja działa pod adresem `http://localhost:3000`.

### 4. Pierwsze konto administratora

Aplikacja nie tworzy konta administratora automatycznie. Zarejestruj się przez formularz, a następnie nadaj sobie uprawnienia w bazie:

```sql
UPDATE breeders
SET role = 'ADMINISTRATOR', status = 'ACTIVE'
WHERE email = 'twoj.adres@example.com';
```

Kolejne konta akceptuje już administrator w panelu aplikacji.

### 5. CAPTCHA i e-maile podczas pracy lokalnej

- **CAPTCHA:** pliki `.env.example` i `.env.local.example` zawierają oficjalne klucze testowe Cloudflare Turnstile, które zawsze przechodzą weryfikację. Konto w Cloudflare nie jest więc potrzebne.
- **E-maile:** domyślne ustawienia pasują do Mailpit (`localhost:1025`). Wysłane wiadomości widać wtedy pod adresem `http://localhost:8025`. Aby wysyłać prawdziwe e-maile, ustaw dane serwera SMTP swojej skrzynki (`MAIL_*`).

## Zmienne środowiskowe

### Backend (`backend/.env`)

| Zmienna | Opis | Wartość domyślna |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Połączenie z bazą PostgreSQL | – |
| `JWT_SECRET` | Klucz podpisujący tokeny logowania (min. 32 znaki) | – |
| `CORS_ALLOWED_ORIGINS` | Adresy frontendu, z których przeglądarka może wysyłać zapytania | `http://localhost:3000` |
| `AUTH_COOKIE_SECURE` | Flaga `Secure` ciasteczka logowania; `true` na serwerze z HTTPS | `false` |
| `FORWARD_HEADERS_STRATEGY` | `native`, gdy backend działa za reverse proxy (np. nginx) | `none` |
| `TURNSTILE_SECRET_KEY` | Klucz tajny Cloudflare Turnstile | klucz testowy w `.env.example` |
| `MAIL_HOST`, `MAIL_PORT` | Serwer SMTP | `localhost`, `1025` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Dane logowania do serwera SMTP | puste |
| `MAIL_FROM` | Adres nadawcy wiadomości | `no-reply@pzhgp-zagan.local` |
| `FRONTEND_URL` | Adres frontendu wstawiany do linków w e-mailach | `http://localhost:3000` |

### Frontend (`frontend/.env.local`)

| Zmienna | Opis |
|---|---|
| `NEXT_PUBLIC_API_URL` | Adres backendu, np. `http://localhost:8080` |
| `NEXT_PUBLIC_TURNSTILE_SITE_KEY` | Klucz witryny Cloudflare Turnstile (klucz testowy w `.env.local.example`) |

## Testy

### Backend

```bash
cd backend
./mvnw test       # ponad 600 testów jednostkowych i integracyjnych
./mvnw verify     # testy i raport pokrycia: target/site/jacoco/index.html
```

- **Baza:** testy korzystają z bazy H2 w pamięci, więc nie wymagają PostgreSQL.
- **Usługi zewnętrzne:** Cloudflare i serwer pocztowy są w testach zastąpione atrapami.

### Frontend

```bash
cd frontend
npm run lint             # ESLint
npm run typecheck        # kontrola typów TypeScript
npm run cypress:open     # testy end-to-end w oknie Cypress
npm run cypress:run      # testy end-to-end w trybie bez okna
```

- **Uruchomienie:** testy Cypress wymagają działającego frontendu (`npm run dev`).
- **Zapytania do API:** są w testach podstawiane, więc backend nie musi działać.

## Wdrożenie produkcyjne

Przy uruchamianiu aplikacji na serwerze zwróć uwagę na kilka ustawień:

- **HTTPS:** aplikacja powinna działać przez HTTPS, z `AUTH_COOKIE_SECURE=true`.
- **Adresy:** `CORS_ALLOWED_ORIGINS` i `FRONTEND_URL` muszą wskazywać publiczny adres frontendu.
- **Reverse proxy:** za proxy ustaw `FORWARD_HEADERS_STRATEGY=native`, żeby limity zapytań widziały adres klienta.
- **CAPTCHA:** zamiast kluczy testowych użyj kluczy własnego widżetu Turnstile i dodaj domenę aplikacji w panelu Cloudflare.
- **Poczta:** ustaw dane prawdziwego serwera SMTP.
- **Zmienne frontendu:** `NEXT_PUBLIC_*` są wbudowywane w aplikację podczas `npm run build`, więc trzeba je ustawić przed budowaniem.
- **Monitoring:** `GET /actuator/health` zwraca stan aplikacji i może służyć do monitoringu.

## Licencja

Projekt jest udostępniony na licencji MIT – szczegóły w pliku [LICENSE.md](LICENSE.md).
