import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { NOT_FOUND_TITLE } from '@/app/utils/metadata';
import { parseRouteId } from '@/app/utils/routeParams';
import FlightDetailsView from './FlightDetailsView';

interface FlightDetailsPageProps {
    params: Promise<{ flightId: string }>;
}

// Przy nieprawidłowym identyfikatorze karta ma tytuł strony 404, a nie tytuł działu.
export async function generateMetadata({ params }: FlightDetailsPageProps): Promise<Metadata> {
    return parseRouteId((await params).flightId) === null ? { title: NOT_FOUND_TITLE } : {};
}

/** Nieprawidłowy identyfikator w adresie daje stronę 404, zanim przeglądarka zacznie pobierać dane. */
export default async function FlightDetailsPage({ params }: FlightDetailsPageProps) {
    const flightId = parseRouteId((await params).flightId);

    if (flightId === null) {
        notFound();
    }

    return <FlightDetailsView flightId={flightId} />;
}
