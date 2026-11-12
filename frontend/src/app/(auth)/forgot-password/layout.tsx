import type { Metadata } from 'next';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
export const metadata: Metadata = { title: 'Odzyskiwanie hasła' };

export default function ForgotPasswordLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}