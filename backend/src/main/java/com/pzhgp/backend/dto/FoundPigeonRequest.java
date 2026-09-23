package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.ReportLanguage;
import jakarta.validation.constraints.*;

/**
 * Dane wysyłane przez publiczny formularz odnalezienia gołębia.
 * <p>
 * Formularz wypełniają osoby postronne, często z zagranicy, dlatego walidacja musi
 * przyjmować zapisy spoza Polski, a jednocześnie odrzucać wartości ewidentnie błędne.
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
        @Size(max = 64, message = "Numer obrączki może mieć maksymalnie 64 znaki.")
        @Pattern(
                regexp = "^[A-Za-z0-9 ./\\-]+$",
                message = "Numer obrączki może zawierać wyłącznie litery, cyfry, spacje, kropki, kreski i ukośniki."
        )
        String ringNumber,

        @Size(max = 32, message = "Numer telefonu może mieć maksymalnie 32 znaki.")
        @Pattern(
                regexp = "^$|^\\+?[0-9 ()./\\-]+$",
                message = "Numer telefonu może zawierać wyłącznie cyfry, opcjonalny znak + oraz spacje, nawiasy, kropki i kreski."
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

    /** Najmniejsza liczba cyfr uznawana za numer telefonu. */
    private static final int MIN_PHONE_DIGITS = 6;

    /** Największa liczba cyfr w numerze telefonu wg zalecenia E.164. */
    private static final int MAX_PHONE_DIGITS = 15;

    /**
     * Zgłoszenie bez żadnej metody kontaktu jest bezużyteczne — administrator nie miałby
     * jak odpowiedzieć znalazcy. Wystarczy telefon albo e-mail; można podać oba.
     */
    @AssertTrue(message = "Podaj numer telefonu lub adres e-mail, aby administrator mógł się z Tobą skontaktować.")
    public boolean isContactMethodProvided() {
        return hasText(contactPhone) || hasText(contactEmail);
    }

    /**
     * Liczba cyfr sprawdzana jest osobno od dozwolonych znaków, ponieważ separatory
     * różnią się między krajami, a sama ich obecność nic nie mówi o poprawności numeru.
     */
    @AssertTrue(message = "Numer telefonu musi zawierać od 6 do 15 cyfr.")
    public boolean isPhoneDigitCountValid() {
        if (!hasText(contactPhone)) {
            return true;
        }

        long digits = contactPhone.chars().filter(Character::isDigit).count();
        return digits >= MIN_PHONE_DIGITS && digits <= MAX_PHONE_DIGITS;
    }

    /** Numer obrączki musi zawierać choć jedną literę lub cyfrę — same separatory to nie numer. */
    @AssertTrue(message = "Numer obrączki musi zawierać litery lub cyfry.")
    public boolean isRingNumberMeaningful() {
        return ringNumber == null || ringNumber.chars().anyMatch(Character::isLetterOrDigit);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
