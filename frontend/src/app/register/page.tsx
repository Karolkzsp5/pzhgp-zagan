"use client";

import { useState, ChangeEvent, FormEvent } from 'react';

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

    const [formData, setFormData] = useState<RegistrationFormData>({
        name: '',
        surname: '',
        dateOfBirth: '',
        email: '',
        phoneNumber: '',
        password: '',
        confirmPassword: '',
        sectionId: 1, // Domyślnie Żagań
        postalCode: '',
        city: '',
        street: '',
        houseNumber: ''
    });

    const [message, setMessage] = useState<MessageState>({ text: '', type: '' });

    const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: name === 'sectionId' ? parseInt(value) || 1 : value
        }));
    };

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (formData.password !== formData.confirmPassword) {
            setMessage({ text: 'Podane hasła nie są identyczne!', type: 'error' });
            return;
        }

        setMessage({ text: 'Wysyłanie...', type: 'info' });

        try {
            const { confirmPassword, ...dataToSend } = formData;

            const response = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(dataToSend),
            });

            const data = await response.text();

            if (response.status === 201) {
                setMessage({ text: data, type: 'success' });
            } else {
                setMessage({ text: data, type: 'error' });
            }
        } catch (error) {
            setMessage({ text: 'Błąd połączenia z serwerem. Upewnij się, że backend jest uruchomiony.', type: 'error' });
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
            <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-2xl">
                <h1 className="text-2xl font-bold mb-6 text-gray-800 border-b pb-2">Rejestracja Hodowcy PZHGP Żagań</h1>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-1 gap-4">

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Imię</label>
                            <input type="text" name="name" value={formData.name} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Nazwisko</label>
                            <input type="text" name="surname" value={formData.surname} onChange={handleChange} required
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
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Numer telefonu</label>
                            <input type="text" name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Hasło</label>
                            <input type="password" name="password" value={formData.password} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Powtórz hasło</label>
                            <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Sekcja</label>
                            <select name="sectionId" value={formData.sectionId} onChange={handleChange} required
                                    className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500 bg-white">
                                <option value={1}>Żagań</option>
                                <option value={2}>Wymiarki</option>
                                <option value={3}>Chotków</option>
                                <option value={4}>Kożuchów</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Kod pocztowy</label>
                            <input type="text" name="postalCode" value={formData.postalCode} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Miejscowość</label>
                            <input type="text" name="city" value={formData.city} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Ulica</label>
                            <input type="text" name="street" value={formData.street} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700">Numer domu/lokalu</label>
                            <input type="text" name="houseNumber" value={formData.houseNumber} onChange={handleChange} required
                                   className="mt-1 block w-full p-2 border text-gray-700 border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500" />
                        </div>

                    </div>

                    {message.text && (
                        <div className={`p-4 mb-6 rounded ${
                            message.type === 'success' ? 'bg-green-100 text-green-700' :
                                message.type === 'error' ? 'bg-red-100 text-red-700' :
                                    'bg-blue-100 text-blue-700'
                        }`}>
                            {message.text}
                        </div>
                    )}

                    <button type="submit"
                            className="w-full mt-6 bg-blue-600 text-white font-bold py-2 px-4 rounded hover:bg-blue-700 transition duration-200">
                        Zarejestruj się
                    </button>
                </form>
            </div>
        </div>
    );
}