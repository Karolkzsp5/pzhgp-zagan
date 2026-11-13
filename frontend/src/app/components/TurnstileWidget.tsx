'use client';

import { Ref, useEffect, useImperativeHandle, useRef, useState } from 'react';
import Script from 'next/script';

const SITE_KEY = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY ?? '';

// Parametr render=explicit wyłącza automatyczne wyszukiwanie widżetów na stronie: komponent sam wywołuje turnstile.render().
const SCRIPT_URL = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';

// Standardowy widżet ma 300 px szerokości. W węższym formularzu (mały telefon) używany jest wariant kompaktowy.
const NORMAL_WIDGET_WIDTH = 300;

export type TurnstileLanguage = 'pl' | 'en' | 'de';

/** Opcje przekazywane do turnstile.render(). Nazwy z myślnikami pochodzą z API Cloudflare. */
interface TurnstileRenderOptions {
    sitekey: string;
    language: TurnstileLanguage;
    theme: 'light';
    size: 'normal' | 'compact';
    callback: (token: string) => void;
    'expired-callback': () => void;
    'error-callback': () => boolean;
}

/** Fragment API skryptu Cloudflare Turnstile, z którego korzysta komponent. */
interface TurnstileApi {
    render: (container: HTMLElement, options: TurnstileRenderOptions) => string;
    reset: (widgetId: string) => void;
    remove: (widgetId: string) => void;
}

declare global {
    interface Window {
        turnstile?: TurnstileApi;
    }
}

export interface TurnstileWidgetHandle {
    /** Unieważnia bieżący token i rozpoczyna weryfikację od nowa. */
    reset: () => void;
}

interface TurnstileWidgetProps {
    /** Token potwierdzający weryfikację albo null, gdy tokenu jeszcze nie ma, wygasł lub widżet został zresetowany. */
    onTokenChange: (token: string | null) => void;
    language?: TurnstileLanguage;
    errorMessage?: string;
    ref?: Ref<TurnstileWidgetHandle>;
}

/**
 * Widżet CAPTCHA Cloudflare Turnstile.
 *
 * Po przejściu weryfikacji przekazuje token, który formularz wysyła do backendu, a backend sprawdza w API Cloudflare.
 * Token jest jednorazowy i ważny 5 minut, dlatego po każdej nieudanej wysyłce formularz wywołuje reset().
 */
export default function TurnstileWidget({
    onTokenChange,
    language = 'pl',
    errorMessage = 'Nie udało się wczytać weryfikacji CAPTCHA. Odśwież stronę i spróbuj ponownie.',
    ref
}: TurnstileWidgetProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const widgetIdRef = useRef<string | null>(null);
    const onTokenChangeRef = useRef(onTokenChange);
    const [isScriptReady, setIsScriptReady] = useState(false);
    const [hasError, setHasError] = useState(false);

    useEffect(() => {
        onTokenChangeRef.current = onTokenChange;
    }, [onTokenChange]);

    useEffect(() => {
        const container = containerRef.current;
        if (!isScriptReady || !container || !window.turnstile) return;

        const widgetId = window.turnstile.render(container, {
            sitekey: SITE_KEY,
            language,
            theme: 'light',
            size: container.clientWidth < NORMAL_WIDGET_WIDTH ? 'compact' : 'normal',
            callback: (token) => {
                setHasError(false);
                onTokenChangeRef.current(token);
            },
            // Po wygaśnięciu tokenu widżet sam rozpoczyna nową weryfikację i ponownie wywołuje callback.
            'expired-callback': () => onTokenChangeRef.current(null),
            'error-callback': () => {
                setHasError(true);
                onTokenChangeRef.current(null);
                // Zwrócenie true oznacza, że błąd został obsłużony (komunikat pod widżetem).
                return true;
            }
        });
        widgetIdRef.current = widgetId;

        return () => {
            widgetIdRef.current = null;
            window.turnstile?.remove(widgetId);
            onTokenChangeRef.current(null);
        };
    }, [isScriptReady, language]);

    useImperativeHandle(ref, () => ({
        reset: () => {
            onTokenChangeRef.current(null);

            if (widgetIdRef.current !== null) {
                window.turnstile?.reset(widgetIdRef.current);
            }
        }
    }), []);

    if (!SITE_KEY) {
        return (
            <p role="alert" className="text-sm text-red-600">
                Brak klucza Cloudflare Turnstile. Ustaw zmienną NEXT_PUBLIC_TURNSTILE_SITE_KEY.
            </p>
        );
    }

    return (
        <div>
            {/* onReady działa po wczytaniu skryptu i przy każdym kolejnym zamontowaniu komponentu. */}
            <Script src={SCRIPT_URL} onReady={() => setIsScriptReady(true)} onError={() => setHasError(true)} />

            {/* Stała minimalna wysokość zapobiega przesunięciu formularza po wczytaniu widżetu. */}
            <div ref={containerRef} className="min-h-[65px]" />

            {hasError && (
                <p role="alert" className="mt-2 text-sm text-red-600">{errorMessage}</p>
            )}
        </div>
    );
}