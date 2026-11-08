'use client';

import Link from 'next/link';

/**
 * Widok awaryjny, gdy podstrona rzuci nieobsłużony błąd podczas renderowania.
 * Błędy pobierania danych podstrony obsługują same (ErrorState), więc ten widok oznacza błąd w kodzie.
 */
export default function ErrorPage({ retry }: { retry: () => void }) {
    return (
        <main className="grow flex items-center justify-center bg-gray-50 py-16 px-4">
            <div className="max-w-md text-center">
                <h1 className="text-2xl font-bold text-gray-900 mb-3">Coś poszło nie tak</h1>
                <p className="text-sm text-gray-600 mb-6">
                    Wystąpił nieoczekiwany błąd. Spróbuj ponownie albo wróć na stronę główną.
                </p>
                <div className="flex flex-col sm:flex-row justify-center gap-3">
                    <button type="button" onClick={() => retry()}
                            className="px-5 py-2.5 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition"
                    >
                        Spróbuj ponownie
                    </button>
                    <Link href="/"
                          className="px-5 py-2.5 border border-gray-300 bg-white text-gray-700 rounded-md text-sm font-bold hover:bg-gray-100 transition"
                    >
                        Strona główna
                    </Link>
                </div>
            </div>
        </main>
    );
}