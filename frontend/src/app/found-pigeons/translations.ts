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
    languageSelectorLabel: string;
    formTitle: string;
    ringNumberLabel: string;
    ringNumberPlaceholder: string;
    errorRingInvalid: string;
    contactSectionTitle: string;
    contactSectionHint: string;
    phoneLabel: string;
    emailLabel: string;
    countryLabel: string;
    locationLabel: string;
    descriptionLabel: string;
    descriptionCounter: (used: number, max: number) => string;
    privacyTitle: string;
    privacyBody: string;
    requiredFieldsNote: string;
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
        languageSelectorLabel: 'Język',
        formTitle: 'Formularz zgłoszenia',
        ringNumberLabel: 'Numer obrączki',
        ringNumberPlaceholder: 'np. PL-0369-24-1234',
        errorRingInvalid: 'Numer obrączki musi mieć format PL-0369-RR-NNNN, np. PL-0369-24-1234.',
        contactSectionTitle: 'Kontakt do Ciebie',
        contactSectionHint: 'Podaj przynajmniej jeden sposób kontaktu - telefon albo adres e-mail.',
        phoneLabel: 'Telefon',
        emailLabel: 'Adres e-mail',
        countryLabel: 'Kraj odnalezienia',
        locationLabel: 'Miejscowość lub okolica',
        descriptionLabel: 'Opis okoliczności',
        descriptionCounter: (used, max) => `${used} z ${max} znaków`,
        privacyTitle: 'Co zrobimy z Twoimi danymi',
        privacyBody: 'Twój numer telefonu i adres e-mail służą wyłącznie do obsługi tego zgłoszenia. '
            + 'Widzi je tylko administrator oddziału. Jeżeli uda się ustalić właściciela gołębia, '
            + 'administrator może przekazać mu Twój kontakt. '
            + 'Zgłoszenia nie są publikowane na stronie.',
        requiredFieldsNote: 'Pola oznaczone gwiazdką są wymagane.',
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
        languageSelectorLabel: 'Language',
        formTitle: 'Report form',
        ringNumberLabel: 'Ring number',
        ringNumberPlaceholder: 'e.g. PL-0369-24-1234',
        errorRingInvalid: 'The ring number must have the format PL-0369-YY-NNNN, e.g. PL-0369-24-1234.',
        contactSectionTitle: 'Your contact details',
        contactSectionHint: 'Provide at least one way to contact you - a phone number or an e-mail address.',
        phoneLabel: 'Phone number',
        emailLabel: 'E-mail address',
        countryLabel: 'Country where it was found',
        locationLabel: 'Town or area',
        descriptionLabel: 'Circumstances',
        descriptionCounter: (used, max) => `${used} of ${max} characters`,
        privacyTitle: 'What we do with your details',
        privacyBody: 'Your phone number and e-mail address are used only to handle this report. '
            + 'Only the branch administrator can see them. If the owner of the pigeon is identified, '
            + 'the administrator may pass your contact details on so that the bird can be collected. '
            + 'Reports are never published on the website.',
        requiredFieldsNote: 'Fields marked with an asterisk are required.',
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
        languageSelectorLabel: 'Sprache',
        formTitle: 'Meldeformular',
        ringNumberLabel: 'Ringnummer',
        ringNumberPlaceholder: 'z. B. PL-0369-24-1234',
        errorRingInvalid: 'Die Ringnummer muss das Format PL-0369-JJ-NNNN haben, z. B. PL-0369-24-1234.',
        contactSectionTitle: 'Ihre Kontaktdaten',
        contactSectionHint: 'Geben Sie mindestens eine Kontaktmöglichkeit an - Telefon oder E-Mail-Adresse.',
        phoneLabel: 'Telefonnummer',
        emailLabel: 'E-Mail-Adresse',
        countryLabel: 'Land des Fundes',
        locationLabel: 'Ort oder Umgebung',
        descriptionLabel: 'Umstände des Fundes',
        descriptionCounter: (used, max) => `${used} von ${max} Zeichen`,
        privacyTitle: 'Was mit Ihren Daten geschieht',
        privacyBody: 'Ihre Telefonnummer und E-Mail-Adresse werden ausschließlich zur Bearbeitung dieser Meldung '
            + 'verwendet. Nur der Administrator der Abteilung kann sie einsehen. Wird der Besitzer der Taube '
            + 'ermittelt, kann der Administrator Ihre Kontaktdaten weitergeben, damit der Vogel abgeholt werden '
            + 'kann. Meldungen werden nicht auf der Website veröffentlicht.',
        requiredFieldsNote: 'Mit einem Sternchen gekennzeichnete Felder sind Pflichtfelder.',
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
