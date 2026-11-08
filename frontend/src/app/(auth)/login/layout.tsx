import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Logowanie' };

export default function LoginLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
