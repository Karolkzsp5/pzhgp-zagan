import type { Metadata } from 'next';
import { TITLE_TEMPLATE } from '@/app/utils/metadata';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
// Szablon trzeba podać ponownie, bo Next nie przenosi go na podstrony segmentu z własnym tytułem (np. strona 404).
export const metadata: Metadata = { title: { default: 'Mapy lotów', template: TITLE_TEMPLATE } };

export default function FlightsLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
