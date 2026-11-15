'use client';

import ErrorFallback from '@/app/components/ErrorFallback';

/**
 * Błąd poza podstronami z paskiem nawigacji: logowanie, rejestracja, odzyskiwanie hasła
 * oraz sam wspólny layout (Navbar, stopka).
 */
export default function RootError({ retry }: { retry: () => void }) {
    return <ErrorFallback retry={retry} />;
}