import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { NOT_FOUND_TITLE } from '@/app/utils/metadata';
import { parseRouteId } from '@/app/utils/routeParams';
import FlightPlanYearView from './FlightPlanYearView';

interface FlightPlanYearPageProps {
    params: Promise<{ year: string }>;
}

// Tytuł karty z rokiem z adresu, np. "Plan lotów 2026"; przy nieprawidłowym roku tytuł strony 404.
export async function generateMetadata({ params }: FlightPlanYearPageProps): Promise<Metadata> {
    const year = parseRouteId((await params).year);
    return { title: year === null ? NOT_FOUND_TITLE : `Plan lotów ${year}` };
}

/** Nieprawidłowy rok w adresie daje stronę 404, zanim przeglądarka zacznie pobierać dane. */
export default async function FlightPlanYearPage({ params }: FlightPlanYearPageProps) {
    const year = parseRouteId((await params).year);

    if (year === null) {
        notFound();
    }

    return <FlightPlanYearView year={year} />;
}