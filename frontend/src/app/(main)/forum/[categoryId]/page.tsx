import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { NOT_FOUND_TITLE } from '@/app/utils/metadata';
import { parseRouteId } from '@/app/utils/routeParams';
import CategoryView from './CategoryView';

interface CategoryPageProps {
    params: Promise<{ categoryId: string }>;
}

// Przy nieprawidłowym identyfikatorze karta ma tytuł strony 404, a nie tytuł działu.
export async function generateMetadata({ params }: CategoryPageProps): Promise<Metadata> {
    return parseRouteId((await params).categoryId) === null ? { title: NOT_FOUND_TITLE } : {};
}

/** Nieprawidłowy identyfikator w adresie daje stronę 404, zanim przeglądarka zacznie pobierać dane. */
export default async function CategoryPage({ params }: CategoryPageProps) {
    const categoryId = parseRouteId((await params).categoryId);

    if (categoryId === null) {
        notFound();
    }

    return <CategoryView categoryId={categoryId} />;
}