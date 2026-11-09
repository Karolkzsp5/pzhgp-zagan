package com.pzhgp.backend.controller;

import com.pzhgp.backend.dto.FoundPigeonRequest;
import com.pzhgp.backend.exception.SubmissionRateLimitException;
import com.pzhgp.backend.service.FoundPigeonService;
import com.pzhgp.backend.service.SubmissionRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publiczny punkt przyjmowania zgłoszeń odnalezienia gołębia.
 * <p>
 * Jedyna operacja dostępna bez uwierzytelnienia. Celowo nie ma tu żadnego odczytu —
 * lista zgłoszeń i dane kontaktowe znalazców są dostępne wyłącznie przez panel
 * administratora.
 */
@RestController
@RequestMapping("/api/found-pigeons")
@RequiredArgsConstructor
public class FoundPigeonController {

    private final FoundPigeonService foundPigeonService;
    private final SubmissionRateLimiter submissionRateLimiter;

    /**
     * Przyjmuje zgłoszenie odnalezienia gołębia.
     *
     * @return identyfikator zgłoszenia — bez żadnych danych podanych w formularzu
     */
    @PostMapping
    public ResponseEntity<Map<String, Long>> submitReport(@Valid @RequestBody FoundPigeonRequest request, HttpServletRequest httpRequest) {
        // Adres zdalny połączenia zamiast nagłówka X-Forwarded-For: nagłówek pochodzi
        // od klienta i można go dowolnie ustawić, więc limit oparty na nim byłby pozorny.
        if (!submissionRateLimiter.tryAcquire(httpRequest.getRemoteAddr())) {
            throw new SubmissionRateLimitException("Zbyt wiele zgłoszeń. Spróbuj ponownie później.");
        }

        Long reportId = foundPigeonService.createReport(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", reportId));
    }
}
