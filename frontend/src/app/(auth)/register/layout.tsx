import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Rejestracja' };

export default function RegisterLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
