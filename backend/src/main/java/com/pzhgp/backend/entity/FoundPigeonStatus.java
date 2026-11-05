package com.pzhgp.backend.entity;

/**
 * Status obsługi zgłoszenia odnalezienia gołębia.
 * <p>
 * Dozwolone przejścia tworzą jednokierunkowy przepływ: zgłoszenie oczekujące można
 * zatwierdzić lub odrzucić, zatwierdzone zamknąć lub odrzucić. Sprawy zakończone
 * i odrzucone pozostają zamknięte — ponownego otwierania nie przewidziano.
 */
public enum FoundPigeonStatus {

    /** Nowe zgłoszenie oczekujące na sprawdzenie przez administratora. */
    PENDING("Oczekujące"),

    /** Administrator zweryfikował zgłoszenie i ustala właściciela gołębia. */
    APPROVED("W trakcie"),

    /** Obsługa zgłoszenia została zakończona. */
    RESOLVED("Zakończone"),

    /** Zgłoszenie odrzucono, np. z powodu nieprawidłowych danych lub spamu. */
    REJECTED("Odrzucone");

    /** Nazwa statusu wyświetlana użytkownikowi (taka sama jak w panelu administratora). */
    private final String label;

    FoundPigeonStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Sprawdza, czy ze stanu bieżącego wolno przejść do wskazanego stanu.
     */
    public boolean canTransitionTo(FoundPigeonStatus target) {
        if (target == null || target == this) {
            return false;
        }

        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED;
            case APPROVED -> target == RESOLVED || target == REJECTED;
            case RESOLVED, REJECTED -> false;
        };
    }
}
