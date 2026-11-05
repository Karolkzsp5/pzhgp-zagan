-- Jednorazowa migracja istniejącej bazy PostgreSQL po zmianie pól ForumThread.isLocked
-- i ForumThread.isPinned z Boolean na boolean.
--
-- Pole typu boolean nie przyjmuje wartości NULL, a spring.jpa.hibernate.ddl-auto=update
-- nie dodaje ograniczenia NOT NULL do istniejących kolumn. Skrypt zamienia ewentualne
-- wartości NULL na false i dodaje to ograniczenie. Można go bezpiecznie uruchomić ponownie.
-- Nowa, pusta baza go nie potrzebuje.
--
-- Uruchomienie: w pgAdmin (Query Tool) albo psql -d nazwa_bazy -f forum-thread-flags-not-null.sql

UPDATE forum_threads SET is_locked = false WHERE is_locked IS NULL;
UPDATE forum_threads SET is_pinned = false WHERE is_pinned IS NULL;

ALTER TABLE forum_threads ALTER COLUMN is_locked SET NOT NULL;
ALTER TABLE forum_threads ALTER COLUMN is_pinned SET NOT NULL;
