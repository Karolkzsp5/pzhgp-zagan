'use client';

import { FormEvent, useState } from 'react';
import { accountService } from '@/app/services/accountService';
import { AccountDto, ContactUpdateRequest } from '@/app/types/account';
import { getErrorMessage } from '@/app/utils/apiClient';
import { HOUSE_NUMBER_PATTERN, formatHouseNumberInput, formatNameInput, formatPhoneInput, formatPhoneNumber, formatPostalCodeInput, formatStreetInput } from '@/app/utils/formatters';

interface ContactDetailsFormProps {
    account: AccountDto;
    onSaved: (account: AccountDto) => void;
}

const INPUT_CLASS = 'mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 outline-none transition focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100';

const toFormValues = (account: AccountDto): ContactUpdateRequest => ({
    phoneNumber: formatPhoneInput(account.phoneNumber),
    postalCode: account.postalCode,
    city: account.city,
    street: account.street,
    houseNumber: account.houseNumber
});

/** Telefon i adres: podgląd z przyciskiem edycji. Pozostałe dane konta zmienia administrator. */
export default function ContactDetailsForm({ account, onSaved }: ContactDetailsFormProps) {
    const [isEditing, setIsEditing] = useState(false);
    const [values, setValues] = useState<ContactUpdateRequest>(() => toFormValues(account));
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    const startEditing = () => {
        setValues(toFormValues(account));
        setError('');
        setSuccessMessage('');
        setIsEditing(true);
    };

    const cancelEditing = () => {
        setError('');
        setIsEditing(false);
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        const phoneNumber = values.phoneNumber.replace(/\s+/g, '');
        if (phoneNumber.length !== 9) {
            setError('Numer telefonu musi zawierać dokładnie 9 cyfr.');
            return;
        }

        if (!/^\d{2}-\d{3}$/.test(values.postalCode)) {
            setError('Podaj kod pocztowy w formacie XX-XXX.');
            return;
        }

        if (!values.city.trim() || !values.street.trim()) {
            setError('Podaj miejscowość i ulicę.');
            return;
        }

        if (!HOUSE_NUMBER_PATTERN.test(values.houseNumber)) {
            setError('Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4).');
            return;
        }

        setIsSaving(true);
        try {
            const updated = await accountService.updateContact({ ...values, phoneNumber });
            onSaved(updated);
            setIsEditing(false);
            setSuccessMessage('Dane kontaktowe zostały zapisane.');
        } catch (error) {
            setError(getErrorMessage(error, 'Nie udało się zapisać danych kontaktowych.'));
        } finally {
            setIsSaving(false);
        }
    };

    if (!isEditing) {
        return (
            <div>
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <dt className="text-xs text-gray-500 uppercase tracking-wider">Telefon</dt>
                        <dd className="mt-1 text-sm font-medium text-gray-800">{formatPhoneNumber(account.phoneNumber)}</dd>
                    </div>
                    <div>
                        <dt className="text-xs text-gray-500 uppercase tracking-wider">Adres zamieszkania</dt>
                        <dd className="mt-1 text-sm font-medium text-gray-800">
                            ul. {account.street} {account.houseNumber}<br />
                            {account.postalCode} {account.city}
                        </dd>
                    </div>
                </dl>

                {successMessage && (
                    <p role="status" className="mt-4 text-sm text-green-700 bg-green-50 border border-green-200 rounded-md px-3 py-2">
                        {successMessage}
                    </p>
                )}

                <button
                    type="button"
                    onClick={startEditing}
                    className="mt-5 px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md text-sm font-bold hover:bg-gray-100 transition"
                >
                    Zmień dane kontaktowe
                </button>
            </div>
        );
    }

    return (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
                <label htmlFor="contact-phone" className="block text-sm font-medium text-gray-700">Numer telefonu</label>
                <input id="contact-phone" type="tel" inputMode="tel" autoComplete="tel" maxLength={11}
                       value={values.phoneNumber}
                       onChange={(e) => setValues(prev => ({ ...prev, phoneNumber: formatPhoneInput(e.target.value) }))}
                       disabled={isSaving}
                       className={INPUT_CLASS} />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                    <label htmlFor="contact-postal-code" className="block text-sm font-medium text-gray-700">Kod pocztowy</label>
                    <input id="contact-postal-code" type="text" autoComplete="postal-code" maxLength={6}
                           value={values.postalCode}
                           onChange={(e) => setValues(prev => ({ ...prev, postalCode: formatPostalCodeInput(e.target.value, prev.postalCode) }))}
                           disabled={isSaving}
                           className={INPUT_CLASS} />
                </div>
                <div className="sm:col-span-2">
                    <label htmlFor="contact-city" className="block text-sm font-medium text-gray-700">Miejscowość</label>
                    <input id="contact-city" type="text" autoComplete="address-level2" maxLength={100}
                           value={values.city}
                           onChange={(e) => setValues(prev => ({ ...prev, city: formatNameInput(e.target.value) }))}
                           disabled={isSaving}
                           className={INPUT_CLASS} />
                </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="sm:col-span-2">
                    <label htmlFor="contact-street" className="block text-sm font-medium text-gray-700">Ulica</label>
                    <input id="contact-street" type="text" autoComplete="address-line1" maxLength={100}
                           value={values.street}
                           onChange={(e) => setValues(prev => ({ ...prev, street: formatStreetInput(e.target.value) }))}
                           disabled={isSaving}
                           className={INPUT_CLASS} />
                </div>
                <div>
                    <label htmlFor="contact-house-number" className="block text-sm font-medium text-gray-700">Numer domu/lokalu</label>
                    <input id="contact-house-number" type="text" autoComplete="address-line2" maxLength={10}
                           value={values.houseNumber}
                           onChange={(e) => setValues(prev => ({ ...prev, houseNumber: formatHouseNumberInput(e.target.value) }))}
                           disabled={isSaving}
                           className={INPUT_CLASS} />
                </div>
            </div>

            {error && (
                <p role="alert" className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">
                    {error}
                </p>
            )}

            <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 pt-1">
                <button
                    type="button"
                    onClick={cancelEditing}
                    disabled={isSaving}
                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md text-sm font-medium hover:bg-gray-100 transition disabled:opacity-50"
                >
                    Anuluj
                </button>
                <button
                    type="submit"
                    disabled={isSaving}
                    className="px-5 py-2 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition disabled:bg-blue-400 disabled:cursor-wait"
                >
                    {isSaving ? 'Zapisywanie...' : 'Zapisz zmiany'}
                </button>
            </div>
        </form>
    );
}