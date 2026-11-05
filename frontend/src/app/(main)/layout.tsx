import Footer from '@/app/components/Footer';
import Navbar from '@/app/components/Navbar';

/**
 * Wspólny układ podstron z nawigacją i stopką (bez logowania i rejestracji).
 * Layout zostaje na miejscu przy przejściu między podstronami, więc Navbar nie montuje się od nowa.
 */
export default function MainLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />
            {children}
            <Footer />
        </div>
    );
}
