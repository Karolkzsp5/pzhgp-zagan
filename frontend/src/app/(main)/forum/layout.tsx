import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Forum' };

export default function ForumLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
