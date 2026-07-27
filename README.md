# Aplikacja webowa do zarządzania oddziałem Związku Hodowców Gołębi Pocztowych 🕊️
**Kompleksowa platforma webowa realizowana w ramach mojej pracy dyplomowej**

Nowoczesna aplikacja zaprojektowana od zera dla **Polskiego Związku Hodowców Gołębi Pocztowych (PZHGP) – Oddział Żagań**. Cel projektu to pełna cyfryzacja działalności oddziału: od obsługi wyników lotowych i interaktywnych map trasy, przez zintegrowane centrum komunikacji hodowców, aż po bezpieczeństwo danych i zaawansowane zarządzanie sekcjami.

---

## 👥 Role użytkowników w systemie

Aplikacja opiera się na rygorystycznym podziale uprawnień (RBAC - Role-Based Access Control), dostosowanym do realnej struktury związku:

1. **Gość (Użytkownik niezalogowany):**
   * Ma dostęp do części publicznej: przeglądanie aktualności, list konkursowych, tabel z wynikami, planów lotów, galerii zdjęć, widżetu pogodowego oraz podstawowych informacji o zarządzie i sekcjach.
2. **Hodowca (Użytkownik zalogowany):**
   * Posiada zweryfikowane konto (aktywowane przez administratora po rejestracji).
   * Zyskuje dostęp do analizy lotów gołębi, tworzenia wątków i dyskusji na Forum. Ma możliwość wysyłania bezpośrednich wiadomości do Zarządu (w zakładce Kontakt) oraz publikowania zgłoszeń zaginionych gołębi.
3. **Moderator:**
   * Odpowiada za utrzymanie porządku w sekcji społecznościowej (zarządzanie wpisami na forum, akceptacja i moderacja zdjęć/filmów w galerii, aktualizacja drobnych ogłoszeń).
4. **Administrator:**
   * Posiada pełną kontrolę nad systemem: zarządzanie użytkownikami (akceptacja kont ze statusem `PENDING`, blokowanie), wgrywanie plików z listami konkursowymi (`.txt`), aktualizacja struktur zarządu oraz edycja globalnych parametrów oddziału.

---

## 🚀 Architektura funkcjonalna systemu

### 1. Moduł Lotów i Rywalizacji (Core Biznesowy)
* **Listy konkursowe:** Inteligentny parser i przeglądarka plików `.txt` generowanych przez zegary lotowe. System umożliwia zaawansowane sortowanie i filtrowanie list według daty, miejscowości lotu oraz przynależności do ligi.
* **Wyniki oddziałowe:** Dynamiczne tabele z klasyfikacją na szczeblu oddziału, podzielone na oficjalne kategorie pucharowe i olimpijskie (np. **GMP, Kat. A, Kat. B, Kat. C, Kat. M, Młode**). Tabele prezentują kluczowe dane: *Lp., Imię i Nazwisko hodowcy, liczba zdobytych konkursów oraz sumaryczna liczba punktów*.
* **Plan lotów:** Czytelny harmonogram zaplanowanych lotów z wyraźnym podziałem na sezon gołębi dorosłych oraz gołębi młodych.
* **Interaktywne Mapy Lotów:** Zaawansowane narzędzie oparte o mapy (OpenStreetMap / Google Maps), renderujące trasy lotów na podstawie wgrywanych plików **.gpx** lub **.csv**. Wykres mapy precyzyjnie pokazuje punkt wypuszczenia (start), punkt gołębnika (meta), linię lotu oraz dokładny dystans w kilometrach.
* **Centrum Pogodowe:** Zintegrowany z zewnętrznym API radar pogodowy na żywo, pomagający hodowcom w analizie warunków atmosferycznych na trasach przelotu.

### 2. Moduł Społeczności i Komunikacji
* **Forum dyskusyjne:** Wewnętrzna platforma wymiany wiedzy dla zalogowanych hodowców, umożliwiająca zakładanie tematów, odpowiadanie na posty oraz prowadzenie dyskusji sekcyjnych.
* **Galeria multimedialna:** Przestrzeń do wgrywania zdjęć i materiałów wideo z wystaw, lotów, wręczenia nagród czy działalności oddziału.
* **Baza zaginionych gołębi:** Dedykowany system zgłaszania i wyszukiwania zgubionych gołębi (numer obrączki rodowej, kolor, cechy szczególne, kontakt do właściciela), ułatwiający powrót gołębi do ich domów.
* **Komunikator (Kontakt):** Bezpieczny kanał bezpośredniej wiadomości na linii *Zalogowany Hodowca ➔ Wybrany Administrator / Moderator*.
* **Globalna Wyszukiwarka:** Algorytm przeszukujący całą treść serwisu (słowa kluczowe w ogłoszeniach, wpisach na forum, opisach zdjęć czy dokumentach).

### 3. Moduł Informacyjno - Organizacyjny
* **Strona główna (Aktualności):** Tablica z najnowszymi komunikatami zarządu oddziału.
* **Zarząd oddziału:** Przejrzysta wizytówka władz oddziału (Prezes, Sekretarz, Skarbnik, Członkowie Zarządu) wraz z funkcjami i danymi kontaktowymi.
* **Wykaz Sekcji:** Lista wszystkich sekcji zrzeszonych w oddziale Żagań wraz z przypisanym im zarządem:
  * *Sekcja 1 – Żagań*
  * *Sekcja 2 – Wymiarki*
  * *Sekcja 3 – Chotków*
  * *Sekcja 4 – Kożuchów*
* **Centrum Pobierania:** Repozytorium plików dla hodowców (druki, spisy gołębi do pobrania w formacie `.pdf`/`.doc`, regulaminy lotowe).

---

## 🛠️ Stos technologiczny i integracje

 Projekt realizowany z wykorzystaniem najnowszych, rynkowych standardów inżynierii oprogramowania:

### Frontend (Klient webowy i mobilny)
* **Framework:** [Next.js](https://nextjs.org/) (App Router, Server Components)
* **Język:** [TypeScript](https://www.typescriptlang.org/) – eliminacja błędów w czasie kompilacji, rygorystyczne typowanie struktur DTO i odpowiedzi z API
* **Stylizowanie i RWD:** [Tailwind CSS](https://tailwindcss.com/) – interfejs w 100% responsywny, dostosowany do ekranów smartfonów (wykorzystanie natywnych klawiatur numerycznych przy polach telefonów i kodów pocztowych)
* **Mapy i Geolokalizacja:** Integracja z bibliotekami mapowymi (Leaflet / Google Maps API) do obsługi plików `.gpx`
* **Wizualizacja:** Dynamiczne komponenty wykresów i wskaźników (np. animowany miernik siły hasła)

### Backend (Serwer API)
* **Framework:** [Spring Boot 3](https://spring.io/projects/spring-boot) (Java 17+)
* **Bezpieczeństwo (Spring Security):** Bezstanowa autoryzacja za pomocą tokenów **JWT (JSON Web Token)**, szyfrowanie haseł algorytmem **BCrypt**, zaawansowana polityka CORS
* **Warstwa danych (JPA / Hibernate):** Relacyjne mapowanie obiektowe z automatyczną strategią nazewnictwa i walidacją po stronie encji
* **Baza danych:** [PostgreSQL](https://www.postgresql.org/) – niezawodna, relacyjna baza danych
* **Integracje zewnętrzne:** Komunikacja z REST API dostawców pogodowych (np. OpenWeatherMap / Windy API)

---

## 🧪 Testowanie i QA

Wysoka bezawaryjność systemu w warunkach produkcyjnych (szczególnie w szczycie sezonu lotowego) jest gwarantowana przez wielopoziomowe testy automatyczne i manualne, implementowane zgodnie z najlepszymi praktykami inżynierii oprogramowania:

### 1. Testy Jednostkowe (Unit Testing - Backend)
* **Narzędzia:** JUnit 5, Mockito
* **Zakres:** Izolowane testowanie logiki biznesowej w warstwie serwisów (np. walidacja siły i szyfrowania haseł, przeliczanie punktów w tabelach wyników, parsowanie struktur plików `.txt` i `.gpx`).

### 2. Testy Integracyjne (Integration Testing - API)
* **Narzędzia:** Spring Boot Test, MockMvc, Testcontainers
* **Zakres:** Weryfikacja przepływu danych między kontrolerami REST, a relacyjną bazą danych PostgreSQL. Testowanie mechanizmów bezpieczeństwa Spring Security (odrzucanie żądań bez ważnego tokena JWT, weryfikacja uprawnień dla poszczególnych ról: `BREEDER`, `MODERATOR`, `ADMINISTRATOR`).

### 3. Testy Komponentów i E2E (Frontend)
* **Narzędzia:** Jest, React Testing Library, Playwright / Cypress
* **Zakres:** 
  * Automatyczna weryfikacja renderowania interfejsu oraz walidacji formularzy (np. natychmiastowe blokowanie niedozwolonych znaków w polach tekstowych).
  * Testy End-to-End (E2E) symulujące realne ścieżki użytkownika w przeglądarce: od wypełnienia formularza rejestracji, przez logowanie, po przesłanie zgłoszenia zaginionego gołębia.

### 4. Testy Responsywności i Multiplatformowości (RWD & Mobile QA)
* Weryfikacja działania interfejsu na fizycznych urządzeniach mobilnych z systemami Android oraz iOS.
* Testowanie stabilności w środowisku produkcyjnym (paczki generowane przez `npm run build`), sprawdzanie natywnych zachowań klawiatur wirtualnych (`inputMode="numeric"`, `type="tel"`) oraz integracji z buforem autokorekty na smartfonach.

---

## 📈 Status realizacji

- [x] Opracowanie architektury relacyjnej bazy danych (PostgreSQL)
- [x] Backendowy moduł rejestracji i logowania (Spring Security, JWT, BCrypt, DTO)
- [x] Frontendowy formularz rejestracji z walidacją w czasie rzeczywistym, maskowaniem telefonów/kodów i wskaźnikiem siły hasła
- [x] Widok logowania (`/login`) i obsługa sesji klienta w Next.js
- [ ] Panel administratora do zarządzania kontami ze statusem `PENDING`
- [ ] Strona główna z ogłoszeniami
- [ ] Podstrona do tworzenia planów lotów
- [ ] Parser plików `.txt` dla list konkursowych i generator tabel wyników
- [ ] Zakładka pobierz - repozytorium plików do pobrania
- [ ] Integracja map interaktywnych i parsowanie tras `.gpx`/`.csv`
- [ ] Forum dyskusyjne, panel zaginionych gołębi oraz galeria multimedialna
- [ ] Widżet pogodowy
- [ ] Wyszukiwarka i podstrony: zarząd oddziału i kontakt

---
*Projekt tworzony z pasją do programowania i informatyki*
