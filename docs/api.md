# API REST

Backend udostępnia API REST pod adresem `http://localhost:8080` (lokalnie i w Dockerze). Wszystkie endpointy aplikacji zaczynają się od `/api/`.

## Konwencje

### Format danych

- **Żądania i odpowiedzi:** JSON (`application/json`). Wyjątki:
  - pliki (GPX, wyniki lotów) wysyła się jako `multipart/form-data`, w częściach `file` (plik) i `metadata` (JSON z dodatkowymi danymi);
  - plik z wynikami lotu jest zwracany jako `text/plain; charset=UTF-8`.
- **Daty:** daty bez godziny w formacie `RRRR-MM-DD`; znaczniki czasu w ISO 8601 w UTC, np. `2026-04-26T08:15:00Z`.
- **Stronicowanie:** listy stronicowane przyjmują parametry `page` (numer strony od 0) i `size` (rozmiar strony; każda lista ma limit) i zwracają obiekt:

  ```json
  {
    "content": [ … ],
    "totalElements": 42,
    "totalPages": 5,
    "number": 0,
    "size": 10
  }
  ```

- **Uprawnienia w odpowiedziach:** DTO zawierają pola wyliczone przez backend, np. `canEdit`, `canDelete`, `canModerate`, na podstawie których frontend pokazuje przyciski.

### Uwierzytelnianie

- **Logowanie** (`POST /api/auth/login`) ustawia ciasteczko `pzhgp_auth` (`HttpOnly`, `SameSite=Lax`) z tokenem JWT. Przeglądarka dołącza je do kolejnych żądań; zapytania z frontendu są wysyłane z `credentials: 'include'`.
- **Ochrona CSRF:**
  - żądania zmieniające dane (`POST`, `PUT`, `PATCH`, `DELETE`) wysyłane z ciasteczkiem logowania muszą mieć nagłówek `X-XSRF-TOKEN` o tej samej wartości co ciasteczko `XSRF-TOKEN`;
  - logowanie i wylogowanie wymagają tego nagłówka zawsze;
  - token można pobrać przez `GET /api/auth/csrf`.
- **CORS:** zapytania są przyjmowane tylko z adresów z `CORS_ALLOWED_ORIGINS`.

Szczegóły w rozdziale [Bezpieczeństwo](bezpieczenstwo.md).

### Kody odpowiedzi

Udane odpowiedzi:

| Kod | Znaczenie |
|---|---|
| `200 OK` | odpowiedź z danymi |
| `201 Created` | zasób utworzony. Gdy klient potrzebuje identyfikatora nowego zasobu (plan lotów, lot w planie, wyniki, lot GPX, zgłoszenie gołębia), treść to `{"id": 123}`; pozostałe odpowiedzi `201` nie mają treści |
| `204 No Content` | operacja wykonana, brak treści (zmiany, usunięcia, logowanie, wylogowanie) |

Błędy zawsze mają treść `text/plain` z komunikatem po polsku, który frontend pokazuje użytkownikowi:

| Kod | Kiedy |
|---|---|
| `400 Bad Request` | błąd walidacji (komunikat dotyczy pierwszego błędnego pola), zły format JSON lub parametru, niepoprawny plik GPX, nieudana weryfikacja CAPTCHA |
| `401 Unauthorized` | brak logowania, wygasła lub unieważniona sesja, błędne dane logowania |
| `403 Forbidden` | brak uprawnień do operacji lub do konkretnego zasobu, brak lub błędny token CSRF |
| `404 Not Found` | zasób nie istnieje albo zły adres |
| `405 Method Not Allowed` | metoda HTTP nieobsługiwana pod danym adresem |
| `409 Conflict` | konflikt ze stanem danych, np. zajęty e-mail, istniejący plan na dany rok, niedozwolona zmiana statusu, odpowiedź w zablokowanym wątku, usuwanie kategorii z wątkami |
| `413 Content Too Large` | plik większy niż pozwala serwer |
| `415 Unsupported Media Type` | nieobsługiwany format treści żądania |
| `429 Too Many Requests` | przekroczony limit prób (logowanie, rejestracja, reset i zmiana hasła, zgłoszenia gołębi) |
| `500 Internal Server Error` | nieoczekiwany błąd serwera (szczegóły tylko w logu serwera) |
| `503 Service Unavailable` | weryfikacja CAPTCHA chwilowo niedostępna (brak odpowiedzi Cloudflare albo błędna konfiguracja klucza) |

## Endpointy

Oznaczenia w kolumnie **Dostęp**:
- **publiczny:** bez logowania;
- **zalogowany:** dowolna aktywna rola;
- **moderator:** moderator lub administrator;
- **administrator:** tylko administrator.

Dodatkowe warunki sprawdzane w serwisach (np. „autor”) są opisane w kolumnie **Opis**.

### Uwierzytelnianie i konto (`/api/auth`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/auth/csrf` | publiczny | ustawia ciasteczko `XSRF-TOKEN` z tokenem CSRF (`204`) |
| `POST` | `/api/auth/register` | publiczny | rejestracja (`RegistrationRequest`: dane osobowe, adres, sekcja, e-mail, telefon, hasło, `captchaToken`); konto ma status oczekujący (`201`); limit 5 prób na godzinę z adresu IP |
| `POST` | `/api/auth/login` | publiczny | logowanie (`email`, `password`, `rememberMe`); ustawia ciasteczko `pzhgp_auth` (`204`); limit 30 prób na 15 minut |
| `POST` | `/api/auth/logout` | publiczny | wylogowanie, usuwa ciasteczko (`204`) |
| `GET` | `/api/auth/me` | zalogowany | dane zalogowanego użytkownika: id, e-mail, imię, rola, status; `401`, gdy nikt nie jest zalogowany |
| `POST` | `/api/auth/forgot-password` | publiczny | prośba o link do zmiany hasła (`email`, `captchaToken`); zawsze `204`; limit 5 prób na godzinę |
| `POST` | `/api/auth/reset-password` | publiczny | ustawienie nowego hasła z tokenu z linku (`token`, `password`) (`204`); wspólny limit z prośbą o link |

### Ustawienia konta (`/api/account`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/account` | zalogowany | dane konta: dane osobowe, kontakt, sekcja, rola, data założenia, funkcje w zarządzie |
| `PUT` | `/api/account/contact` | zalogowany | zmiana telefonu i adresu (`ContactUpdateRequest`); zwraca zaktualizowane dane konta |
| `PUT` | `/api/account/password` | zalogowany | zmiana hasła (`currentPassword`, `newPassword`); kończy wszystkie sesje i usuwa ciasteczko (`204`); limit 5 prób na 15 minut |
| `POST` | `/api/account/logout-all` | zalogowany | wylogowanie ze wszystkich urządzeń (`204`) |
| `GET` | `/api/account/export` | zalogowany | kopia danych użytkownika w JSON: konto, wątki, wpisy, ogłoszenia, loty |

### Panel administratora: konta (`/api/admin/accounts`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/admin/accounts/pending` | administrator | konta czekające na akceptację |
| `GET` | `/api/admin/accounts/registered` | administrator | konta aktywne i zablokowane |
| `PUT` | `/api/admin/accounts/{id}/approve` | administrator | akceptacja konta (`204`) |
| `DELETE` | `/api/admin/accounts/{id}` | administrator | odrzucenie konta oczekującego, które jest wtedy usuwane (`204`) |
| `PUT` | `/api/admin/accounts/{id}/block` | administrator | blokada aktywnego konta (`204`) |
| `PUT` | `/api/admin/accounts/{id}/unblock` | administrator | odblokowanie konta (`204`) |
| `PUT` | `/api/admin/accounts/{id}/role` | administrator | zmiana roli (`{"role": "BREEDER"}` albo `"MODERATOR"`) (`204`) |

### Sekcje (`/api/sections`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/sections` | publiczny | lista sekcji w kolejności wyświetlania (formularz rejestracji) |

### Ogłoszenia (`/api/announcements`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/announcements?page=&size=` | publiczny | ogłoszenia: przypięte, potem od najnowszych (domyślnie 10 na stronę, najwyżej 100) |
| `POST` | `/api/announcements` | moderator | nowe ogłoszenie (`title`, `content` w HTML, `isPinned`) (`201`) |
| `PUT` | `/api/announcements/{id}` | moderator | edycja; tylko autor (`204`) |
| `DELETE` | `/api/announcements/{id}` | zalogowany | usunięcie: autor albo administrator (ogłoszenia innych osób poza administratorami) (`204`) |

### Forum (`/api/forum`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/forum/categories` | zalogowany | kategorie w kolejności `sortOrder` |
| `GET` | `/api/forum/categories/{id}` | zalogowany | jedna kategoria |
| `POST` | `/api/forum/categories` | moderator | nowa kategoria (`name`, `description`, `sortOrder`) (`201`) |
| `PUT` | `/api/forum/categories/{id}` | moderator | edycja: autor albo administrator (`204`) |
| `DELETE` | `/api/forum/categories/{id}` | administrator | usunięcie pustej kategorii (`204`, `409` gdy zawiera wątki) |
| `GET` | `/api/forum/categories/{categoryId}/threads?page=&size=` | zalogowany | wątki kategorii: przypięte, potem według ostatniego wpisu (domyślnie 15) |
| `GET` | `/api/forum/threads/{threadId}?countView=` | zalogowany | wątek; domyślnie zwiększa licznik wyświetleń (`countView=false` przy odświeżaniu) |
| `POST` | `/api/forum/threads` | zalogowany | nowy wątek z pierwszym wpisem (`categoryId`, `title`, `initialPostContent`) (`201`) |
| `PATCH` | `/api/forum/threads/{id}/title` | zalogowany | zmiana tytułu; tylko autor (`204`) |
| `PATCH` | `/api/forum/threads/{id}/status` | moderator | blokada albo przypięcie (`{"isLocked": true}` albo `{"isPinned": false}`) według hierarchii ról (`204`) |
| `DELETE` | `/api/forum/threads/{id}` | zalogowany | usunięcie wątku z wpisami według hierarchii ról (`204`) |
| `GET` | `/api/forum/threads/{threadId}/posts?page=&size=` | zalogowany | wpisy wątku od najstarszego (domyślnie 20) |
| `POST` | `/api/forum/threads/{threadId}/posts` | zalogowany | odpowiedź w wątku (`body` w HTML) (`201`); `409`, gdy wątek jest zablokowany |
| `PUT` | `/api/forum/posts/{postId}` | zalogowany | edycja wpisu; tylko autor (`204`) |
| `DELETE` | `/api/forum/posts/{postId}` | zalogowany | usunięcie wpisu według hierarchii ról; pierwszego wpisu nie można usunąć (`409`) (`204`) |

### Plany i wyniki lotów (`/api/flight-plans`, `/api/flight-results`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/flight-plans` | zalogowany | lista planów (id i rok), od najnowszego |
| `GET` | `/api/flight-plans/{year}` | zalogowany | plan roku: loty gołębi dorosłych i młodych z listą dostępnych wyników |
| `POST` | `/api/flight-plans` | administrator | nowy plan (`{"year": 2027}`) (`201`, `{"id"}`) |
| `DELETE` | `/api/flight-plans/{year}` | administrator | usunięcie pustego planu (`204`) |
| `POST` | `/api/flight-plans/{year}/entries` | administrator | nowy lot w planie (`FlightPlanEntryRequest`: grupa wiekowa, data, miejscowość, dystans, kategoria, rodzaj listy) (`201`, `{"id"}`) |
| `PUT` | `/api/flight-plans/entries/{entryId}` | administrator | edycja lotu (`204`) |
| `DELETE` | `/api/flight-plans/entries/{entryId}` | administrator | usunięcie lotu bez wyników (`204`) |
| `POST` | `/api/flight-results/entries/{entryId}` | administrator | wgranie wyników: `file` (`.txt`, najwyżej 5 MB) i `metadata` (`{"scope": "BRANCH"}` albo `{"scope": "SECTION", "sectionId": 2}`) (`201`, `{"id"}`) |
| `GET` | `/api/flight-results/{resultId}/file` | zalogowany | treść pliku z wynikami (`text/plain`, nazwa w `Content-Disposition`) |
| `DELETE` | `/api/flight-results/{resultId}` | administrator | usunięcie wyników (`204`) |

### Mapy lotów GPX (`/api/flights`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `POST` | `/api/flights` | zalogowany | wgranie lotu: `file` (`.gpx`, najwyżej 10 MB) i opcjonalnie `metadata` (`name`, `ringNumber`, `releaseSite`) (`201`, `{"id"}`) |
| `GET` | `/api/flights?page=&size=` | zalogowany | loty zalogowanego użytkownika, od najnowszego (domyślnie 10) |
| `GET` | `/api/flights/{id}?tolerance=` | zalogowany | szczegóły lotu ze statystykami i trasą; tylko właściciel lub administrator. `tolerance` to tolerancja upraszczania trasy w metrach (`0` = pełna trasa) |
| `DELETE` | `/api/flights/{id}` | zalogowany | usunięcie lotu; tylko właściciel lub administrator (`204`) |

### Znalezione gołębie (`/api/found-pigeons`, `/api/admin/found-pigeons`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `POST` | `/api/found-pigeons` | publiczny | zgłoszenie (`FoundPigeonRequest`: numer obrączki `PL-0369-RR-NNNN`, telefon lub e-mail, kraj, miejscowość, opis, język, `captchaToken`) (`201`, `{"id"}`); limit 5 na godzinę z adresu IP |
| `GET` | `/api/admin/found-pigeons?status=&ringNumber=&page=&size=` | administrator | lista zgłoszeń z filtrem statusu i numeru obrączki (domyślnie 10) |
| `GET` | `/api/admin/found-pigeons/pending-count` | administrator | liczba zgłoszeń oczekujących |
| `GET` | `/api/admin/found-pigeons/{id}` | administrator | jedno zgłoszenie |
| `PATCH` | `/api/admin/found-pigeons/{id}/status` | administrator | zmiana statusu (`{"status": "APPROVED"}`); niedozwolone przejście daje `409` |
| `PATCH` | `/api/admin/found-pigeons/{id}/note` | administrator | notatka administratora (`{"adminNote": "…"}`) |
| `DELETE` | `/api/admin/found-pigeons/{id}` | administrator | usunięcie zgłoszenia (`204`) |

### Zarząd (`/api/board`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/board` | publiczny | członkowie zarządu oddziału i sekcji |
| `GET` | `/api/board/candidates` | administrator | aktywni hodowcy do wyboru w formularzu (id, imię, nazwisko, id sekcji), bez danych kontaktowych |
| `POST` | `/api/board` | administrator | nowy członek (`BoardMemberRequest`: stanowisko, sekcja lub oddział, konto hodowcy albo dane wpisane ręcznie) (`201`) |
| `PUT` | `/api/board/{id}` | administrator | edycja członka zarządu (`204`) |
| `DELETE` | `/api/board/{id}` | administrator | usunięcie członka zarządu (`204`) |

### Powiadomienia (`/api/notifications`)

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/api/notifications?page=&size=` | zalogowany | powiadomienia użytkownika od najnowszego (domyślnie i najwyżej 50) |
| `GET` | `/api/notifications/unread-count` | zalogowany | liczba nieprzeczytanych powiadomień |
| `PUT` | `/api/notifications/{id}/read` | zalogowany | oznaczenie jako przeczytane; tylko własne powiadomienie (`204`) |
| `PUT` | `/api/notifications/read-all` | zalogowany | oznaczenie wszystkich jako przeczytane (`204`) |
| `DELETE` | `/api/notifications/{id}` | zalogowany | usunięcie własnego powiadomienia (`204`) |

### Monitoring

| Metoda | Ścieżka | Dostęp | Opis |
|---|---|---|---|
| `GET` | `/actuator/health` | publiczny | stan aplikacji (`{"status": "UP"}`), bez szczegółów |

## Przykład: zalogowanie i dodanie ogłoszenia

```http
GET /api/auth/csrf HTTP/1.1
Host: localhost:8080

HTTP/1.1 204 No Content
Set-Cookie: XSRF-TOKEN=5f1c…; Path=/
```

```http
POST /api/auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Cookie: XSRF-TOKEN=5f1c…
X-XSRF-TOKEN: 5f1c…

{"email": "admin.demo@example.com", "password": "Demo.2026!", "rememberMe": false}

HTTP/1.1 204 No Content
Set-Cookie: pzhgp_auth=eyJhbGciOi…; Path=/; HttpOnly; SameSite=Lax
```

```http
POST /api/announcements HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Cookie: pzhgp_auth=eyJhbGciOi…; XSRF-TOKEN=5f1c…
X-XSRF-TOKEN: 5f1c…

{"title": "Zebranie sekcji", "content": "<p>Zapraszamy w sobotę o 10:00.</p>", "isPinned": true}

HTTP/1.1 201 Created
```

Przykład błędu:

```http
HTTP/1.1 409 Conflict
Content-Type: text/plain;charset=UTF-8

Plan lotów dla roku 2027 już istnieje.
```
