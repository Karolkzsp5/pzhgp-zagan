"use client";

import { useState, ChangeEvent, FormEvent } from 'react';
import Link from "next/link";
import { useRouter } from 'next/navigation';

interface RegistrationFormData {
    name: string;
    surname: string;
    dateOfBirth: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
    sectionId: number;
    email: string;
    phoneNumber: string;
    password: string;
    confirmPassword?: string;
    [key: string]: string | number | undefined;
}

interface MessageState {
    text: string;
    type: 'success' | 'error' | 'info' | '';
}

export default function RegisterPage() {
    const router = useRouter();

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

    const [message, setMessage] = useState<MessageState>({ text: '', type: '' });
    const [focusedField, setFocusedField] = useState<string | null>(null);

    const formatPhoneNumber = (val: string) => {
        const digits = val.replace(/\D/g, '').slice(0, 9);
        return digits.replace(/(\d{3})(?=\d)/g, '$1 ').trim();
    };

    const formatPostalCode = (val: string, prevVal: string) => {
        if (val.length < prevVal.length) {
            if (val.endsWith('-')) {
                return val.slice(0, -1);
            }
            return val;
        }

        const digits = val.replace(/\D/g, '').slice(0, 5);

        if (digits.length >= 2) {
            return `${digits.slice(0, 2)}-${digits.slice(2)}`;
        }
        return digits;
    };

    const formatTextOnly = (val: string) => {
        return val.replace(/[^a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\s-]/g, '');
    };

    const getPasswordStrength = (pass: string) => {
        let score = 0;
        if (!pass) return { score: 0, label: '', widthClass: 'w-0', colorClass: 'bg-transparent' };

        if (pass.length >= 8) score += 1;
        if (/[A-ZĄĆĘŁŃÓŚŹŻ]/.test(pass)) score += 1;
        if (/[a-ząćęłńóśźż]/.test(pass)) score += 1;
        if (/\d/.test(pass)) score += 1;
        if (/[!@#$%^&*(),.?":{}|<>_\-\=\+\/\\]/.test(pass)) score += 1;

        switch (score) {
            case 1:
                return { score, label: 'Bardzo słabe', widthClass: 'w-1/5', colorClass: 'bg-red-500' };
            case 2:
                return { score, label: 'Słabe', widthClass: 'w-2/5', colorClass: 'bg-red-500' };
            case 3:
                return { score, label: 'Średnie', widthClass: 'w-3/5', colorClass: 'bg-yellow-500' };
            case 4:
                return { score, label: 'Dobre', widthClass: 'w-4/5', colorClass: 'bg-blue-500' };
            case 5:
                return { score, label: 'Bardzo silne', widthClass: 'w-full', colorClass: 'bg-green-500' };
            default:
                return { score: 0, label: '', widthClass: 'w-0', colorClass: 'bg-transparent' };
        }
    };

    const validateStrongPassword = (pass: string): string | null => {
        if (pass.length < 8) return "Hasło musi zawierać co najmniej 8 znaków.";
        if (!/[A-ZĄĆĘŁŃÓŚŹŻ]/.test(pass)) return "Hasło musi zawierać co najmniej jedną dużą literę.";
        if (!/[a-ząćęłńóśźż]/.test(pass)) return "Hasło musi zawierać co najmniej jedną małą literę.";
        if (!/\d/.test(pass)) return "Hasło musi zawierać co najmniej jedną cyfrę.";
        if (!/[!@#$%^&*(),.?":{}|<>_\-\=\+\/\\]/.test(pass)) return "Hasło musi zawierać co najmniej jeden znak specjalny.";
        return null;
    };

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;

        let formattedValue: string | number = value;

        if (name === 'phoneNumber') {
            formattedValue = formatPhoneNumber(value);
        } else if (name === 'postalCode') {
            formattedValue = formatPostalCode(value, formData.postalCode);
        } else if (['name', 'surname', 'city', 'street'].includes(name)) {
            formattedValue = formatTextOnly(value);
        } else if (name === 'sectionId') {
            formattedValue = parseInt(value) || 0;
        } else if (name === 'houseNumber') {
            formattedValue = value.replace(/[^0-9a-zA-Z\s\/-]/g, '');
        }

        setFormData((prev) => ({
            ...prev,
            [name]: formattedValue
        }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!emailRegex.test(formData.email)) {
            setMessage({ text: 'Proszę podać prawidłowy adres e-mail (np. jan.kowalski@domena.pl).', type: 'error' });
            return;
        }

        const rawPhoneNumber = formData.phoneNumber.replace(/\s+/g, '');
        if (rawPhoneNumber.length !== 9) {
            setMessage({ text: 'Numer telefonu musi zawierać dokładnie 9 cyfr.', type: 'error' });
            return;
        }

        const passwordError = validateStrongPassword(formData.password);
        if (passwordError) {
            setMessage({ text: passwordError, type: 'error' });
            return;
        }

        if (formData.password !== formData.confirmPassword) {
            setMessage({ text: 'Podane hasła nie są identyczne.', type: 'error' });
            return;
        }

        if (formData.sectionId === 0) {
            setMessage({ text: 'Proszę wybrać sekcję do której chcesz należeć', type: 'error' });
            return;
        }

        const houseNumberRegex = /^[1-9]\d*\s?[a-zA-Z]?(\s?[\/-]\s?[1-9]\d*\s?[a-zA-Z]?)?$/;
        if (!houseNumberRegex.test(formData.houseNumber)) {
            setMessage({ text: 'Podaj poprawny numer domu/lokalu (np. 12, 12A, 12/4).', type: 'error' });
            return;
        }

        try {
            const { confirmPassword, ...dataToSend } = formData;

            const cleanDataToSend = {
                ...dataToSend,
                phoneNumber: formData.phoneNumber.replace(/\s+/g, '')
            };

            const response = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(cleanDataToSend),
            });

            const data = await response.text();

            if (response.status === 201) {
                router.push('/?registered=true');
            } else {
                setMessage({ text: data, type: 'error' });
            }
        } catch (error) {
            setMessage({ text: 'Błąd połączenia z serwerem.', type: 'error' });
        }
    };

    const passwordStrength = getPasswordStrength(formData.password);

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-2xl">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Rejestracja Hodowcy PZHGP Żagań</h1>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-1 gap-4">

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Imię</label>
                            <input type="text" name="name" value={formData.name} onChange={handleChange} required
                                   maxLength={32}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Nazwisko</label>
                            <input type="text" name="surname" value={formData.surname} onChange={handleChange} required
                                   maxLength={64}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Data urodzenia</label>
                            <input type="date" name="dateOfBirth" value={formData.dateOfBirth} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Adres E-mail</label>
                            <input type="email" name="email" value={formData.email} onChange={handleChange} required
                                   maxLength={320}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Numer telefonu</label>
                            <input type="text" name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} required
                                   maxLength={11}
                                   onFocus={() => setFocusedField('phoneNumber')}
                                   onBlur={() => setFocusedField(null)}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Hasło</label>
                            <input type="password" name="password" value={formData.password} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />

                            {formData.password.length > 0 && (
                                <div className="mt-2">
                                    <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                                        <div className={`h-1.5 rounded-full transition-all duration-300 ${passwordStrength.widthClass} ${passwordStrength.colorClass}`} />
                                    </div>
                                    <p className="text-xs text-gray-600 mt-1 flex justify-between">
                                        <span>Siła hasła: <strong className="text-gray-800">{passwordStrength.label}</strong></span>
                                        <span className="text-gray-400">{passwordStrength.score}/5</span>
                                    </p>
                                </div>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Powtórz hasło</label>
                            <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Wybierz sekcję do której chcesz należeć</label>
                            <select name="sectionId" value={formData.sectionId} onChange={handleChange} required
                                    className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 bg-white">
                                <option value={0}>Wybierz</option>
                                <option value={1}>Żagań</option>
                                <option value={2}>Wymiarki</option>
                                <option value={3}>Chotków</option>
                                <option value={4}>Kożuchów</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Kod pocztowy</label>
                            <input type="text" name="postalCode" value={formData.postalCode} onChange={handleChange} required
                                   maxLength={6}
                                   onFocus={() => setFocusedField('postalCode')}
                                   onBlur={() => setFocusedField(null)}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Miejscowość</label>
                            <input type="text" name="city" value={formData.city} onChange={handleChange} required
                                   maxLength={100}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Ulica</label>
                            <input type="text" name="street" value={formData.street} onChange={handleChange} required
                                   maxLength={100}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Numer domu/lokalu</label>
                            <input type="text" name="houseNumber" value={formData.houseNumber} onChange={handleChange} required
                                   maxLength={10}
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                    </div>

                    {message.text && (
                        <div className={`p-4 my-4 rounded ${
                            message.type === 'success' ? 'bg-green-100 text-green-700' :
                                message.type === 'error' ? 'bg-red-100 text-red-700' :
                                    'bg-blue-100 text-blue-700'
                        }`}>
                            {message.text}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={message.type === 'info'}
                        className="w-full mt-6 bg-blue-600 text-white font-bold py-2 px-4 rounded hover:bg-blue-700 transition duration-200 disabled:bg-blue-400 disabled:cursor-not-allowed disabled:hover:bg-blue-400 flex justify-center items-center"
                    >
                        {message.type === 'info' ? (
                            <>
                                <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                Wysyłanie...
                            </>
                        ) : (
                            'Zarejestruj się'
                        )}
                    </button>
                </form>

                <div className="mt-6 text-center text-sm text-gray-600 border-t pt-4">
                    Masz już konto?{' '}
                    <Link href="/login" className="text-blue-600 hover:text-blue-800 font-semibold transition duration-200">
                        Zaloguj się tutaj
                    </Link>
                </div>
            </div>
        </div>
    );
}