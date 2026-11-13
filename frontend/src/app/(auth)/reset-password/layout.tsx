import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Zmiana hasła' };

export default function ResetPasswordLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}