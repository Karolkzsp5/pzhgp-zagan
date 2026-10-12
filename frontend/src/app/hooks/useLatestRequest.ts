"use client";

import { useCallback, useEffect, useRef } from 'react';

export default function useLatestRequest() {
    const latestRequestIdRef = useRef(0);

    const beginRequest = useCallback(() => {
        latestRequestIdRef.current += 1;

        return latestRequestIdRef.current;
    }, []);

    const isLatestRequest = useCallback((requestId: number) => latestRequestIdRef.current === requestId, []);

    useEffect(() => {
        return () => {
            latestRequestIdRef.current += 1;
        };
    }, []);

    return {
        beginRequest,
        isLatestRequest
    };
}