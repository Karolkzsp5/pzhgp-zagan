package com.pzhgp.backend.entity;

import lombok.Getter;

@Getter
public enum BoardRole {
    PREZES(true),
    WICEPREZES_DS_LOTOWYCH(true),
    WICEPREZES_DS_FINANSOWYCH(true),
    WICEPREZES_DS_GOSPODARCZYCH(true),
    SEKRETARZ(true),
    SKARBNIK(true),
    CZLONEK_ZARZADU(false);

    private final boolean uniquePerBoard;

    BoardRole(boolean uniquePerBoard) {
        this.uniquePerBoard = uniquePerBoard;
    }
}