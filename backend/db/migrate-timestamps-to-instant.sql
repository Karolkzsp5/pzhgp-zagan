-- Jednorazowa migracja istniejącej bazy PostgreSQL po przejściu backendu na java.time.Instant.
--
-- Wcześniej znaczniki czasu (LocalDateTime) trafiały do kolumn "timestamp without time zone"
-- i oznaczały czas lokalny serwera. Instant jest zapisywany w kolumnach "timestamp with time zone",
-- a spring.jpa.hibernate.ddl-auto=update nie zmienia typu istniejących kolumn.
--
-- Skrypt zmienia typ tylko tych kolumn, które nadal są "without time zone", więc można go
-- bezpiecznie uruchomić ponownie. Nowa, pusta baza go nie potrzebuje.
--
-- Stare wartości są traktowane jako czas w strefie Europe/Warsaw. Jeśli serwer działał
-- w innej strefie, zmień wartość old_time_zone poniżej.
--
-- Uruchomienie: psql -d nazwa_bazy -f migrate-timestamps-to-instant.sql

DO $$
DECLARE
    old_time_zone CONSTANT text := 'Europe/Warsaw';
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
                target.table_name, target.column_name, target.column_name, old_time_zone
        );
        RAISE NOTICE 'Zmieniono typ kolumny %.%', target.table_name, target.column_name;
    END LOOP;
END
$$;
