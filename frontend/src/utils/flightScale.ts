import { FlightTrackPointDto } from '@/app/types/flight';

/**
 * Skala prędkości użyta na mapie i w legendzie.
 *
 * Prędkość jest wielkością ciągłą, więc kodowana jest sekwencyjnie — jednym odcieniem
 * od jasnego do ciemnego. Skala tęczowa, choć popularna na mapach GPS, nie ma naturalnego
 * porządku i staje się nieczytelna przy zaburzeniach rozpoznawania barw.
 *
 * Kroki pochodzą z jednej rampy błękitu i zostały zweryfikowane pod kątem monotoniczności
 * jasności, odstępu między krokami oraz kontrastu najjaśniejszego kroku względem tła.
 */
export const SPEED_RAMP = ['#86b6ef', '#5598e7', '#2a78d6', '#1c5cab', '#104281'];

/** Kolor fragmentów zarejestrowanych w spoczynku — neutralny, celowo poza skalą prędkości. */
export const STATIONARY_COLOR = '#9a9a93';

/** Kolor linii pomocniczej łączącej miejsce wypuszczenia z gołębnikiem. */
export const REFERENCE_COLOR = '#52514e';

/** Kolor znacznika pozycji na osi czasu (druga barwa palety, poza skalą prędkości). */
export const CURSOR_COLOR = '#eb6834';

/**
 * Sprawdza, czy punkt o podanym indeksie należy do fazy lotu wyznaczonej przez serwer.
 * Gdy plik nie zawierał znaczników czasu, cała trasa traktowana jest jako lot.
 */
export const createFlightPhasePredicate = (
    points: FlightTrackPointDto[],
    releaseTime: string | null,
    arrivalTime: string | null
): ((index: number) => boolean) => {
    const releaseMs = releaseTime ? Date.parse(releaseTime) : null;
    const arrivalMs = arrivalTime ? Date.parse(arrivalTime) : null;

    return (index: number): boolean => {
        const time = points[index]?.time;
        if (releaseMs === null || arrivalMs === null || !time) return true;

        const value = Date.parse(time);
        return value >= releaseMs && value <= arrivalMs;
    };
};

/**
 * Wyznacza progi prędkości metodą kwantyli, na podstawie rozkładu w obrębie danego lotu.
 *
 * Stałe progi byłyby nieczytelne jednocześnie dla krótkiego lotu treningowego i dla lotu
 * konkursowego z silnym wiatrem w ogon — kwantyle zawsze rozkładają barwy równomiernie.
 *
 * @returns granice przedziałów (o jeden mniej niż kroków skali) albo {@code null},
 *          gdy danych o prędkości jest zbyt mało
 */
export const computeSpeedThresholds = (
    points: FlightTrackPointDto[],
    isInFlight: (index: number) => boolean
): number[] | null => {
    const speeds = points
        .filter((_, index) => isInFlight(index))
        .map(point => point.speedKmh)
        .filter((speed): speed is number => speed !== null && Number.isFinite(speed))
        .sort((a, b) => a - b);

    if (speeds.length < SPEED_RAMP.length) return null;

    return Array.from({ length: SPEED_RAMP.length - 1 }, (_, i) =>
        speeds[Math.floor(((i + 1) / SPEED_RAMP.length) * (speeds.length - 1))]
    );
};

/** Przypisuje prędkość do przedziału skali. */
export const speedBucket = (speed: number | null, thresholds: number[] | null): number => {
    if (speed === null || !thresholds) return SPEED_RAMP.length - 1;

    let bucket = 0;
    while (bucket < thresholds.length && speed > thresholds[bucket]) {
        bucket++;
    }
    return bucket;
};

/** Buduje opisy przedziałów skali prędkości do legendy, np. "70–85 km/h". */
export const speedLegendLabels = (thresholds: number[] | null): string[] => {
    if (!thresholds) return SPEED_RAMP.map(() => '—');

    return SPEED_RAMP.map((_, index) => {
        const from = index === 0 ? null : thresholds[index - 1];
        const to = index === SPEED_RAMP.length - 1 ? null : thresholds[index];

        if (from === null) return `< ${Math.round(to as number)}`;
        if (to === null) return `> ${Math.round(from)}`;
        return `${Math.round(from)}–${Math.round(to)}`;
    });
};
