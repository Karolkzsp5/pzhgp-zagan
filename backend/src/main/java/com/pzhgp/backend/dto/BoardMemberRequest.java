package com.pzhgp.backend.dto;

import com.pzhgp.backend.entity.BoardRole;
import jakarta.validation.constraints.*;

public record BoardMemberRequest(
        @NotNull(message = "Rola jest wymagana")
        BoardRole role,

        @NotNull(message = "Kolejność sortowania jest wymagana")
        @Min(value = 1, message = "Kolejność musi wynosić minimum 1")
        Integer sortOrder,

        Long managedSectionId,
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
                if (breederId != null) return true;
                return customName != null && !customName.isBlank() &&
                        customSurname != null && !customSurname.isBlank();
        }
}