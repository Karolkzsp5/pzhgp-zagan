import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Zgłoszenia znalezionych gołębi' };

export default function FoundPigeonsAdminLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
