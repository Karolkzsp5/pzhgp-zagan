-- Indeksy pod najczęstsze zapytania. PostgreSQL nie tworzy indeksów na kluczach obcych automatycznie.

-- Liczba nieprzeczytanych powiadomień (sprawdzana przy każdym przejściu między stronami) i lista powiadomień
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_read ON notifications (breeder_id, is_read);

-- Wpisy wątku od najstarszego i pierwszy wpis wątku
CREATE INDEX IF NOT EXISTS idx_forum_posts_thread ON forum_posts (thread_id, created_at);

-- Wątki kategorii forum
CREATE INDEX IF NOT EXISTS idx_forum_threads_category ON forum_threads (category_id);

-- Loty planu i wyniki lotu
CREATE INDEX IF NOT EXISTS idx_flight_plan_entries_plan ON flight_plan_entries (flight_plan_id);
CREATE INDEX IF NOT EXISTS idx_flight_results_entry ON flight_results (flight_plan_entry_id);

-- Loty GPX hodowcy od najnowszego
CREATE INDEX IF NOT EXISTS idx_pigeon_flights_owner ON pigeon_flights (owner_id, uploaded_at);
