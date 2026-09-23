"use client";

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import { foundPigeonService } from '@/app/services/foundPigeonService';
import { ReportLanguage } from '@/app/types/foundPigeon';
import { getAuthToken, getUserRole, isJwtValid } from '@/app/utils/jwt';
import { FORM_TRANSLATIONS, LANGUAGE_ORDER } from './translations';

const MAX_DESCRIPTION_LENGTH = 1000;

/** Pola formularza obsługiwane przez jeden stan, żeby czyszczenie po wysyłce było proste. */
interface FormValues {
    ringNumber: string;
    contactPhone: string;
    contactEmail: string;
    foundCountry: string;
    foundLocation: string;
    description: string;
}

const EMPTY_FORM: FormValues = {
    ringNumber: '',
    contactPhone: '',
    contactEmail: '',
    foundCountry: '',
    foundLocation: '',
    description: ''
};

/** Nazwy pól, przy których wyświetlamy komunikat walidacyjny. */
type FieldError = 'ringNumber' | 'contact' | 'contactEmail' | 'contactPhone' | 'description';

export default function FoundPigeonsPage() {
    const [language, setLanguage] = useState<ReportLanguage>('PL');
    const [values, setValues] = useState<FormValues>(EMPTY_FORM);
    const [errors, setErrors] = useState<Partial<Record<FieldError, string>>>({});
    const [submitError, setSubmitError] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [isAdmin, setIsAdmin] = useState(false);

    const text = FORM_TRANSLATIONS[language];

    useEffect(() => {
        const token = getAuthToken();
        if (isJwtValid(token)) {
            setIsAdmin(getUserRole(token) === 'ADMINISTRATOR');
        }
    }, []);

    const setValue = (field: keyof FormValues, value: string) => {
        setValues(previous => ({ ...previous, [field]: value }));
        setErrors({});
        setSubmitError('');
    };

    const descriptionUsed = useMemo(() => values.description.length, [values.description]);

    /**
     * Walidacja po stronie przeglądarki daje szybką podpowiedź w języku formularza.
     * Backend sprawdza te same reguły niezależnie — atrybut required nie jest zabezpieczeniem.
     */
    const validate = (): boolean => {
        const found: Partial<Record<FieldError, string>> = {};

        if (!values.ringNumber.trim()) {
            found.ringNumber = text.errorRingRequired;
        }

        const hasPhone = values.contactPhone.trim().length > 0;
        const hasEmail = values.contactEmail.trim().length > 0;

        if (!hasPhone && !hasEmail) {
            found.contact = text.errorContactRequired;
        }

        if (hasEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(values.contactEmail.trim())) {
            found.contactEmail = text.errorEmailInvalid;
        }

        if (hasPhone) {
            const digits = values.contactPhone.replace(/\D/g, '').length;
            const allowedCharacters = /^\+?[0-9 ()./-]+$/.test(values.contactPhone.trim());

            if (!allowedCharacters || digits < 6 || digits > 15) {
                found.contactPhone = text.errorPhoneInvalid;
            }
        }

        if (values.description.length > MAX_DESCRIPTION_LENGTH) {
            found.description = text.errorDescriptionTooLong;
        }

        setErrors(found);
        return Object.keys(found).length === 0;
    };

    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        if (isSubmitting || !validate()) return;

        setIsSubmitting(true);
        setSubmitError('');

        try {
            await foundPigeonService.submitReport({
                ringNumber: values.ringNumber.trim(),
                contactPhone: values.contactPhone.trim() || null,
                contactEmail: values.contactEmail.trim() || null,
                foundCountry: values.foundCountry.trim() || null,
                foundLocation: values.foundLocation.trim() || null,
                description: values.description.trim() || null,
                preferredLanguage: language
            });

            setIsSubmitted(true);
            setValues(EMPTY_FORM);
        } catch (error) {
            // Serwer odpowiada po polsku, a formularz może być po angielsku lub niemiecku,
            // dlatego pokazujemy komunikat w języku wybranym przez użytkownika.
            const isThrottled = error instanceof Error && error.message.includes('Zbyt wiele zgłoszeń');
            setSubmitError(isThrottled ? text.errorTooManyRequests : text.errorSubmitFailed);
        } finally {
            setIsSubmitting(false);
        }
    };

    const fieldClasses = (hasError: boolean) =>
        `w-full px-3 py-2 border rounded-md text-sm text-gray-900 outline-none transition focus:ring-2 ${
            hasError
                ? 'border-red-400 focus:ring-red-400 focus:border-red-400'
                : 'border-gray-300 focus:ring-blue-500 focus:border-blue-500'
        }`;

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />

            <main className="grow max-w-3xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                {/* Przełącznik języka nad treścią — znalazca z zagranicy widzi go od razu. */}
                <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-6">
                    <span className="text-sm text-gray-600">{text.languageSelectorLabel}:</span>
                    <div className="flex gap-2" role="group" aria-label={text.languageSelectorLabel}>
                        {LANGUAGE_ORDER.map(code => (
                            <button
                                key={code}
                                type="button"
                                onClick={() => setLanguage(code)}
                                aria-pressed={language === code}
                                className={`px-3 py-1.5 rounded-md text-sm font-semibold border transition ${
                                    language === code
                                        ? 'bg-blue-600 text-white border-blue-600'
                                        : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-100'
                                }`}
                            >
                                {FORM_TRANSLATIONS[code].languageName}
                            </button>
                        ))}
                    </div>

                    {isAdmin && (
                        <Link
                            href="/found-pigeons/admin"
                            className="sm:ml-auto text-sm font-semibold text-blue-700 hover:text-blue-900 hover:underline whitespace-nowrap"
                        >
                            Panel zgłoszeń →
                        </Link>
                    )}
                </div>

                <div className="border-b border-gray-200 pb-5 mb-6">
                    <h1 className="text-3xl font-bold text-gray-900 [overflow-wrap:anywhere]">{text.pageTitle}</h1>
                    <p className="mt-3 text-sm text-gray-600 leading-relaxed">{text.pageLead}</p>
                </div>

                {isSubmitted ? (
                    <div className="bg-white rounded-lg shadow-sm border border-green-200 p-8 text-center">
                        <div className="w-12 h-12 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
                            <svg className="w-7 h-7 text-green-700" viewBox="0 -960 960 960" fill="currentColor">
                                <path d="M382-240 154-468l57-57 171 171 367-367 57 57-424 424Z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-gray-900">{text.successTitle}</h2>
                        <p className="mt-2 text-sm text-gray-600 leading-relaxed">{text.successBody}</p>
                        <button
                            type="button"
                            onClick={() => setIsSubmitted(false)}
                            className="mt-6 px-5 py-2.5 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition"
                        >
                            {text.submitAnother}
                        </button>
                    </div>
                ) : (
                    <>
                        {/* Krótkie wyjaśnienie, zanim znalazca zacznie wypełniać pola. */}
                        <section className="bg-white rounded-lg border border-gray-200 p-5 mb-6">
                            <h2 className="text-sm font-bold text-gray-900 mb-3">{text.howItWorksTitle}</h2>
                            <ol className="space-y-2">
                                {text.howItWorksSteps.map((step, index) => (
                                    <li key={step} className="flex gap-3 text-sm text-gray-700">
                                        <span className="shrink-0 w-6 h-6 rounded-full bg-blue-100 text-blue-800 font-bold text-xs flex items-center justify-center">
                                            {index + 1}
                                        </span>
                                        <span className="leading-relaxed">{step}</span>
                                    </li>
                                ))}
                            </ol>
                        </section>

                        <form
                            onSubmit={handleSubmit}
                            noValidate
                            className="bg-white rounded-lg shadow-sm border border-gray-200 p-6"
                        >
                            <h2 className="text-lg font-bold text-gray-900 mb-5">{text.formTitle}</h2>

                            <div className="mb-6">
                                <label htmlFor="ring-number" className="block text-sm font-medium text-gray-700 mb-1">
                                    {text.ringNumberLabel} <span className="text-red-500">*</span>
                                </label>
                                <input
                                    id="ring-number"
                                    type="text"
                                    maxLength={64}
                                    value={values.ringNumber}
                                    onChange={event => setValue('ringNumber', event.target.value)}
                                    placeholder={text.ringNumberPlaceholder}
                                    aria-invalid={Boolean(errors.ringNumber)}
                                    aria-describedby="ring-number-hint"
                                    className={fieldClasses(Boolean(errors.ringNumber))}
                                />
                                <p id="ring-number-hint" className="mt-1 text-xs text-gray-500">
                                    {text.ringNumberHint}
                                </p>
                                {errors.ringNumber && (
                                    <p className="mt-1 text-sm text-red-600">{errors.ringNumber}</p>
                                )}
                            </div>

                            <fieldset className="mb-6">
                                <legend className="text-sm font-medium text-gray-700 mb-1">
                                    {text.contactSectionTitle} <span className="text-red-500">*</span>
                                </legend>
                                <p className="text-xs text-gray-500 mb-3">{text.contactSectionHint}</p>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                    <div>
                                        <label htmlFor="contact-phone" className="block text-sm text-gray-700 mb-1">
                                            {text.phoneLabel}
                                        </label>
                                        <input
                                            id="contact-phone"
                                            type="tel"
                                            inputMode="tel"
                                            maxLength={32}
                                            value={values.contactPhone}
                                            onChange={event => setValue('contactPhone', event.target.value)}
                                            placeholder={text.phonePlaceholder}
                                            aria-invalid={Boolean(errors.contactPhone || errors.contact)}
                                            className={fieldClasses(Boolean(errors.contactPhone || errors.contact))}
                                        />
                                        {errors.contactPhone && (
                                            <p className="mt-1 text-sm text-red-600">{errors.contactPhone}</p>
                                        )}
                                    </div>

                                    <div>
                                        <label htmlFor="contact-email" className="block text-sm text-gray-700 mb-1">
                                            {text.emailLabel}
                                        </label>
                                        <input
                                            id="contact-email"
                                            type="email"
                                            maxLength={320}
                                            value={values.contactEmail}
                                            onChange={event => setValue('contactEmail', event.target.value)}
                                            placeholder={text.emailPlaceholder}
                                            aria-invalid={Boolean(errors.contactEmail || errors.contact)}
                                            className={fieldClasses(Boolean(errors.contactEmail || errors.contact))}
                                        />
                                        {errors.contactEmail && (
                                            <p className="mt-1 text-sm text-red-600">{errors.contactEmail}</p>
                                        )}
                                    </div>
                                </div>

                                {errors.contact && (
                                    <p className="mt-2 text-sm text-red-600">{errors.contact}</p>
                                )}
                            </fieldset>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
                                <div>
                                    <label htmlFor="found-country" className="block text-sm font-medium text-gray-700 mb-1">
                                        {text.countryLabel}{' '}
                                        <span className="text-gray-400 font-normal">({text.optionalSuffix})</span>
                                    </label>
                                    <input
                                        id="found-country"
                                        type="text"
                                        maxLength={100}
                                        value={values.foundCountry}
                                        onChange={event => setValue('foundCountry', event.target.value)}
                                        placeholder={text.countryPlaceholder}
                                        className={fieldClasses(false)}
                                    />
                                </div>

                                <div>
                                    <label htmlFor="found-location" className="block text-sm font-medium text-gray-700 mb-1">
                                        {text.locationLabel}{' '}
                                        <span className="text-gray-400 font-normal">({text.optionalSuffix})</span>
                                    </label>
                                    <input
                                        id="found-location"
                                        type="text"
                                        maxLength={150}
                                        value={values.foundLocation}
                                        onChange={event => setValue('foundLocation', event.target.value)}
                                        placeholder={text.locationPlaceholder}
                                        className={fieldClasses(false)}
                                    />
                                </div>
                            </div>

                            <div className="mb-6">
                                <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                                    {text.descriptionLabel}{' '}
                                    <span className="text-gray-400 font-normal">({text.optionalSuffix})</span>
                                </label>
                                <textarea
                                    id="description"
                                    rows={4}
                                    maxLength={MAX_DESCRIPTION_LENGTH}
                                    value={values.description}
                                    onChange={event => setValue('description', event.target.value)}
                                    placeholder={text.descriptionPlaceholder}
                                    aria-invalid={Boolean(errors.description)}
                                    className={`${fieldClasses(Boolean(errors.description))} resize-y`}
                                />
                                <p className="mt-1 text-xs text-gray-500">
                                    {text.descriptionCounter(descriptionUsed, MAX_DESCRIPTION_LENGTH)}
                                </p>
                                {errors.description && (
                                    <p className="mt-1 text-sm text-red-600">{errors.description}</p>
                                )}
                            </div>

                            {/* Informacja o przetwarzaniu danych kontaktowych — wymóg modułu. */}
                            <section className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                                <h3 className="text-sm font-bold text-blue-900 mb-1">{text.privacyTitle}</h3>
                                <p className="text-sm text-blue-900/90 leading-relaxed">{text.privacyBody}</p>
                            </section>

                            {submitError && (
                                <p
                                    role="alert"
                                    className="mb-4 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2"
                                >
                                    {submitError}
                                </p>
                            )}

                            <div className="flex justify-end">
                                <button
                                    type="submit"
                                    disabled={isSubmitting}
                                    className="px-5 py-2.5 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center gap-2"
                                >
                                    {isSubmitting && (
                                        <span className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></span>
                                    )}
                                    {isSubmitting ? text.submittingButton : text.submitButton}
                                </button>
                            </div>
                        </form>
                    </>
                )}
            </main>

            <Footer />
        </div>
    );
}
