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

/** Kolor linii pomocniczej łączącej początek trasy z jej końcem. */
export const REFERENCE_COLOR = '#52514e';

/** Kolor znacznika pozycji na trasie (druga barwa palety, poza skalą prędkości). */
export const CURSOR_COLOR = '#eb6834';

/**
 * Wyznacza progi prędkości metodą kwantyli, na podstawie rozkładu w obrębie danego lotu.
 *
 * Stałe progi byłyby nieczytelne jednocześnie dla krótkiego lotu treningowego i dla lotu
 * konkursowego z silnym wiatrem w ogon — kwantyle zawsze rozkładają barwy równomiernie.
 *
 * @returns granice przedziałów (o jeden mniej niż kroków skali) albo {@code null},
 *          gdy danych o prędkości jest zbyt mało
 */
export const computeSpeedThresholds = (points: FlightTrackPointDto[]): number[] | null => {
    const speeds = points
        .map(point => point.speedMetersPerMinute)
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

/** Buduje opisy przedziałów skali prędkości do legendy, np. "700–900". */
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

/** Szerokość okna mediany wygładzającej wysokość — tyle samo co po stronie serwera. */
const ELEVATION_MEDIAN_WINDOW = 5;

/**
 * Wygładza serię wysokości medianą ruchomą.
 *
 * Odbiornik w obrączce potrafi w pojedynczym odczycie podać wysokość o kilkaset metrów
 * zawyżoną. Bez wygładzenia jeden taki punkt rozciąga oś wykresu tak, że reszta przebiegu
 * spłaszcza się do linii. Serwer stosuje ten sam filtr przy liczeniu statystyk wysokości,
 * więc wykres i tabela pokazują zgodne wartości.
 */
export const smoothElevations = (values: (number | null)[]): (number | null)[] => {
    if (values.length < ELEVATION_MEDIAN_WINDOW) return values;

    const radius = Math.floor(ELEVATION_MEDIAN_WINDOW / 2);

    return values.map((value, index) => {
        if (value === null) return null;

        const window = values
            .slice(Math.max(0, index - radius), index + radius + 1)
            .filter((candidate): candidate is number => candidate !== null)
            .sort((a, b) => a - b);

        return window.length > 0 ? window[Math.floor(window.length / 2)] : value;
    });
};
