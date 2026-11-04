package com.pzhgp.backend.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Limit prób w przesuwanym oknie czasowym, liczony osobno dla każdego klucza (zwykle adresu IP).
 * <p>
 * Licznik trzymany jest w pamięci aplikacji — dla pojedynczej instancji serwera oddziału
 * to wystarcza i nie wymaga dodatkowej infrastruktury. Próba sprzed dokładnie jednego okna
 * nie jest już liczona.
 */
final class SlidingWindowRateLimiter {

    /** Powyżej tylu śledzonych kluczy wykonywane jest porządkowanie mapy. */
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Duration window;
    private final int maxAttempts;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    SlidingWindowRateLimiter(Duration window, int maxAttempts, Clock clock) {
        this.window = window;
        this.maxAttempts = maxAttempts;
        this.clock = clock;
    }

    /**
     * Rejestruje próbę dla podanego klucza.
     *
     * @return {@code true}, gdy próba mieści się w limicie
     */
    boolean tryAcquire(String key) {
        String normalizedKey = key == null || key.isBlank() ? "unknown" : key;
        Instant now = clock.instant();

        if (attempts.size() > CLEANUP_THRESHOLD) {
            removeExpiredEntries(now);
        }

        AtomicBoolean accepted = new AtomicBoolean(false);

        attempts.compute(normalizedKey, (ignored, timestamps) -> {
            Deque<Instant> current = timestamps == null ? new ArrayDeque<>() : timestamps;
            discardExpired(current, now);

            if (current.size() < maxAttempts) {
                current.addLast(now);
                accepted.set(true);
            }

            return current;
        });

        return accepted.get();
    }

    void reset() {
        attempts.clear();
    }

    private void discardExpired(Deque<Instant> timestamps, Instant now) {
        Instant windowStart = now.minus(window);

        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(windowStart)) {
            timestamps.removeFirst();
        }
    }

    /** Usuwa klucze, z których od dawna nic nie przyszło — bez tego mapa rosłaby bez końca. */
    private void removeExpiredEntries(Instant now) {
        attempts.forEach((key, ignored) ->
                attempts.computeIfPresent(key, (currentKey, timestamps) -> {
                    discardExpired(timestamps, now);
                    return timestamps.isEmpty() ? null : timestamps;
                })
        );
    }
}
