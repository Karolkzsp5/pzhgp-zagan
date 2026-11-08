package com.pzhgp.backend.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Ograniczenie częstotliwości wysyłania publicznego formularza.
 * <p>
 * Formularz odnalezienia gołębia działa bez logowania, więc bez żadnej bariery dałoby się
 * zapełnić bazę automatycznymi zgłoszeniami.
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

    private final SlidingWindowRateLimiter submissions;

    public SubmissionRateLimiter(Clock clock) {
        this.submissions = new SlidingWindowRateLimiter(WINDOW, MAX_SUBMISSIONS_PER_WINDOW, clock);
    }

    /**
     * Rejestruje próbę wysłania formularza.
     *
     * @param clientKey identyfikator nadawcy, zwykle adres zdalny połączenia
     * @return {@code true}, gdy zgłoszenie mieści się w limicie
     */
    public boolean tryAcquire(String clientKey) {
        return submissions.tryAcquire(clientKey);
    }
}
