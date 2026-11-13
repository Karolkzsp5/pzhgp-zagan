'use client';

import { ChangeEvent, FormEvent, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import Modal from '@/app/components/Modal';
import PasswordStrengthMeter from '@/app/components/PasswordStrengthMeter';
import TurnstileWidget, { TurnstileWidgetHandle } from '@/app/components/TurnstileWidget';
import { authService } from '@/app/services/authService';
import { sectionService } from '@/app/services/sectionService';
import { RegistrationRequest } from '@/app/types/auth';
import { SectionDto } from '@/app/types/section';
import { getErrorMessage } from '@/app/utils/apiClient';
import { HOUSE_NUMBER_PATTERN, formatHouseNumberInput, formatNameInput, formatPhoneInput, formatPostalCodeInput, formatStreetInput } from '@/app/utils/formatters';
import { validateStrongPassword } from '@/app/utils/password';

// Token CAPTCHA nie jest polem formularza: przekazuje go widżet Turnstile.
interface RegistrationFormData extends Omit<RegistrationRequest, 'captchaToken'> {
    confirmPassword: string;
}

// Backend przyjmuje wyłącznie datę z przeszłości (@Past), więc ostatni dozwolony dzień to wczoraj.
const getLatestBirthDate = () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const month = String(yesterday.getMonth() + 1).padStart(2, '0');
    const day = String(yesterday.getDate()).padStart(2, '0');
    return `${yesterday.getFullYear()}-${month}-${day}`;
};

export default function RegisterPage() {
    const router = useRouter();
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [sections, setSections] = useState<SectionDto[]>([]);
    const [sectionsError, setSectionsError] = useState('');
    const [isRegistrationSuccessful, setIsRegistrationSuccessful] = useState(false);
    const [error, setError] = useState('');
    // Ustawiane po załadowaniu w przeglądarce, żeby statycznie wygenerowana strona nie miała daty z dnia budowania.
    const [latestBirthDate, setLatestBirthDate] = useState<string | undefined>(undefined);
    const [captchaToken, setCaptchaToken] = useState<string | null>(null);
    const captchaRef = useRef<TurnstileWidgetHandle>(null);

    const [formData, setFormData] = useState<RegistrationFormData>({
        name: '',
        surname: '',
        dateOfBirth: '',
        email: '',
        phoneNumber: '',
        password: '',
        confirmPassword: '',
        sectionId: 0,
        postalCode: '',
        city: '',
        street: '',
        houseNumber: ''
    });

    useEffect(() => {
        setLatestBirthDate(getLatestBirthDate());
    }, []);

    useEffect(() => {
        const fetchSections = async () => {
            setSectionsError('');

            try {
                setSections(await sectionService.getSections());
            } catch (error) {
                setSectionsError(getErrorMessage(error, 'Nie udało się pobrać listy sekcji. Odśwież stronę i spróbuj ponownie.'));
            }
        };

        void fetchSections();
    }, []);

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;

        let formattedValue: string | number = value;

        if (name === 'phoneNumber') {
            formattedValue = formatPhoneInput(value);
        } else if (name === 'postalCode') {
            formattedValue = formatPostalCodeInput(value, formData.postalCode);
        } else if (['name', 'surname', 'city'].includes(name)) {
            formattedValue = formatNameInput(value);
        } else if (name === 'street') {
            formattedValue = formatStreetInput(value);
        } else if (name === 'sectionId') {
            formattedValue = parseInt(value) || 0;
        } else if (name === 'houseNumber') {
            formattedValue = formatHouseNumberInput(value);
        }

        setFormData((prev) => ({
            ...prev,
            [name]: formattedValue
        }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        if (formData.dateOfBirth > getLatestBirthDate()) {
            setError('Data urodzenia musi być z przeszłości.');
            return;
        }

        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!emailRegex.test(formData.email)) {
            setError('Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).');
            return;
        }

        const rawPhoneNumber = formData.phoneNumber.replace(/\s+/g, '');
        if (rawPhoneNumber.length !== 9) {
            setError('Numer telefonu musi zawierać dokładnie 9 cyfr.');
            return;
        }

        const passwordError = validateStrongPassword(formData.password);
        if (passwordError) {
            setError(passwordError);
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            setError('Podane hasła nie są identyczne.');
            return;
        }

        if (formData.sectionId === 0) {
            setError('Proszę wybrać sekcję, do której chcesz należeć.');
            return;
        }

        if (!HOUSE_NUMBER_PATTERN.test(formData.houseNumber)) {
            setError('Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4).');
            return;
        }

        if (!captchaToken) {
            setError('Dokończ weryfikację CAPTCHA przed wysłaniem formularza.');
            return;
        }

        setIsLoading(true);
        try {
            const {
                confirmPassword: _confirmPassword,
                ...registrationData
            } = formData;

            const cleanDataToSend = {
                ...registrationData,
                phoneNumber: rawPhoneNumber,
                captchaToken
            };

            await authService.register(cleanDataToSend);
            setIsRegistrationSuccessful(true);
        } catch (error) {
            setError(getErrorMessage(error, 'Wystąpił błąd podczas rejestracji.'));
            // Token CAPTCHA jest jednorazowy, więc kolejna próba wymaga nowego.
            captchaRef.current?.reset();
        } finally {
            setIsLoading(false);
        }
    };

    const closeRegistrationModal = () => {
        setIsRegistrationSuccessful(false);
        router.push('/');
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-2xl">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Rejestracja Hodowcy PZHGP Żagań</h1>
                {sectionsError && <div className="mb-4 p-3 rounded bg-red-100 text-red-700 text-sm">{sectionsError}</div>}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-1 gap-4">
                        <div>
                            <label htmlFor="name" className="block text-sm font-medium text-gray-700">Imię</label>
                            <input id="name"
                                   type="text"
                                   name="name"
                                   autoComplete="given-name"
                                   value={formData.name}
                                   onChange={handleChange}
                                   required
                                   maxLength={32}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="surname" className="block text-sm font-medium text-gray-700">Nazwisko</label>
                            <input id="surname"
                                   type="text"
                                   name="surname"
                                   autoComplete="family-name"
                                   value={formData.surname}
                                   onChange={handleChange}
                                   required
                                   maxLength={64}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="dateOfBirth" className="block text-sm font-medium text-gray-700">Data urodzenia</label>
                            <input id="dateOfBirth"
                                   type="date"
                                   name="dateOfBirth"
                                   autoComplete="bday"
                                   max={latestBirthDate}
                                   value={formData.dateOfBirth}
                                   onChange={handleChange}
                                   required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-700">Adres e-mail</label>
                            <input id="email"
                                   type="email"
                                   name="email"
                                   autoComplete="email"
                                   value={formData.email}
                                   onChange={handleChange}
                                   required
                                   maxLength={320}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-700">Numer telefonu</label>
                            <input id="phoneNumber"
                                   type="tel"
                                   name="phoneNumber"
                                   autoComplete="tel"
                                   inputMode="tel"
                                   value={formData.phoneNumber}
                                   onChange={handleChange}
                                   required
                                   maxLength={11}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-700">Hasło</label>
                            <input id="password"
                                   type="password"
                                   name="password"
                                   autoComplete="new-password"
                                   value={formData.password}
                                   onChange={handleChange}
                                   required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />

                            <PasswordStrengthMeter password={formData.password} />
                        </div>

                        <div>
                            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">Powtórz hasło</label>
                            <input id="confirmPassword"
                                   type="password"
                                   name="confirmPassword"
                                   autoComplete="new-password"
                                   value={formData.confirmPassword}
                                   onChange={handleChange}
                                   required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="sectionId" className="block text-sm font-medium text-gray-700">Wybierz sekcję, do której chcesz należeć</label>
                            <select id="sectionId"
                                    name="sectionId"
                                    value={formData.sectionId}
                                    onChange={handleChange}
                                    required
                                    disabled={sections.length === 0}
                                    className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 bg-white disabled:bg-gray-100">
                                <option value={0}>Wybierz</option>
                                {sections.map((section) => (
                                    <option key={section.id} value={section.id}>
                                        {section.name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label htmlFor="postalCode" className="block text-sm font-medium text-gray-700">Kod pocztowy</label>
                            <input id="postalCode"
                                   type="text"
                                   name="postalCode"
                                   autoComplete="postal-code"
                                   value={formData.postalCode}
                                   onChange={handleChange}
                                   required
                                   maxLength={6}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="city" className="block text-sm font-medium text-gray-700">Miejscowość</label>
                            <input id="city"
                                   type="text"
                                   name="city"
                                   autoComplete="address-level2"
                                   value={formData.city}
                                   onChange={handleChange}
                                   required
                                   maxLength={100}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="street" className="block text-sm font-medium text-gray-700">Ulica</label>
                            <input id="street"
                                   type="text"
                                   name="street"
                                   autoComplete="address-line1"
                                   value={formData.street}
                                   onChange={handleChange}
                                   required
                                   maxLength={100}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label htmlFor="houseNumber" className="block text-sm font-medium text-gray-700">Numer domu/lokalu</label>
                            <input id="houseNumber"
                                   type="text"
                                   name="houseNumber"
                                   autoComplete="address-line2"
                                   value={formData.houseNumber}
                                   onChange={handleChange}
                                   required
                                   maxLength={10}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>
                    </div>

                    <TurnstileWidget ref={captchaRef} onTokenChange={setCaptchaToken} />

                    {error && (
                        <div className="p-4 my-4 rounded bg-red-100 text-red-700" role="alert">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading || sections.length === 0}
                        className="w-full mt-6 bg-blue-600 text-white font-bold py-2 px-4 rounded hover:bg-blue-700 transition duration-200 disabled:bg-blue-400 disabled:cursor-wait flex justify-center items-center gap-2"
                    >
                        {isLoading && (
                            <span className="animate-spin rounded-full h-5 w-5 border-2 border-white/30 border-t-white" aria-hidden="true"/>
                        )}
                        {isLoading ? 'Wysyłanie...' : 'Zarejestruj się'}
                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-600 border-t pt-4">
                    Masz już konto?{' '}
                    <Link href="/login" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                        Zaloguj się tutaj
                    </Link>
                </div>
            </div>
            <Modal isOpen={isRegistrationSuccessful} title="Rejestracja przebiegła pomyślnie" onClose={closeRegistrationModal} maxWidthClass="max-w-md">
                <div className="p-6 text-center">
                    <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-6">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-green-600" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                            <path d="M389-267 195-460l51-52 143 143 325-324 51 51-376 375Z" />
                        </svg>
                    </div>

                    <p className="text-sm text-gray-600 mb-8 leading-relaxed">
                        Twoje konto zostało pomyślnie utworzone i oczekuje na akceptację administratora.
                        Skontaktuj się z zarządem sekcji w celu weryfikacji tożsamości.
                        Po akceptacji logowanie będzie możliwe.
                    </p>

                    <button
                        type="button"
                        onClick={closeRegistrationModal}
                        className="w-full px-4 py-3 bg-blue-600 text-base font-bold text-white rounded-md hover:bg-blue-700 transition">
                        Rozumiem
                    </button>
                </div>
            </Modal>
        </div>
    );
}
