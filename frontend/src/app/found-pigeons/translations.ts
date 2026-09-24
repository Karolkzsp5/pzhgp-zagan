import { ReportLanguage } from '@/app/types/foundPigeon';

/**
 * Tłumaczenia publicznego formularza odnalezienia gołębia.
 *
 * Zakres wielojęzyczności jest celowo wąski: obejmuje wyłącznie ten formularz i jego
 * komunikaty, ponieważ wypełniają go osoby postronne, często z zagranicy. Reszta
 * aplikacji pozostaje polska — służy hodowcom z oddziału.
 */
export interface FormTranslation {
    languageName: string;
    pageTitle: string;
    pageLead: string;
    howItWorksTitle: string;
    howItWorksSteps: string[];
    languageSelectorLabel: string;
    formTitle: string;
    ringNumberLabel: string;
    ringNumberHint: string;
    ringNumberPlaceholder: string;
    errorRingInvalid: string;
    contactSectionTitle: string;
    contactSectionHint: string;
    phoneLabel: string;
    phonePlaceholder: string;
    emailLabel: string;
    emailPlaceholder: string;
    countryLabel: string;
    countryPlaceholder: string;
    locationLabel: string;
    locationPlaceholder: string;
    descriptionLabel: string;
    descriptionPlaceholder: string;
    descriptionCounter: (used: number, max: number) => string;
    optionalSuffix: string;
    privacyTitle: string;
    privacyBody: string;
    submitButton: string;
    submittingButton: string;
    successTitle: string;
    successBody: string;
    submitAnother: string;
    errorRingRequired: string;
    errorContactRequired: string;
    errorEmailInvalid: string;
    errorPhoneInvalid: string;
    errorDescriptionTooLong: string;
    errorSubmitFailed: string;
    errorTooManyRequests: string;
}

export const FORM_TRANSLATIONS: Record<ReportLanguage, FormTranslation> = {
    PL: {
        languageName: 'Polski',
        pageTitle: 'Znalazłeś gołębia pocztowego?',
        pageLead: 'Jeżeli znalazłeś gołębia z obrączką, wypełnij poniższy formularz. '
            + 'Zgłoszenie trafi do administratora oddziału PZHGP Żagań, który spróbuje ustalić jego właściciela.',
        howItWorksTitle: 'Jak to działa',
        howItWorksSteps: [
            'Odczytaj numer z obrączki na nodze gołębia i przepisz go do formularza.',
            'Podaj telefon lub adres e-mail, żebyśmy mogli się z Tobą skontaktować.',
            'Administrator oddziału sprawdzi zgłoszenie i ustali właściciela gołębia.'
        ],
        languageSelectorLabel: 'Język formularza',
        formTitle: 'Formularz zgłoszenia',
        ringNumberLabel: 'Numer obrączki',
        ringNumberHint: 'Przepisz cały numer z obrączki, razem z literami i myślnikami.',
        ringNumberPlaceholder: 'np. PL-0369-24-1234',
        errorRingInvalid: 'Numer obrączki musi mieć format PL-0369-RR-NNNN, np. PL-0369-24-1234.',
        contactSectionTitle: 'Kontakt do Ciebie',
        contactSectionHint: 'Podaj przynajmniej jeden sposób kontaktu — telefon albo adres e-mail.',
        phoneLabel: 'Telefon',
        phonePlaceholder: 'np. +48 601 234 567',
        emailLabel: 'Adres e-mail',
        emailPlaceholder: 'np. jan@example.com',
        countryLabel: 'Kraj odnalezienia',
        countryPlaceholder: 'np. Polska',
        locationLabel: 'Miejscowość lub okolica',
        locationPlaceholder: 'np. Żagań',
        descriptionLabel: 'Opis okoliczności',
        descriptionPlaceholder: 'np. Gołąb siedzi na parapecie od wczoraj, wygląda na osłabionego.',
        descriptionCounter: (used, max) => `${used} z ${max} znaków`,
        optionalSuffix: 'opcjonalnie',
        privacyTitle: 'Co zrobimy z Twoimi danymi',
        privacyBody: 'Twój numer telefonu i adres e-mail służą wyłącznie do obsługi tego zgłoszenia. '
            + 'Widzi je tylko administrator oddziału. Jeżeli uda się ustalić właściciela gołębia, '
            + 'administrator może przekazać mu Twój kontakt. '
            + 'Zgłoszenia nie są publikowane na stronie.',
        submitButton: 'Wyślij zgłoszenie',
        submittingButton: 'Wysyłanie…',
        successTitle: 'Dziękujemy za zgłoszenie',
        successBody: 'Zgłoszenie trafiło do administratora oddziału. Jeżeli uda się ustalić właściciela '
            + 'gołębia, skontaktujemy się z Tobą podanym sposobem.',
        submitAnother: 'Zgłoś kolejnego gołębia',
        errorRingRequired: 'Podaj numer obrączki gołębia.',
        errorContactRequired: 'Podaj numer telefonu lub adres e-mail.',
        errorEmailInvalid: 'Podaj poprawny adres e-mail.',
        errorPhoneInvalid: 'Podaj poprawny numer telefonu (od 6 do 15 cyfr).',
        errorDescriptionTooLong: 'Opis może mieć maksymalnie 1000 znaków.',
        errorSubmitFailed: 'Nie udało się wysłać zgłoszenia. Spróbuj ponownie za chwilę.',
        errorTooManyRequests: 'Wysłano zbyt wiele zgłoszeń. Spróbuj ponownie później.'
    },

    EN: {
        languageName: 'English',
        pageTitle: 'Have you found a racing pigeon?',
        pageLead: 'If you have found a pigeon wearing a ring, please fill in the form below. '
            + 'Your report goes to the administrator of the PZHGP Żagań branch, who will try to identify the owner.',
        howItWorksTitle: 'How it works',
        howItWorksSteps: [
            'Read the number from the ring on the pigeon’s leg and copy it into the form.',
            'Leave a phone number or an e-mail address so that we can reach you.',
            'The branch administrator reviews the report and tries to identify the owner.'
        ],
        languageSelectorLabel: 'Form language',
        formTitle: 'Report form',
        ringNumberLabel: 'Ring number',
        ringNumberHint: 'Copy the whole number from the ring, including letters and dashes.',
        ringNumberPlaceholder: 'e.g. PL-0369-24-1234',
        errorRingInvalid: 'The ring number must have the format PL-0369-YY-NNNN, e.g. PL-0369-24-1234.',
        contactSectionTitle: 'Your contact details',
        contactSectionHint: 'Provide at least one way to contact you — a phone number or an e-mail address.',
        phoneLabel: 'Phone number',
        phonePlaceholder: 'e.g. +48 601 234 567',
        emailLabel: 'E-mail address',
        emailPlaceholder: 'e.g. john@example.com',
        countryLabel: 'Country where it was found',
        countryPlaceholder: 'e.g. Germany',
        locationLabel: 'Town or area',
        locationPlaceholder: 'e.g. Cottbus',
        descriptionLabel: 'Circumstances',
        descriptionPlaceholder: 'e.g. The pigeon has been sitting on my windowsill since yesterday and looks weak.',
        descriptionCounter: (used, max) => `${used} of ${max} characters`,
        optionalSuffix: 'optional',
        privacyTitle: 'What we do with your details',
        privacyBody: 'Your phone number and e-mail address are used only to handle this report. '
            + 'Only the branch administrator can see them. If the owner of the pigeon is identified, '
            + 'the administrator may pass your contact details on so that the bird can be collected. '
            + 'Reports are never published on the website.',
        submitButton: 'Send report',
        submittingButton: 'Sending…',
        successTitle: 'Thank you for your report',
        successBody: 'Your report has reached the branch administrator. If the owner is identified, '
            + 'we will contact you using the details you provided.',
        submitAnother: 'Report another pigeon',
        errorRingRequired: 'Please enter the ring number.',
        errorContactRequired: 'Please provide a phone number or an e-mail address.',
        errorEmailInvalid: 'Please enter a valid e-mail address.',
        errorPhoneInvalid: 'Please enter a valid phone number (6 to 15 digits).',
        errorDescriptionTooLong: 'The description may contain at most 1000 characters.',
        errorSubmitFailed: 'The report could not be sent. Please try again in a moment.',
        errorTooManyRequests: 'Too many reports have been submitted. Please try again later.'
    },

    DE: {
        languageName: 'Deutsch',
        pageTitle: 'Haben Sie eine Brieftaube gefunden?',
        pageLead: 'Wenn Sie eine Taube mit einem Ring gefunden haben, füllen Sie bitte das folgende Formular aus. '
            + 'Ihre Meldung erreicht den Administrator der Abteilung PZHGP Żagań, der versucht, den Besitzer zu ermitteln.',
        howItWorksTitle: 'So funktioniert es',
        howItWorksSteps: [
            'Lesen Sie die Nummer vom Ring am Bein der Taube ab und tragen Sie sie in das Formular ein.',
            'Hinterlassen Sie eine Telefonnummer oder E-Mail-Adresse, damit wir Sie erreichen können.',
            'Der Administrator prüft die Meldung und versucht, den Besitzer zu ermitteln.'
        ],
        languageSelectorLabel: 'Sprache des Formulars',
        formTitle: 'Meldeformular',
        ringNumberLabel: 'Ringnummer',
        ringNumberHint: 'Übertragen Sie die vollständige Nummer vom Ring, einschließlich Buchstaben und Bindestrichen.',
        ringNumberPlaceholder: 'z. B. PL-0369-24-1234',
        errorRingInvalid: 'Die Ringnummer muss das Format PL-0369-JJ-NNNN haben, z. B. PL-0369-24-1234.',
        contactSectionTitle: 'Ihre Kontaktdaten',
        contactSectionHint: 'Geben Sie mindestens eine Kontaktmöglichkeit an — Telefon oder E-Mail-Adresse.',
        phoneLabel: 'Telefonnummer',
        phonePlaceholder: 'z. B. +49 30 12345678',
        emailLabel: 'E-Mail-Adresse',
        emailPlaceholder: 'z. B. hans@example.com',
        countryLabel: 'Land des Fundes',
        countryPlaceholder: 'z. B. Deutschland',
        locationLabel: 'Ort oder Umgebung',
        locationPlaceholder: 'z. B. Cottbus',
        descriptionLabel: 'Umstände des Fundes',
        descriptionPlaceholder: 'z. B. Die Taube sitzt seit gestern auf meiner Fensterbank und wirkt geschwächt.',
        descriptionCounter: (used, max) => `${used} von ${max} Zeichen`,
        optionalSuffix: 'optional',
        privacyTitle: 'Was mit Ihren Daten geschieht',
        privacyBody: 'Ihre Telefonnummer und E-Mail-Adresse werden ausschließlich zur Bearbeitung dieser Meldung '
            + 'verwendet. Nur der Administrator der Abteilung kann sie einsehen. Wird der Besitzer der Taube '
            + 'ermittelt, kann der Administrator Ihre Kontaktdaten weitergeben, damit der Vogel abgeholt werden '
            + 'kann. Meldungen werden nicht auf der Website veröffentlicht.',
        submitButton: 'Meldung senden',
        submittingButton: 'Wird gesendet…',
        successTitle: 'Vielen Dank für Ihre Meldung',
        successBody: 'Ihre Meldung hat den Administrator der Abteilung erreicht. Wird der Besitzer ermittelt, '
            + 'melden wir uns über die von Ihnen angegebenen Kontaktdaten.',
        submitAnother: 'Weitere Taube melden',
        errorRingRequired: 'Bitte geben Sie die Ringnummer an.',
        errorContactRequired: 'Bitte geben Sie eine Telefonnummer oder E-Mail-Adresse an.',
        errorEmailInvalid: 'Bitte geben Sie eine gültige E-Mail-Adresse an.',
        errorPhoneInvalid: 'Bitte geben Sie eine gültige Telefonnummer an (6 bis 15 Ziffern).',
        errorDescriptionTooLong: 'Die Beschreibung darf höchstens 1000 Zeichen enthalten.',
        errorSubmitFailed: 'Die Meldung konnte nicht gesendet werden. Bitte versuchen Sie es später erneut.',
        errorTooManyRequests: 'Es wurden zu viele Meldungen gesendet. Bitte versuchen Sie es später erneut.'
    }
};

/** Kolejność przycisków przełącznika języka. */
export const LANGUAGE_ORDER: ReportLanguage[] = ['PL', 'EN', 'DE'];
