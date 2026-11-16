/**
 * Identyfikator z adresu strony, np. /forum/thread/12. Przyjmowana jest tylko dodatnia liczba całkowita
 * zapisana samymi cyframi; adres typu /forum/thread/abc albo /forum/thread/0 daje null (strona 404).
 */
export const parseRouteId = (value: string): number | null => {
    if (!/^[1-9]\d*$/.test(value)) return null;

    const id = Number(value);
    return Number.isSafeInteger(id) ? id : null;
};