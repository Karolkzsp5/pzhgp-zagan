import type { Metadata } from 'next';
import Footer from '@/app/components/Footer';
import Navbar from '@/app/components/Navbar';
import NotFoundContent from '@/app/components/NotFoundContent';
import { NOT_FOUND_TITLE } from '@/app/utils/metadata';

export const metadata: Metadata = {
    title: NOT_FOUND_TITLE
};

/**
 * Strona 404 dla adresów, które nie pasują do żadnej podstrony. Nie leży w layoucie (main),
 * dlatego sama dodaje nawigację i stopkę.
 */
export default function NotFound() {
    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />
            <NotFoundContent />
            <Footer />
        </div>
    );
}
