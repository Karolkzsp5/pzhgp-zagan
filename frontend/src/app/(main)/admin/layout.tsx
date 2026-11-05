import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Panel administratora' };

export default function AdminLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
