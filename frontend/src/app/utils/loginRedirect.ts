const LOGIN_PATH = '/login';

/** Adres strony logowania, która po zalogowaniu wróci do podanej ścieżki aplikacji. */
export const loginUrlReturningTo = (path: string, params: Record<string, string> = {}): string => {
    const query = new URLSearchParams(params);

    if (path !== '/' && !path.startsWith(LOGIN_PATH)) {
        query.set('next', path);
    }

    const queryString = query.toString();
    return queryString ? `${LOGIN_PATH}?${queryString}` : LOGIN_PATH;
};

/**
 * Ścieżka, na którą wrócić po zalogowaniu. Przyjmowane są tylko adresy wewnątrz aplikacji,
 * żeby link z parametrem next nie mógł przekierować na obcą stronę.
 */
export const safeReturnPath = (next: string | null): string => {
    if (!next || !next.startsWith('/') || next.startsWith('//') || next.startsWith('/\\') || next.startsWith(LOGIN_PATH)) {
        return '/';
    }

    return next;
};
