"use client";

import { useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, CircleMarker, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { FlightDetailsDto, formatTime } from '@/app/types/flight';
import {
    SPEED_RAMP,
    STATIONARY_COLOR,
    REFERENCE_COLOR,
    CURSOR_COLOR,
    createFlightPhasePredicate,
    computeSpeedThresholds,
    speedBucket
} from '@/utils/flightScale';

type LatLng = [number, number];

interface FlightMapProps {
    flight: FlightDetailsDto;
    /** Indeks punktu wskazywanego przez odtwarzacz trasy. */
    activeIndex: number | null;
    showStraightLine: boolean;
    showStationary: boolean;
}

/** Dopasowuje widok mapy do zasięgu trasy przy pierwszym renderze i zmianie lotu. */
function FitToTrack({ bounds }: { bounds: L.LatLngBoundsExpression | null }) {
    const map = useMap();

    useEffect(() => {
        if (bounds) {
            map.fitBounds(bounds, { padding: [40, 40] });
        }
    }, [map, bounds]);

    return null;
}

/**
 * Znacznik rysowany jako element HTML zamiast domyślnej ikony Leaflet.
 * Domyślne ikony wskazują na pliki PNG rozwiązywane względem adresu strony,
 * co przy bundlowaniu w Next.js kończy się brakującą grafiką.
 */
const pinIcon = (background: string, glyph: string) =>
    L.divIcon({
        className: '',
        html: `<div style="
                background:${background};
                width:30px;height:30px;border-radius:50% 50% 50% 0;
                transform:rotate(-45deg);
                border:2px solid #ffffff;
                box-shadow:0 1px 4px rgba(0,0,0,.4);
                display:flex;align-items:center;justify-content:center;">
                 <span style="transform:rotate(45deg);font-size:14px;line-height:1;">${glyph}</span>
               </div>`,
        iconSize: [30, 30],
        iconAnchor: [15, 30],
        popupAnchor: [0, -30]
    });

export default function FlightMap({ flight, activeIndex, showStraightLine, showStationary }: FlightMapProps) {
    const points = flight.trackPoints;

    /** Podział trasy na fazę lotu i fazy postoju wyznaczone przez serwer. */
    const isInFlight = useMemo(
        () => createFlightPhasePredicate(points, flight.releaseTime, flight.arrivalTime),
        [points, flight.releaseTime, flight.arrivalTime]
    );

    const speedThresholds = useMemo(
        () => computeSpeedThresholds(points, isInFlight),
        [points, isInFlight]
    );

    const bucketOf = useMemo(
        () => (speed: number | null) => speedBucket(speed, speedThresholds),
        [speedThresholds]
    );

    /**
     * Trasa dzielona jest na ciągłe odcinki o jednakowym przedziale prędkości.
     * Renderowanie jednej polilinii na przedział zamiast jednej na punkt utrzymuje
     * liczbę warstw Leaflet na poziomie kilku, a nie kilkuset.
     */
    const { flightRuns, stationaryRuns } = useMemo(() => {
        const flightBuckets: LatLng[][][] = SPEED_RAMP.map(() => []);
        const stationary: LatLng[][] = [];

        let currentBucket = -1;
        let currentRun: LatLng[] = [];
        let currentIsFlight = false;

        const flush = () => {
            if (currentRun.length < 2) {
                currentRun = [];
                return;
            }
            if (currentIsFlight) {
                flightBuckets[currentBucket].push(currentRun);
            } else {
                stationary.push(currentRun);
            }
            currentRun = [];
        };

        for (let i = 1; i < points.length; i++) {
            const segmentInFlight = isInFlight(i - 1) && isInFlight(i);
            const bucket = segmentInFlight ? bucketOf(points[i].speedKmh) : -1;

            if (segmentInFlight !== currentIsFlight || bucket !== currentBucket) {
                flush();
                currentIsFlight = segmentInFlight;
                currentBucket = bucket;
                currentRun = [[points[i - 1].latitude, points[i - 1].longitude]];
            }
            currentRun.push([points[i].latitude, points[i].longitude]);
        }
        flush();

        return { flightRuns: flightBuckets, stationaryRuns: stationary };
    }, [points, isInFlight, bucketOf]);

    const bounds = useMemo<L.LatLngBoundsExpression | null>(() => {
        if (points.length === 0) return null;
        return points.map(point => [point.latitude, point.longitude] as LatLng);
    }, [points]);

    const activePoint = activeIndex !== null ? points[activeIndex] : null;

    const releaseIcon = useMemo(() => pinIcon('#1baf7a', '🏁'), []);
    const loftIcon = useMemo(() => pinIcon('#e34948', '🏠'), []);

    if (points.length === 0) {
        return (
            <div className="h-full flex items-center justify-center text-sm text-gray-500">
                Ten lot nie zawiera punktów trasy.
            </div>
        );
    }

    return (
        <MapContainer
            center={[flight.releaseLatitude, flight.releaseLongitude]}
            zoom={9}
            scrollWheelZoom
            className="h-full w-full"
        >
            <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                maxZoom={19}
            />

            <FitToTrack bounds={bounds} />

            {/* Linia odniesienia: najkrótsza droga z miejsca wypuszczenia do gołębnika. */}
            {showStraightLine && (
                <Polyline
                    positions={[
                        [flight.releaseLatitude, flight.releaseLongitude],
                        [flight.arrivalLatitude, flight.arrivalLongitude]
                    ]}
                    pathOptions={{ color: REFERENCE_COLOR, weight: 2, dashArray: '6 8', opacity: 0.75 }}
                />
            )}

            {/* Fragmenty zarejestrowane przed wypuszczeniem i po przylocie. */}
            {showStationary && stationaryRuns.length > 0 && (
                <Polyline
                    positions={stationaryRuns}
                    pathOptions={{ color: STATIONARY_COLOR, weight: 3, opacity: 0.8 }}
                />
            )}

            {/* Biała otoczka pod trasą — utrzymuje czytelność linii na kolorowych kafelkach mapy. */}
            <Polyline
                positions={flightRuns.flat()}
                pathOptions={{ color: '#ffffff', weight: 7, opacity: 0.9, lineCap: 'round' }}
            />

            {flightRuns.map((runs, bucket) =>
                runs.length > 0 ? (
                    <Polyline
                        key={`bucket-${bucket}`}
                        positions={runs}
                        pathOptions={{ color: SPEED_RAMP[bucket], weight: 4, opacity: 1, lineCap: 'round' }}
                    />
                ) : null
            )}

            <Marker position={[flight.releaseLatitude, flight.releaseLongitude]} icon={releaseIcon}>
                <Popup>
                    <strong>Miejsce wypuszczenia</strong>
                    <br />
                    {flight.releaseSite ?? 'Nazwa nie została podana'}
                    <br />
                    Godzina: {formatTime(flight.releaseTime)}
                </Popup>
            </Marker>

            <Marker position={[flight.arrivalLatitude, flight.arrivalLongitude]} icon={loftIcon}>
                <Popup>
                    <strong>Gołębnik</strong>
                    <br />
                    Przylot: {formatTime(flight.arrivalTime)}
                </Popup>
            </Marker>

            {activePoint && (
                <CircleMarker
                    center={[activePoint.latitude, activePoint.longitude]}
                    radius={8}
                    pathOptions={{ color: '#ffffff', weight: 3, fillColor: CURSOR_COLOR, fillOpacity: 1 }}
                />
            )}
        </MapContainer>
    );
}
