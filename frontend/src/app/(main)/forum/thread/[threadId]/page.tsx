import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { NOT_FOUND_TITLE } from '@/app/utils/metadata';
import { parseRouteId } from '@/app/utils/routeParams';
import ThreadView from './ThreadView';

interface ThreadPageProps {
    params: Promise<{ threadId: string }>;
}

// Przy nieprawidłowym identyfikatorze karta ma tytuł strony 404, a nie tytuł działu.
export async function generateMetadata({ params }: ThreadPageProps): Promise<Metadata> {
    return parseRouteId((await params).threadId) === null ? { title: NOT_FOUND_TITLE } : {};
}

/** Nieprawidłowy identyfikator w adresie daje stronę 404, zanim przeglądarka zacznie pobierać dane. */
export default async function ThreadPage({ params }: ThreadPageProps) {
    const threadId = parseRouteId((await params).threadId);

    if (threadId === null) {
        notFound();
    }

    return <ThreadView threadId={threadId} />;
}
