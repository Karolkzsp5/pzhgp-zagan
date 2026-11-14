import { useCallback, useEffect, useRef } from 'react';

/**
 * Pomija odpowiedzi, które przyszły po rozpoczęciu nowszego zapytania albo po odmontowaniu komponentu.
 * Każde ładowanie danych pobiera numer przez beginRequest() i zapisuje wynik tylko wtedy, gdy isLatestRequest() go potwierdzi.
 */
export default function useLatestRequest() {
    const latestRequestIdRef = useRef(0);

    const beginRequest = useCallback(() => {
        latestRequestIdRef.current += 1;
        return latestRequestIdRef.current;
    }, []);

    const isLatestRequest = useCallback((requestId: number) => latestRequestIdRef.current === requestId, []);

    /** Unieważnia trwające zapytania, np. po zamknięciu okna, które je rozpoczęło. */
    const invalidateRequests = useCallback(() => {
        latestRequestIdRef.current += 1;
    }, []);

    useEffect(() => invalidateRequests, [invalidateRequests]);

    return {
        beginRequest,
        isLatestRequest,
        invalidateRequests
    };
}
