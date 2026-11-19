# Baza danych

Aplikacja korzysta z bazy **PostgreSQL**. Schemat bazy tworzą i zmieniają **migracje Flyway** zapisane w repozytorium. Hibernate (JPA) mapuje encje na tabele, ale sam niczego w schemacie nie zmienia: działa w trybie `validate` i przy starcie sprawdza tylko, czy encje zgadzają się z tabelami.

## Model danych

```mermaid
erDiagram
    sections ||--o{ breeders : "członkowie sekcji"
    sections |o--o{ board_members : "zarząd sekcji"
    breeders |o--o{ board_members : "pełni funkcję"
    breeders ||--o{ announcements : "autor"
    breeders ||--o{ notifications : "odbiorca"
    breeders ||--o{ forum_categories : "autor"
    forum_categories ||--o{ forum_threads : "zawiera"
    breeders ||--o{ forum_threads : "autor"
    forum_threads ||--|{ forum_posts : "zawiera"
    breeders ||--o{ forum_posts : "autor"
    flight_plans ||--o{ flight_plan_entries : "zawiera loty"
    flight_plan_entries ||--o{ flight_results : "wyniki lotu"
    sections |o--o{ flight_results : "wyniki sekcji"
    breeders ||--o{ flight_results : "wgrał"
    breeders ||--o{ pigeon_flights : "właściciel"
    pigeon_flights ||--|{ flight_track_points : "punkty trasy"

    sections {
        bigint id PK
        varchar name UK
        integer sort_order
    }
    breeders {
        bigint id PK
        varchar email UK
        varchar phone_number UK
        varchar password_hash
        varchar name
        varchar surname
        bigint section_id FK
        varchar role
        varchar status
        integer session_version
        varchar password_reset_token_hash
        timestamptz created_at
    }
    board_members {
        bigint id PK
        varchar role
        bigint breeder_id FK
        bigint managed_section_id FK
        varchar custom_name
        varchar custom_surname
        varchar contact_phone
    }
    announcements {
        bigint id PK
        varchar title
        text content
        boolean is_pinned
        bigint author_id FK
        timestamptz created_at
    }
    notifications {
        bigint id PK
        bigint breeder_id FK
        varchar message
        varchar link
        varchar type
        boolean is_read
        timestamptz created_at
    }
    forum_categories {
        bigint id PK
        varchar name
        text description
        integer sort_order
        bigint author_id FK
    }
    forum_threads {
        bigint id PK
        varchar title
        bigint category_id FK
        bigint breeder_id FK
        boolean is_pinned
        boolean is_locked
        integer views
        timestamptz last_post_at
    }
    forum_posts {
        bigint id PK
        bigint thread_id FK
        bigint breeder_id FK
        text body
        timestamptz created_at
        timestamptz edited_at
    }
    flight_plans {
        bigint id PK
        integer plan_year UK
    }
    flight_plan_entries {
        bigint id PK
        bigint flight_plan_id FK
        varchar pigeon_age_group
        date scheduled_date
        varchar location
        integer distance_km
        varchar category
        varchar list_type
    }
    flight_results {
        bigint id PK
        bigint flight_plan_entry_id FK
        varchar scope
        bigint section_id FK
        varchar original_file_name
        text content
        bigint uploaded_by FK
    }
    pigeon_flights {
        bigint id PK
        bigint owner_id FK
        varchar name
        varchar ring_number
        timestamptz start_time
        timestamptz end_time
        double track_distance_meters
        double average_speed_m_per_min
    }
    flight_track_points {
        bigint id PK
        bigint flight_id FK
        integer point_index
        double latitude
        double longitude
        double elevation_meters
        timestamptz recorded_at
    }
    found_pigeon_reports {
        bigint id PK
        varchar ring_number
        varchar ring_number_normalized
        varchar contact_email
        varchar contact_phone
        varchar status
        text admin_note
        timestamptz created_at
    }
```

Diagram pokazuje najważniejsze kolumny. Pełna definicja jest w migracji `backend/src/main/resources/db/migration/V1__initial_schema.sql`.

## Tabele

| Tabela | Encja | Zawartość i ważniejsze ograniczenia |
|---|---|---|
| `sections` | `Section` | Cztery sekcje oddziału (Żagań, Wymiarki, Chotków, Kożuchów) dodawane przez migrację `V1`. Nazwa jest unikalna. |
| `breeders` | `Breeder` | Konta użytkowników. E-mail i telefon są unikalne. `role` to `BREEDER`, `MODERATOR` lub `ADMINISTRATOR`, a `status` to `PENDING`, `ACTIVE` lub `BLOCKED`. Hasło jest zapisane jako skrót BCrypt, a token resetu hasła jako skrót SHA-256. `session_version` służy do unieważniania sesji. |
| `board_members` | `BoardMember` | Członkowie zarządu. Pusta `managed_section_id` oznacza zarząd oddziału. Członek ma albo powiązanie z kontem (`breeder_id`), albo dane wpisane ręcznie (`custom_*`). |
| `announcements` | `Announcement` | Ogłoszenia; treść to oczyszczony HTML. |
| `notifications` | `Notification` | Powiadomienia użytkownika (`breeder_id` to odbiorca). Typ jest ograniczony regułą CHECK do wartości `NotificationType`. |
| `forum_categories` | `ForumCategory` | Kategorie forum z kolejnością `sort_order`. |
| `forum_threads` | `ForumThread` | Wątki z flagami przypięcia i blokady, licznikiem wyświetleń i datą ostatniego wpisu. |
| `forum_posts` | `ForumPost` | Wpisy w wątkach; pierwszy wpis jest treścią wątku. |
| `flight_plans` | `FlightPlan` | Plany lotów; jeden plan na rok (`uk_flight_plans_year`). |
| `flight_plan_entries` | `FlightPlanEntry` | Loty w planie: grupa wiekowa (`ADULT` lub `YOUNG`), data, miejscowość, dystans, kategoria i rodzaj listy. |
| `flight_results` | `FlightResult` | Pliki z wynikami: zasięg `BRANCH` (oddział) lub `SECTION` (sekcja, wtedy `section_id` wskazuje sekcję), treść pliku i autor. |
| `pigeon_flights` | `PigeonFlight` | Loty wgrane z plików GPX razem z wyliczonymi statystykami. |
| `flight_track_points` | `FlightTrackPoint` | Punkty trasy lotu w kolejności `point_index`; jeden lot ma zwykle kilka tysięcy punktów. |
| `found_pigeon_reports` | `FoundPigeonReport` | Zgłoszenia znalezionych gołębi. Nie są powiązane z kontami, bo zgłasza je osoba z zewnątrz. `ring_number_normalized` służy do wyszukiwania. |
| `flyway_schema_history` | – | Tabela techniczna Flyway z historią wykonanych migracji. |

**Klucze i czas:**
- **Klucze główne** są generowane przez bazę (`GENERATED BY DEFAULT AS IDENTITY`).
- **Znaczniki czasu** są zapisywane jako `timestamp with time zone` i mapowane na `java.time.Instant`. W bazie i w API czas jest w UTC, a na czas lokalny zamienia go przeglądarka.
- **Typy wyliczeniowe** są zapisywane jako tekst, a reguły CHECK w bazie pilnują dozwolonych wartości.

## Indeksy

Poza kluczami głównymi i unikalnymi baza ma indeksy pod najczęstsze zapytania. PostgreSQL nie tworzy indeksów na kluczach obcych automatycznie.

| Indeks | Kolumny | Zapytanie |
|---|---|---|
| `idx_notifications_recipient_read` | `notifications (breeder_id, is_read)` | liczba nieprzeczytanych powiadomień (przy każdej zmianie strony) i lista powiadomień |
| `idx_forum_posts_thread` | `forum_posts (thread_id, created_at)` | wpisy wątku i pierwszy wpis |
| `idx_forum_threads_category` | `forum_threads (category_id)` | wątki kategorii |
| `idx_flight_plan_entries_plan` | `flight_plan_entries (flight_plan_id)` | loty planu |
| `idx_flight_results_entry` | `flight_results (flight_plan_entry_id)` | wyniki lotu |
| `idx_pigeon_flights_owner` | `pigeon_flights (owner_id, uploaded_at)` | loty hodowcy od najnowszego |
| `idx_track_point_flight` | `flight_track_points (flight_id, point_index)` | punkty trasy lotu w kolejności |
| `idx_found_pigeon_ring` | `found_pigeon_reports (ring_number_normalized)` | wyszukiwanie zgłoszeń po numerze obrączki |
| `idx_found_pigeon_status` | `found_pigeon_reports (status)` | filtrowanie zgłoszeń po statusie |

## Migracje Flyway

Migracje są w katalogu `backend/src/main/resources/db/migration/`. Flyway wykonuje je przy starcie backendu, przed uruchomieniem Hibernate, i zapisuje wykonane wersje w tabeli `flyway_schema_history`.

| Plik | Zawartość |
|---|---|
| `V1__initial_schema.sql` | Wszystkie tabele, ograniczenia, klucze obce i cztery sekcje oddziału. Odpowiada schematowi, który wcześniej Hibernate tworzył automatycznie, łącznie z nazwami kluczy obcych. |
| `V2__align_schema_created_by_hibernate.sql` | Doprowadza bazę utworzoną wcześniej przez Hibernate (`ddl-auto=update`) do stanu z `V1`: flagi wątków bez wartości NULL, znaczniki czasu ze strefą czasową i aktualne listy wartości w regułach CHECK. W nowej bazie niczego nie zmienia. |
| `V3__add_indexes.sql` | Indeksy pod najczęstsze zapytania. |

### Baza utworzona przed wprowadzeniem Flyway (baseline)

Przed wprowadzeniem Flyway schemat tworzył Hibernate. Taka baza nie ma tabeli historii migracji. Dzięki ustawieniu `spring.flyway.baseline-on-migrate=true` Flyway oznacza ją przy pierwszym starcie jako wersję 1 (*baseline*), pomija `V1` i wykonuje tylko `V2` i `V3`, więc istniejące dane zostają. Pusta baza dostaje wszystkie migracje od `V1`.

### Zasady pracy z migracjami

- **Wykonanej migracji nie wolno edytować.** Flyway zapamiętuje sumy kontrolne plików i przy zmianie zatrzyma start aplikacji. Każda zmiana schematu to nowy plik z kolejnym numerem.
- **Zmiana encji i migracja idą razem.** Jeśli encja nie zgadza się z tabelą, Hibernate w trybie `validate` zatrzyma start i wskaże brakującą kolumnę albo zły typ.
- **Nazwa pliku:** `V<numer>__<opis>.sql`, np. `V4__add_breeder_nickname.sql`:

  ```sql
  ALTER TABLE breeders ADD COLUMN nickname VARCHAR(50);
  ```

- **Testy sprawdzają migracje.** Przed testami baza testowa jest czyszczona i budowana od zera wszystkimi migracjami (rozdział [Testy](testy.md)), więc błędna migracja od razu wychodzi w testach.

## Bazy w różnych środowiskach

| Środowisko | Baza | Uwagi |
|---|---|---|
| Uruchomienie lokalne | `pzhgp_zagan` na lokalnym PostgreSQL | adres i dane logowania w `backend/.env` |
| Testy backendu | `pzhgp_zagan_test` | czyszczona przed testami; nazwa musi kończyć się na `_test` |
| Docker | `pzhgp_zagan` w kontenerze PostgreSQL 18 | dane w wolumenie `db-data`; kopię można wczytać z katalogu `docker/db-import/` |

## Kopia zapasowa i odtworzenie

```bash
# kopia w formacie PostgreSQL (do odtworzenia przez pg_restore)
pg_dump -U postgres -d pzhgp_zagan -F c -f pzhgp_zagan.dump

# odtworzenie do pustej bazy
pg_restore -U postgres -d pzhgp_zagan --no-owner pzhgp_zagan.dump

# kopia jako plik SQL, np. do wczytania w Dockerze
pg_dump -U postgres -d pzhgp_zagan --no-owner --no-privileges -f docker/db-import/pzhgp_zagan.sql
```

Kopię trzeba wczytywać do **pustej** bazy, zanim backend zostanie uruchomiony. Inaczej Flyway utworzy własne tabele, a kopia się z nimi zderzy.
