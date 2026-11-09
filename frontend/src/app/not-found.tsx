import type { Metadata } from 'next';
import Link from 'next/link';
import Footer from '@/app/components/Footer';
import Navbar from '@/app/components/Navbar';

export const metadata: Metadata = {
    title: 'Nie znaleziono strony'
};

/** Strona 404 dla adresów, które nie pasują do żadnej podstrony. */
export default function NotFound() {
    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />
            <main className="grow flex flex-col items-center justify-center py-16 px-4 text-center">
                <p className="text-base font-bold text-blue-600 mb-1">
                    Błąd 404
                </p>
                <h1 className="text-2xl font-bold text-gray-900 mb-5">
                    Nie znaleziono strony
                </h1>
                <Link href="/" className="inline-block px-5 py-2.5 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition">
                    Wróć na stronę główną
                </Link>
            </main>
            <Footer />
        </div>
    );
}