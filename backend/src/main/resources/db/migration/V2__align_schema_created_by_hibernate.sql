-- Doprowadza bazę utworzoną wcześniej przez Hibernate (ddl-auto=update) do stanu z V1.
-- Tryb update dodawał tylko nowe tabele i kolumny: nie zmieniał typów, nie dodawał NOT NULL
-- do istniejących kolumn i nie aktualizował list dozwolonych wartości enumów.
-- W nowej bazie, utworzonej przez V1, wszystkie poniższe polecenia niczego nie zmieniają.

-- 1. Flagi wątku forum: pola boolean bez wartości NULL (wcześniej Boolean).
UPDATE forum_threads SET is_locked = false WHERE is_locked IS NULL;
UPDATE forum_threads SET is_pinned = false WHERE is_pinned IS NULL;
ALTER TABLE forum_threads ALTER COLUMN is_locked SET NOT NULL;
ALTER TABLE forum_threads ALTER COLUMN is_pinned SET NOT NULL;

-- 2. Znaczniki czasu jako "timestamp with time zone" (java.time.Instant, wcześniej LocalDateTime).
-- Zmieniane są tylko kolumny, które nadal są "without time zone". Ich wartości oznaczały
-- czas lokalny serwera, więc są traktowane jako czas w strefie Europe/Warsaw.
DO $$
DECLARE
    target record;
BEGIN
    FOR target IN
        SELECT table_name::text AS table_name, column_name::text AS column_name
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND data_type = 'timestamp without time zone'
          AND table_name::text || '.' || column_name::text IN (
              'announcements.created_at',
              'announcements.updated_at',
              'breeders.created_at',
              'flight_plans.created_at',
              'flight_plans.updated_at',
              'flight_results.uploaded_at',
              'forum_categories.created_at',
              'forum_posts.created_at',
              'forum_posts.edited_at',
              'forum_threads.created_at',
              'forum_threads.last_post_at',
              'found_pigeon_reports.created_at',
              'found_pigeon_reports.updated_at',
              'notifications.created_at'
          )
    LOOP
        EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE timestamp(6) with time zone USING %I AT TIME ZONE %L',
                target.table_name, target.column_name, target.column_name, 'Europe/Warsaw'
        );
    END LOOP;
END
$$;

-- 3. Dozwolone wartości enumów. Hibernate zapisał je w regułach CHECK przy tworzeniu tabeli
-- i nie aktualizował ich po dodaniu nowych wartości, np. nowych typów powiadomień.
ALTER TABLE breeders DROP CONSTRAINT IF EXISTS breeders_role_check;
ALTER TABLE breeders ADD CONSTRAINT breeders_role_check CHECK (role IN ('BREEDER', 'MODERATOR', 'ADMINISTRATOR'));

ALTER TABLE breeders DROP CONSTRAINT IF EXISTS breeders_status_check;
ALTER TABLE breeders ADD CONSTRAINT breeders_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED'));

ALTER TABLE board_members DROP CONSTRAINT IF EXISTS board_members_role_check;
ALTER TABLE board_members ADD CONSTRAINT board_members_role_check CHECK (role IN ('PREZES', 'WICEPREZES_DS_LOTOWYCH',
    'WICEPREZES_DS_FINANSOWYCH', 'WICEPREZES_DS_GOSPODARCZYCH', 'SEKRETARZ', 'SKARBNIK', 'CZLONEK_ZARZADU'));

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN ('ACCOUNT_APPROVED', 'ROLE_CHANGED',
    'NEW_ANNOUNCEMENT', 'ANNOUNCEMENT_DELETED', 'NEW_THREAD', 'NEW_REPLY', 'NEW_FLIGHT_PLAN', 'NEW_FLIGHT_RESULT',
    'NEW_FOUND_PIGEON_REPORT'));

ALTER TABLE flight_plan_entries DROP CONSTRAINT IF EXISTS flight_plan_entries_pigeon_age_group_check;
ALTER TABLE flight_plan_entries ADD CONSTRAINT flight_plan_entries_pigeon_age_group_check
    CHECK (pigeon_age_group IN ('ADULT', 'YOUNG'));

ALTER TABLE flight_results DROP CONSTRAINT IF EXISTS flight_results_scope_check;
ALTER TABLE flight_results ADD CONSTRAINT flight_results_scope_check CHECK (scope IN ('BRANCH', 'SECTION'));

ALTER TABLE found_pigeon_reports DROP CONSTRAINT IF EXISTS found_pigeon_reports_preferred_language_check;
ALTER TABLE found_pigeon_reports ADD CONSTRAINT found_pigeon_reports_preferred_language_check
    CHECK (preferred_language IN ('PL', 'EN', 'DE'));

ALTER TABLE found_pigeon_reports DROP CONSTRAINT IF EXISTS found_pigeon_reports_status_check;
ALTER TABLE found_pigeon_reports ADD CONSTRAINT found_pigeon_reports_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'RESOLVED', 'REJECTED'));
