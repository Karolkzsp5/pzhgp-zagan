package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Zgłoszenie odnalezienia gołębia pocztowego wysłane przez publiczny formularz.
 * <p>
 * Zgłoszenie nie ma powiązania z kontem hodowcy — znalazcą bywa osoba postronna,
 * często z zagranicy, która nie zakłada konta w systemie. Dane kontaktowe znalazcy
 * są danymi osobowymi i widzi je wyłącznie administrator oddziału.
 */
@Entity
@Table(
        name = "found_pigeon_reports",
        indexes = {
                @Index(name = "idx_found_pigeon_ring", columnList = "ring_number_normalized"),
                @Index(name = "idx_found_pigeon_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class FoundPigeonReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numer obrączki w postaci wpisanej przez znalazcę — służy do wyświetlania. */
    @Column(name = "ring_number", nullable = false, length = 64)
    private String ringNumber;

    /**
     * Numer obrączki sprowadzony do postaci porównywalnej: wielkie litery, bez znaków
     * rozdzielających. Pozwala odnaleźć zgłoszenie niezależnie od tego, czy znalazca
     * zapisał numer jako "PL-0208-24-1234", "pl 0208 24 1234" czy "PL0208241234".
     */
    @Column(name = "ring_number_normalized", nullable = false, length = 64)
    private String ringNumberNormalized;

    /** Telefon znalazcy — opcjonalny, ale razem z adresem e-mail nie może zabraknąć obu. */
    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "found_location", length = 150)
    private String foundLocation;

    @Column(name = "found_country", length = 100)
    private String foundCountry;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false, length = 2)
    private ReportLanguage preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoundPigeonStatus status;

    /** Prywatna notatka administratora — nigdy nie trafia poza panel administracyjny. */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = FoundPigeonStatus.PENDING;
        }
        if (this.preferredLanguage == null) {
            this.preferredLanguage = ReportLanguage.PL;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
