import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Zarząd' };

export default function BoardLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
