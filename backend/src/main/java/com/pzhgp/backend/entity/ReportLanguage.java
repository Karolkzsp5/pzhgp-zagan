package com.pzhgp.backend.entity;

/**
 * Język, w którym znalazca wypełnił publiczny formularz zgłoszenia.
 * <p>
 * Zapisywany razem ze zgłoszeniem, aby administrator wiedział, w jakim języku
 * skontaktować się ze znalazcą.
 */
public enum ReportLanguage {

    /** Polski — język domyślny. */
    PL,

    /** Angielski. */
    EN,

    /** Niemiecki. */
    DE
}
