import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Mapy lotów' };

export default function FlightsLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
