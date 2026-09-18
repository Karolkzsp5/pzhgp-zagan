"use client";

import { useMemo, useRef, useState } from 'react';
import { FlightTrackPointDto, formatSpeed, formatTime } from '@/app/types/flight';
import { CURSOR_COLOR, smoothElevations } from '@/app/utils/flightScale';

const CHART_WIDTH = 800;
const PANEL_HEIGHT = 96;
const PANEL_GAP = 46;
const MARGIN_LEFT = 52;
const MARGIN_RIGHT = 14;
const MARGIN_TOP = 16;
const PLOT_WIDTH = CHART_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
const CHART_HEIGHT = MARGIN_TOP + PANEL_HEIGHT * 2 + PANEL_GAP + 28;

const LINE_COLOR = '#2a78d6';
const AREA_COLOR = '#cde2fb';
const GRID_COLOR = '#e5e7eb';
const AXIS_TEXT_COLOR = '#6b7280';

interface FlightProfileChartProps {
    /** Punkty należące do fazy lotu, w kolejności chronologicznej. */
    points: FlightTrackPointDto[];
    /** Indeksy punktów w oryginalnej tablicy trasy — używane przy podświetlaniu pozycji na mapie. */
    originalIndexes: number[];
    activeIndex: number | null;
    /** Podgląd punktu przy przesuwaniu kursora nad wykresem. */
    onHover: (originalIndex: number | null) => void;
    /** Wybór punktu kliknięciem — ustawia pozycję również na pasku odtwarzania. */
    onSelect: (originalIndex: number | null) => void;
}

interface Panel {
    title: string;
    unit: string;
    values: (number | null)[];
    offsetY: number;
}

/**
 * Profile wysokości i prędkości wzdłuż trasy lotu.
 *
 * Obie wielkości mają różne jednostki i rzędy wielkości, dlatego prezentowane są
 * na dwóch osobnych panelach o wspólnej osi czasu, a nie na jednym wykresie z dwiema
 * osiami — wykres dwuosiowy pozwalałby dowolnie przesuwać jedną serię względem drugiej
 * i sugerować zależności, których w danych nie ma.
 */
export default function FlightProfileChart({
    points,
    originalIndexes,
    activeIndex,
    onHover,
    onSelect
}: FlightProfileChartProps) {
    const svgRef = useRef<SVGSVGElement>(null);
    const [hoverIndex, setHoverIndex] = useState<number | null>(null);

    // Wysokość wygładzana medianą — pojedynczy błędny odczyt potrafiłby rozciągnąć oś
    // wykresu tak, że cały przebieg spłaszczyłby się do linii.
    const elevations = useMemo(
        () => smoothElevations(points.map(point => point.elevation)),
        [points]
    );
    const speeds = useMemo(() => points.map(point => point.speedMetersPerMinute), [points]);

    /**
     * Panel powstaje tylko dla wielkości faktycznie obecnych w pliku — trasy bez zapisu
     * wysokości pokazują sam profil prędkości.
     */
    const panels = useMemo<Panel[]>(() => {
        const available: Omit<Panel, 'offsetY'>[] = [];

        if (speeds.some(value => value !== null)) {
            available.push({ title: 'Prędkość', unit: 'm/min', values: speeds });
        }
        if (elevations.some(value => value !== null)) {
            available.push({ title: 'Wysokość n.p.m.', unit: 'm', values: elevations });
        }

        return available.map((panel, index) => ({
            ...panel,
            offsetY: MARGIN_TOP + index * (PANEL_HEIGHT + PANEL_GAP)
        }));
    }, [elevations, speeds]);

    /** Pozycja punktu na osi czasu; dla plików bez znaczników czasu — pozycja według indeksu. */
    const xPositions = useMemo(() => {
        const times = points.map(point => (point.time ? Date.parse(point.time) : null));
        const valid = times.filter((value): value is number => value !== null);

        if (valid.length < 2) {
            return points.map((_, index) => (index / Math.max(1, points.length - 1)) * PLOT_WIDTH);
        }

        const min = valid[0];
        const max = valid[valid.length - 1];
        const span = Math.max(1, max - min);

        // Pętla zamiast map(): pozycja punktu bez znacznika czasu przenoszona jest
        // z poprzedniego punktu, a lokalna tablica nie wymaga zmiennej domknięcia.
        const positions: number[] = [];
        for (let index = 0; index < times.length; index++) {
            const value = times[index];

            if (value === null) {
                positions.push(index === 0 ? 0 : positions[index - 1]);
            } else {
                positions.push(((value - min) / span) * PLOT_WIDTH);
            }
        }
        return positions;
    }, [points]);

    /**
     * Zakres osi jest nieco szerszy od danych, żeby linia nie dotykała krawędzi panelu,
     * ale opisy osi pokazują rzeczywiste wartości skrajne — te same, które trafiają
     * do tabeli statystyk.
     */
    const scaleFor = (values: (number | null)[]) => {
        const finite = values.filter((value): value is number => value !== null && Number.isFinite(value));
        if (finite.length === 0) return { min: 0, max: 1, dataMin: 0, dataMax: 1 };

        const dataMin = Math.min(...finite);
        const dataMax = Math.max(...finite);
        const padding = (dataMax - dataMin) * 0.12 || 1;

        return { min: dataMin - padding, max: dataMax + padding, dataMin, dataMax };
    };

    const buildPath = (panel: Panel) => {
        const { min, max, dataMin, dataMax } = scaleFor(panel.values);
        const span = Math.max(1e-9, max - min);

        let line = '';
        let started = false;

        panel.values.forEach((value, index) => {
            if (value === null || !Number.isFinite(value)) return;

            const x = MARGIN_LEFT + xPositions[index];
            const y = panel.offsetY + PANEL_HEIGHT - ((value - min) / span) * PANEL_HEIGHT;

            line += `${started ? 'L' : 'M'}${x.toFixed(2)} ${y.toFixed(2)}`;
            started = true;
        });

        const area = started
            ? `${line}L${(MARGIN_LEFT + PLOT_WIDTH).toFixed(2)} ${panel.offsetY + PANEL_HEIGHT}` +
              `L${MARGIN_LEFT} ${panel.offsetY + PANEL_HEIGHT}Z`
            : '';

        return { line, area, min, max, dataMin, dataMax };
    };

    const nearestIndexAt = (clientX: number): number | null => {
        const svg = svgRef.current;
        if (!svg || points.length === 0) return null;

        const rect = svg.getBoundingClientRect();
        const ratio = (clientX - rect.left) / rect.width;
        const target = ratio * CHART_WIDTH - MARGIN_LEFT;

        let best = 0;
        let bestDistance = Number.POSITIVE_INFINITY;
        xPositions.forEach((x, index) => {
            const distance = Math.abs(x - target);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = index;
            }
        });
        return best;
    };

    const handlePointerMove = (event: React.PointerEvent<SVGSVGElement>) => {
        if (event.pointerType === 'touch') return;

        const index = nearestIndexAt(event.clientX);
        setHoverIndex(index);
        onHover(index !== null ? originalIndexes[index] : null);
    };

    const handlePointerLeave = (event: React.PointerEvent<SVGSVGElement>) => {
        if (event.pointerType === 'touch') return;

        setHoverIndex(null);
        onHover(null);
    };

    /** Indeks lokalny odpowiadający punktowi podświetlonemu na mapie. */
    const highlightedIndex = useMemo(() => {
        if (hoverIndex !== null) return hoverIndex;
        if (activeIndex === null) return null;

        const found = originalIndexes.indexOf(activeIndex);
        return found >= 0 ? found : null;
    }, [hoverIndex, activeIndex, originalIndexes]);

    /** Kliknięcie utrwala wskazany punkt, więc zostaje on widoczny po zjechaniu kursorem z wykresu. */
    const handleClick = (event: React.MouseEvent<SVGSVGElement>) => {
        const index = nearestIndexAt(event.clientX);
        if (index !== null) {
            onSelect(originalIndexes[index]);
        }
    };

    /** Obsługa klawiatury: strzałkami można przesuwać wskazany punkt wzdłuż trasy. */
    const handleKeyDown = (event: React.KeyboardEvent<SVGSVGElement>) => {
        const step = event.key === 'ArrowLeft' ? -1 : event.key === 'ArrowRight' ? 1 : 0;
        if (step === 0) return;

        event.preventDefault();
        const current = highlightedIndex ?? 0;
        const next = Math.max(0, Math.min(points.length - 1, current + step));
        onSelect(originalIndexes[next]);
    };

    if (panels.length === 0 || points.length < 2) {
        return (
            <p className="text-sm text-gray-500">
                Ten lot nie zawiera danych o wysokości ani prędkości.
            </p>
        );
    }

    const timeTicks = [0, 0.25, 0.5, 0.75, 1].map(fraction => {
        const index = Math.min(points.length - 1, Math.round(fraction * (points.length - 1)));
        return { x: MARGIN_LEFT + xPositions[index], label: formatTime(points[index].time) };
    });

    const highlighted = highlightedIndex !== null ? points[highlightedIndex] : null;

    return (
        <div className="relative">
            {/* Na wąskich ekranach wykres zachowuje minimalną szerokość i przewija się poziomo,
                zamiast kurczyć się do rozmiaru, w którym opisy osi stają się nieczytelne. */}
            <div className="overflow-x-auto -mx-1 px-1">
            <svg
                ref={svgRef}
                viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
                className="w-full h-auto min-w-[560px] cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 rounded"
                role="img"
                tabIndex={0}
                aria-label="Profile wysokości i prędkości wzdłuż trasy lotu. Kliknij, aby wybrać punkt; strzałkami przesuniesz wybór."
                onPointerMove={handlePointerMove}
                onPointerLeave={handlePointerLeave}
                onClick={handleClick}
                onKeyDown={handleKeyDown}
            >
                {panels.map(panel => {
                    const { line, area, min, max, dataMin, dataMax } = buildPath(panel);
                    const span = Math.max(1e-9, max - min);
                    const ticks = [dataMax, (dataMax + dataMin) / 2, dataMin].map(value => ({
                        value,
                        y: panel.offsetY + PANEL_HEIGHT - ((value - min) / span) * PANEL_HEIGHT
                    }));

                    return (
                        <g key={panel.title}>
                            <text
                                x={MARGIN_LEFT}
                                y={panel.offsetY - 6}
                                fontSize="12"
                                fontWeight="600"
                                fill="#374151"
                            >
                                {panel.title} <tspan fill={AXIS_TEXT_COLOR} fontWeight="400">[{panel.unit}]</tspan>
                            </text>

                            {ticks.map((tick, tickIndex) => (
                                <g key={`${panel.title}-tick-${tickIndex}`}>
                                    <line
                                        x1={MARGIN_LEFT}
                                        x2={MARGIN_LEFT + PLOT_WIDTH}
                                        y1={tick.y}
                                        y2={tick.y}
                                        stroke={GRID_COLOR}
                                        strokeWidth="1"
                                        vectorEffect="non-scaling-stroke"
                                    />
                                    <text
                                        x={MARGIN_LEFT - 8}
                                        y={tick.y + 4}
                                        fontSize="11"
                                        textAnchor="end"
                                        fill={AXIS_TEXT_COLOR}
                                    >
                                        {Math.round(tick.value)}
                                    </text>
                                </g>
                            ))}

                            {area && <path d={area} fill={AREA_COLOR} opacity="0.55" />}
                            <path
                                d={line}
                                fill="none"
                                stroke={LINE_COLOR}
                                strokeWidth="2"
                                strokeLinejoin="round"
                                strokeLinecap="round"
                                vectorEffect="non-scaling-stroke"
                            />

                            {highlightedIndex !== null && (() => {
                                const value = panel.values[highlightedIndex];
                                if (value === null || !Number.isFinite(value)) return null;

                                const y = panel.offsetY + PANEL_HEIGHT - ((value - min) / span) * PANEL_HEIGHT;

                                return (
                                    <circle
                                        cx={MARGIN_LEFT + xPositions[highlightedIndex]}
                                        cy={y}
                                        r="4.5"
                                        fill={CURSOR_COLOR}
                                        stroke="#ffffff"
                                        strokeWidth="2"
                                        vectorEffect="non-scaling-stroke"
                                    />
                                );
                            })()}
                        </g>
                    );
                })}

                {timeTicks.map((tick, index) => (
                    <text
                        key={`time-${index}`}
                        x={Math.min(CHART_WIDTH - MARGIN_RIGHT, Math.max(MARGIN_LEFT, tick.x))}
                        y={CHART_HEIGHT - 8}
                        fontSize="11"
                        textAnchor={index === 0 ? 'start' : index === timeTicks.length - 1 ? 'end' : 'middle'}
                        fill={AXIS_TEXT_COLOR}
                    >
                        {tick.label}
                    </text>
                ))}

                {highlightedIndex !== null && (
                    <line
                        x1={MARGIN_LEFT + xPositions[highlightedIndex]}
                        x2={MARGIN_LEFT + xPositions[highlightedIndex]}
                        y1={MARGIN_TOP}
                        y2={MARGIN_TOP + PANEL_HEIGHT * 2 + PANEL_GAP}
                        stroke={CURSOR_COLOR}
                        strokeWidth="1"
                        strokeDasharray="3 3"
                        vectorEffect="non-scaling-stroke"
                    />
                )}
            </svg>
            </div>

            {highlighted && (
                <div className="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-600 border-t border-gray-100 pt-2">
                    <span>
                        Godzina: <strong className="text-gray-900">{formatTime(highlighted.time)}</strong>
                    </span>
                    {highlighted.speedMetersPerMinute !== null && (
                        <span>
                            Prędkość:{' '}
                            <strong className="text-gray-900">{formatSpeed(highlighted.speedMetersPerMinute)}</strong>
                        </span>
                    )}
                    {highlighted.elevation !== null && (
                        <span>
                            Wysokość: <strong className="text-gray-900">{Math.round(highlighted.elevation)} m n.p.m.</strong>
                        </span>
                    )}
                </div>
            )}
        </div>
    );
}