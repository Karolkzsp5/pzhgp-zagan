# Bezpieczeństwo

Ten rozdział opisuje mechanizmy ochrony zastosowane w aplikacji: uwierzytelnianie, autoryzację, ochronę przed atakami z przeglądarki, limity prób, ochronę treści i plików oraz danych osobowych.

## Uwierzytelnianie

### Token JWT w ciasteczku

Po poprawnym zalogowaniu (`POST /api/auth/login`) backend tworzy token JWT (`service/JwtService.java`):

- **Podpis:** HMAC z kluczem ze zmiennej `JWT_SECRET` (co najmniej 32 znaki).
- **Zawartość:** e-mail użytkownika jako `subject`, data wydania, data wygaśnięcia i wersja sesji konta (`sessionVersion`).
- **Ważność:** 24 godziny albo 30 dni z opcją „Zapamiętaj mnie”.

Token trafia do ciasteczka `pzhgp_auth` (`config/AuthCookieService.java`):

| Atrybut | Wartość | Po co |
|---|---|---|
| `HttpOnly` | tak | JavaScript nie może odczytać tokenu, więc nie ukradnie go też wstrzyknięty skrypt (XSS) |
| `SameSite` | `Lax` | przeglądarka nie dołącza ciasteczka do żądań `POST` wysyłanych z obcych stron |
| `Secure` | `AUTH_COOKIE_SECURE` | na serwerze z HTTPS ciasteczko jest wysyłane tylko szyfrowanym połączeniem |
| `Max-Age` | tylko z „Zapamiętaj mnie” | bez tej opcji ciasteczko znika po zamknięciu przeglądarki |

### Sprawdzanie każdego żądania

`config/JwtAuthenticationFilter.java` działa przed kontrolerami i dla każdego żądania:

1. odczytuje token z ciasteczka;
2. sprawdza podpis i datę wygaśnięcia;
3. odczytuje konto z bazy i sprawdza, czy ma status `ACTIVE`, a wersja sesji w tokenie zgadza się z wersją w bazie;
4. ustawia użytkownika z uprawnieniem równym jego roli (`BREEDER`, `MODERATOR` lub `ADMINISTRATOR`).

Rola **nie** jest brana z tokenu, tylko z bazy. Zmiana roli albo blokada konta działa więc od następnego żądania, bez czekania na wygaśnięcie tokenu.

### Wersja sesji i natychmiastowe wylogowanie

Konto ma licznik `session_version`. Zwiększają go:
- zmiana hasła w ustawieniach;
- ustawienie nowego hasła przez link z e-maila;
- opcja **Wyloguj ze wszystkich urządzeń**.

Wszystkie wcześniej wydane tokeny mają wtedy starą wersję, więc filtr je odrzuca. Dzięki temu bezstanowy JWT da się unieważnić bez przechowywania listy sesji na serwerze.

### Hasła

- **Przechowywanie:** hasła są zapisywane wyłącznie jako skrót **BCrypt** (z losową solą).
- **Wymagania:** co najmniej 8 znaków, mała i wielka litera, cyfra oraz znak specjalny. Hasło może mieć najwyżej 72 bajty, bo dłuższej części BCrypt nie uwzględnia.
- **Brak zdradzania kont przy logowaniu:**
  - przy złym e-mailu i przy złym haśle komunikat jest ten sam: „Nieprawidłowy adres e-mail lub hasło.”;
  - gdy konto nie istnieje, backend i tak porównuje hasło z przygotowanym skrótem-atrapą, więc czas odpowiedzi nie zdradza, czy adres jest zarejestrowany.
- **Odzyskiwanie hasła** (`service/PasswordResetService.java`):
  - token z linku to 32 losowe bajty (`SecureRandom`), a w bazie jest zapisany tylko jego skrót SHA-256;
  - link jest ważny 30 minut i działa jeden raz;
  - odpowiedź na prośbę o link jest taka sama dla istniejących i nieistniejących adresów.

## Autoryzacja

Uprawnienia są sprawdzane na dwóch poziomach:

1. **`config/SecurityConfig.java`:** reguły dla adresów i metod HTTP, np. `POST /api/flight-plans/**` tylko dla administratora, a każdy adres bez reguły publicznej wymaga zalogowania.
2. **Serwisy:** decyzje zależne od konkretnego rekordu, np. czy użytkownik jest autorem wpisu, właścicielem lotu albo czy ma wyższą rolę niż autor treści (`service/PermissionRules.java`).

Odmowy mają jednolite odpowiedzi:
- brak logowania: `401` „Wymagane jest uwierzytelnienie.” (`config/RestAuthenticationEntryPoint.java`);
- brak uprawnień: `403` z komunikatem (`config/RestAccessDeniedHandler.java` albo wyjątek z serwisu).

Pełna macierz uprawnień jest w rozdziale [Funkcje](funkcje.md#role-i-uprawnienia).

Frontend dodatkowo ukrywa niedostępne przyciski i strony (`AuthGuard`), ale to wyłącznie wygoda. O dostępie zawsze decyduje backend.

## Ochrona przed atakami z przeglądarki

### CSRF (Cross-Site Request Forgery)

Ciasteczko logowania jest wysyłane automatycznie, więc obca strona mogłaby próbować wykonać operację w imieniu zalogowanego użytkownika. Ochrona działa na zasadzie *double submit cookie* (tryb SPA w Spring Security):

- backend ustawia ciasteczko `XSRF-TOKEN`, które frontend może odczytać;
- przy żądaniach `POST`, `PUT`, `PATCH` i `DELETE` frontend przepisuje jego wartość do nagłówka `X-XSRF-TOKEN` (`utils/apiClient.ts`);
- obca strona nie może odczytać ciasteczka innej domeny, więc nie ustawi poprawnego nagłówka.

Token jest wymagany przy logowaniu i wylogowaniu oraz przy każdym żądaniu z ciasteczkiem logowania. Żądania bez tego ciasteczka (np. rejestracja) nie korzystają z sesji, więc CSRF ich nie dotyczy; chronią je CAPTCHA i limity prób.

Frontend odczytuje ciasteczko `XSRF-TOKEN` z JavaScriptu, a przeglądarka udostępnia je tylko stronom z tego samego hosta. Dlatego **frontend i backend muszą działać pod jednym adresem hosta**. Porty mogą być różne, np. `localhost:3000` i `localhost:8080` (rozdział [Uruchomienie](uruchomienie.md#wdrożenie-na-serwer)).

### CORS

Frontend (port 3000) i backend (port 8080) mają różne „pochodzenie” (origin), więc przeglądarka pyta backend, czy może wysyłać zapytania. Backend zezwala tylko na adresy z `CORS_ALLOWED_ORIGINS`:
- metody `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`;
- nagłówki `Content-Type` i nagłówek CSRF;
- przesyłanie ciasteczek.

Zapytania z innych adresów przeglądarka blokuje.

### XSS (wstrzykiwanie skryptów) w treściach użytkowników

Ogłoszenia i wpisy na forum zawierają HTML z edytora tekstu, dlatego są czyszczone dwa razy:

1. **Na serwerze, przed zapisem** (`utils/RichTextSanitizer.java`, biblioteka jsoup):
   - zostają tylko znaczniki formatowania: akapity, nagłówki, listy, pogrubienie, kursywa, podkreślenie, przekreślenie, cytaty, kod, linki i wyróżnienie;
   - linki mogą mieć tylko adresy `http`, `https` i `mailto` i zawsze dostają `rel="noopener noreferrer nofollow"`;
   - z atrybutu `style` zostaje tylko wyrównanie tekstu i kolor wyróżnienia;
   - skrypty, ramki, obsługa zdarzeń (`onclick` itp.) i wszystkie inne elementy są usuwane.
2. **W przeglądarce, przed wyświetleniem** (DOMPurify): drugi poziom ochrony na wypadek treści zapisanych w inny sposób.

Pozostałe dane, np. nazwy, tytuły i opisy, React wyświetla jako zwykły tekst, więc HTML w nich nie jest interpretowany.

## Ochrona przed nadużyciami

### Limity prób

| Operacja | Limit | Klucz |
|---|---|---|
| Logowanie | 30 prób na 15 minut | adres IP |
| Rejestracja | 5 prób na godzinę | adres IP |
| Prośba o link i ustawienie nowego hasła | 5 prób na godzinę (wspólny limit) | adres IP |
| Zmiana hasła w ustawieniach | 5 prób na 15 minut | konto |
| Zgłoszenie znalezionego gołębia | 5 zgłoszeń na godzinę | adres IP |

**Mechanizm:**
- Limity działają w przesuwanym oknie czasu (`service/SlidingWindowRateLimiter.java`) i są trzymane w pamięci backendu.
- Ponowne uruchomienie backendu je zeruje.
- Przekroczenie limitu daje odpowiedź `429` z komunikatem.
- Za serwerem proxy (np. nginx) trzeba ustawić `FORWARD_HEADERS_STRATEGY=native`, żeby limity widziały adres klienta, a nie adres proxy.

### CAPTCHA

Formularze dostępne bez logowania (rejestracja, odzyskiwanie hasła, zgłoszenie znalezionego gołębia) są chronione przez Cloudflare Turnstile. Token z przeglądarki weryfikuje backend w API Cloudflare. Limit prób jest sprawdzany **przed** CAPTCHA, żeby nie dało się wymusić nieograniczonej liczby zapytań do Cloudflare. Szczegóły w rozdziale [Integracja z Cloudflare Turnstile](integracja-turnstile.md).

## Pliki przesyłane przez użytkowników

| Plik | Zabezpieczenia |
|---|---|
| GPX (loty) | najwyżej 10 MB i 200 000 punktów, najwyżej 200 lotów na hodowcę. Parser XML ma wyłączone DTD i encje zewnętrzne, co chroni przed atakami XXE. Błędny plik daje `400` z opisem problemu. |
| Wyniki lotów (`.txt`) | najwyżej 5 MB, tylko tekst w UTF-8 (pliki binarne są odrzucane), oczyszczona nazwa pliku. Treść jest zwracana jako `text/plain`, więc przeglądarka nigdy nie potraktuje jej jak strony HTML. |
| Ogólny limit serwera | 10 MB na plik i 12 MB na całe żądanie (`spring.servlet.multipart.*`); większe pliki dają `413` |

## Dane osobowe

- **Logi:** nie zawierają adresów e-mail użytkowników ani danych kontaktowych znalazców gołębi. W logach pojawiają się tylko identyfikatory kont.
- **Dane kontaktowe znalazców** widzi wyłącznie administrator w panelu zgłoszeń.
- **Lista kandydatów do zarządu** zwraca tylko imię, nazwisko i sekcję, bez telefonu, adresu i daty urodzenia.
- **Prawo dostępu do danych (RODO):** użytkownik może pobrać kopię swoich danych w pliku JSON w ustawieniach konta.
- **Hasła i klucze:** są w zmiennych środowiskowych (`.env`), które nie trafiają do repozytorium (`.gitignore`). W repozytorium są tylko pliki przykładowe z kluczami testowymi.

## Pozostałe ustawienia

- **Nagłówki odpowiedzi API:** domyślne nagłówki bezpieczeństwa Spring Security, m.in. `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` i `Cache-Control: no-store`.
- **Monitoring:** `/actuator/health` zwraca tylko ogólny status, bez szczegółów konfiguracji. Pozostałe endpointy Actuatora nie są udostępnione przez HTTP.
- **Testy:** nie uruchomią się na bazie, której nazwa nie kończy się na `_test`, więc nie skasują prawdziwych danych (rozdział [Testy](testy.md)).

## Znane ograniczenia

- **Limity prób w pamięci:** działają poprawnie przy jednej instancji backendu. Przy kilku instancjach trzeba by przenieść je do wspólnego magazynu, np. Redis.
- **Nagłówki stron frontendu:** strony Next.js nie ustawiają nagłówków `Content-Security-Policy` ani `X-Frame-Options`. Przy wdrożeniu na serwer warto dodać je w serwerze proxy (np. nginx) albo w `next.config.ts`.
- **E-mail w tokenie:** token JWT zawiera adres e-mail jako identyfikator użytkownika. Token jest w ciasteczku `HttpOnly`, więc skrypty go nie odczytają, ale stały numer konta byłby bardziej neutralnym identyfikatorem.
