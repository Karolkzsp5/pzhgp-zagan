package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.BoardRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BoardMemberRequest(
        @NotNull(message = "Rola jest wymagana")
        BoardRole role,

        @Positive(message = "ID sekcji musi być liczbą dodatnią")
        Long managedSectionId,

        @Positive(message = "ID hodowcy musi być liczbą dodatnią")
        Long breederId,

        @Size(max = 32, message = "Imię może mieć maksymalnie 32 znaki")
        String customName,

        @Size(max = 64, message = "Nazwisko może mieć maksymalnie 64 znaki")
        String customSurname,

        @Pattern(regexp = "^\\d{9}$", message = "Numer telefonu musi mieć dokładnie 9 cyfr")
        String contactPhone
) {
    @AssertTrue(message = "Wymagane jest wybranie konta hodowcy ALBO podanie imienia i nazwiska dla osoby, która nie ma konta w systemie.")
    public boolean isPersonDataValid() {
        boolean accountSelected = breederId != null;
        boolean customNameProvided = customName != null && !customName.isBlank();
        boolean customSurnameProvided = customSurname != null && !customSurname.isBlank();

        if (accountSelected) {
            return !customNameProvided && !customSurnameProvided;
        }

        return customNameProvided && customSurnameProvided;
    }
}