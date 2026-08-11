"use client";

import { Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';

function ModalContent() {
    const searchParams = useSearchParams();
    const router = useRouter();

    const showModal = searchParams.get('registered') === 'true';

    const closeModal = () => {
        router.replace('/');
    };

    if (!showModal) return null;

    return (
        <div className="fixed inset-0 bg-gray-50/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity">
            <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-2xl transform transition-all">
                <div className="text-center">
                    <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-green-100 mb-6">
                        <svg className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                        </svg>
                    </div>
                    <h3 className="text-xl font-bold text-gray-900 mb-3">
                        Rejestracja przebiegła pomyślnie
                    </h3>
                    <p className="text-sm text-gray-600 mb-8 leading-relaxed">
                        Twoje konto zostało pomyślnie utworzone i oczekuje na akceptację administratora.
                        Skontaktuj się z zarządem sekcji w celu weryfikacji tożsamości.
                        Po akceptacji logowanie będzie możliwe.
                    </p>
                    <button
                        onClick={closeModal}
                        className="w-full inline-flex justify-center rounded-md shadow-sm px-4 py-3 bg-blue-600 text-base font-bold text-white hover:bg-blue-700 focus:outline-none transition duration-200"
                    >
                        Rozumiem
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function RegistrationModal() {
    return (
        <Suspense fallback={null}>
            <ModalContent />
        </Suspense>
    );
}