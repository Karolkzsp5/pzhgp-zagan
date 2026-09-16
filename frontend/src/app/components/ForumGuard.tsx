"use client";

import AuthGuard from '@/app/components/AuthGuard';

/**
 * Osłona sekcji forum. Warunek dostępu jest taki sam jak dla pozostałych podstron
 * dla zalogowanych hodowców, dlatego logika trzymana jest w jednym miejscu.
 */
export default function ForumGuard({ children }: { children: React.ReactNode }) {
    return <AuthGuard>{children}</AuthGuard>;
}