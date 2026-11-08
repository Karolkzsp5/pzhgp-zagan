package com.pzhgp.backend.entity;

import lombok.Getter;

@Getter
public enum BoardRole {
    PREZES(true, "Prezes"),
    WICEPREZES_DS_LOTOWYCH(true, "V-ce Prezes ds. lotowych"),
    WICEPREZES_DS_FINANSOWYCH(true, "V-ce Prezes ds. finansowych"),
    WICEPREZES_DS_GOSPODARCZYCH(true, "V-ce Prezes ds. gospodarczych"),
    SEKRETARZ(true, "Sekretarz"),
    SKARBNIK(true, "Skarbnik"),
    CZLONEK_ZARZADU(false, "Członek Zarządu");

    private final boolean uniquePerBoard;

    /** Nazwa stanowiska wyświetlana użytkownikowi (taka sama jak na stronie zarządu). */
    private final String label;

    BoardRole(boolean uniquePerBoard, String label) {
        this.uniquePerBoard = uniquePerBoard;
        this.label = label;
    }
}