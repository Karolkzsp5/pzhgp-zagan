package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.ReportLanguage;
import jakarta.validation.constraints.*;

/**
 * Dane wysyłane przez publiczny formularz odnalezienia gołębia.
 * <p>
 * Formularz może być wypełniany przez osoby z Polski i zagranicy.
 * Numer obrączki dotyczy gołębi należących do oddziału PZHGP Żagań,
 * dlatego wymagany jest format PL-0369-RR-NNNN.
 * Wymagany jest numer obrączki oraz co najmniej jedna metoda kontaktu.
 *
 * @param ringNumber        numer odczytany z obrączki gołębia
 * @param contactPhone      telefon znalazcy, także zagraniczny
 * @param contactEmail      adres e-mail znalazcy
 * @param foundLocation     miejscowość lub przybliżone miejsce odnalezienia
 * @param foundCountry      kraj odnalezienia
 * @param description       krótki opis okoliczności
 * @param preferredLanguage język formularza; brak wartości oznacza polski
 */
public record FoundPigeonRequest(

        @NotBlank(message = "Podaj numer obrączki gołębia.")
        @Pattern(
                regexp = "^PL-0369-\\d{2}-\\d{4}$",
                message = "Numer obrączki musi mieć format PL-0369-RR-NNNN, np. PL-0369-24-1234."
        )
        String ringNumber,

        @Size(max = 15, message = "Numer telefonu może mieć maksymalnie 15 znaków.")
        @Pattern(
                regexp = "^\\+?[0-9 ()./\\-]+$",
                message = "Numer telefonu może zawierać wyłącznie cyfry, opcjonalny znak + oraz spacje, nawiasy, kropki, ukośniki i kreski."
        )
        String contactPhone,

        @Email(message = "Podaj poprawny adres e-mail.")
        @Size(max = 320, message = "Adres e-mail może mieć maksymalnie 320 znaków.")
        String contactEmail,

        @Size(max = 150, message = "Miejsce odnalezienia może mieć maksymalnie 150 znaków.")
        String foundLocation,

        @Size(max = 100, message = "Nazwa kraju może mieć maksymalnie 100 znaków.")
        String foundCountry,

        @Size(max = 1000, message = "Opis może mieć maksymalnie 1000 znaków.")
        String description,

        ReportLanguage preferredLanguage
) {

    private static final int MIN_PHONE_DIGITS = 6;
    private static final int MAX_PHONE_DIGITS = 15;

    public FoundPigeonRequest {
        ringNumber = trim(ringNumber);
        contactPhone = trimToNull(contactPhone);
        contactEmail = trimToNull(contactEmail);
        foundLocation = trimToNull(foundLocation);
        foundCountry = trimToNull(foundCountry);
        description = trimToNull(description);
    }

    @AssertTrue(message = "Podaj numer telefonu lub adres e-mail, aby administrator mógł się z Tobą skontaktować.")
    public boolean isContactMethodProvided() {
        return contactPhone != null || contactEmail != null;
    }

    @AssertTrue(message = "Numer telefonu musi zawierać od 6 do 15 cyfr.")
    public boolean isPhoneDigitCountValid() {
        if (contactPhone == null) {
            return true;
        }

        long digits = contactPhone.chars()
                .filter(Character::isDigit)
                .count();

        return digits >= MIN_PHONE_DIGITS
                && digits <= MAX_PHONE_DIGITS;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}