package com.pzhgp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proste ograniczenie częstotliwości wysyłania publicznego formularza.
 * <p>
 * Formularz odnalezienia gołębia działa bez logowania, więc bez żadnej bariery dałoby się
 * zapełnić bazę automatycznymi zgłoszeniami. Licznik trzymany jest w pamięci aplikacji —
 * dla pojedynczej, lokalnej instancji oddziału to wystarcza i nie wymaga dodatkowej
 * infrastruktury.
 * <p>
 * Kluczem jest adres zdalny połączenia, a nie nagłówek przesłany przez klienta: nagłówki
 * typu {@code X-Forwarded-For} użytkownik może dowolnie podrobić, więc oparcie limitu
 * na nich byłoby pozorne.
 */
@Component
public class SubmissionRateLimiter {

    /** Długość okna, w którym zliczane są zgłoszenia. */
    private static final Duration WINDOW = Duration.ofHours(1);

    /** Dopuszczalna liczba zgłoszeń z jednego adresu w oknie. */
    private static final int MAX_SUBMISSIONS_PER_WINDOW = 5;

    /** Powyżej tylu śledzonych adresów wykonywane jest porządkowanie mapy. */
    private static final int CLEANUP_THRESHOLD = 1_000;

    private final Map<String, Deque<Instant>> submissions = new ConcurrentHashMap<>();

    /**
     * Rejestruje próbę wysłania formularza.
     *
     * @param clientKey identyfikator nadawcy, zwykle adres zdalny połączenia
     * @return {@code true}, gdy zgłoszenie mieści się w limicie
     */
    public boolean tryAcquire(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "unknown" : clientKey;
        Instant now = Instant.now();

        if (submissions.size() > CLEANUP_THRESHOLD) {
            removeExpiredEntries(now);
        }

        Deque<Instant> timestamps = submissions.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (timestamps) {
            discardOlderThanWindow(timestamps, now);

            if (timestamps.size() >= MAX_SUBMISSIONS_PER_WINDOW) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    /** Czyści zapamiętane zgłoszenia — wykorzystywane w testach. */
    public void reset() {
        submissions.clear();
    }

    private void discardOlderThanWindow(Deque<Instant> timestamps, Instant now) {
        Instant windowStart = now.minus(WINDOW);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
            timestamps.pollFirst();
        }
    }

    /**
     * Usuwa adresy, z których od dawna nic nie przyszło. Bez tego mapa rosłaby
     * w nieskończoność przy dużym ruchu.
     */
    private void removeExpiredEntries(Instant now) {
        submissions.forEach((key, timestamps) -> {
            synchronized (timestamps) {
                discardOlderThanWindow(timestamps, now);
                if (timestamps.isEmpty()) {
                    submissions.remove(key, timestamps);
                }
            }
        });
    }
}
