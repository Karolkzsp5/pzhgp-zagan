'use client';

import ErrorFallback from '@/app/components/ErrorFallback';

/** Błąd podstrony z paskiem nawigacji. Granica błędów leży pod layoutem (main), więc Navbar i stopka zostają na miejscu. */
export default function MainError({ retry }: { retry: () => void }) {
    return <ErrorFallback retry={retry} />;
}