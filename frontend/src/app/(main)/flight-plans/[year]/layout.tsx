import type { Metadata } from 'next';

interface FlightPlanYearLayoutProps {
    children: React.ReactNode;
    params: Promise<{ year: string }>;
}

// Tytuł karty z rokiem z adresu, np. "Plan lotów 2026". Przy niepoprawnym roku zostaje tytuł nadrzędny.
export async function generateMetadata({ params }: Omit<FlightPlanYearLayoutProps, 'children'>): Promise<Metadata> {
    const { year } = await params;
    return /^\d{4}$/.test(year) ? { title: `Plan lotów ${year}` } : {};
}

export default function FlightPlanYearLayout({ children }: Readonly<FlightPlanYearLayoutProps>) {
    return children;
}
