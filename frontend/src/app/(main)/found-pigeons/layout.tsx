import type { Metadata } from 'next';
import { TITLE_TEMPLATE } from '@/app/utils/metadata';

// Strona jest komponentem klienckim, więc tytuł karty ustawia jej layout.
// Szablon trzeba podać ponownie, bo Next nie przenosi go na podstrony segmentu z własnym tytułem.
export const metadata: Metadata = { title: { default: 'Znalezione gołębie', template: TITLE_TEMPLATE } };

export default function FoundPigeonsLayout({ children }: Readonly<{ children: React.ReactNode }>) {
    return children;
}
