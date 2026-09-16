"use client";

import { useEffect, useMemo } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, CircleMarker, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { FlightDetailsDto, formatTime } from '@/app/types/flight';
import {
    SPEED_RAMP,
    REFERENCE_COLOR,
    CURSOR_COLOR,
    computeSpeedThresholds,
    speedBucket
} from '@/app/utils/flightScale';

type LatLng = [number, number];

interface FlightMapProps {
    flight: FlightDetailsDto;
    /** Indeks punktu wskazywanego na trasie. */
    activeIndex: number | null;
    showStraightLine: boolean;
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

export default function FlightMap({ flight, activeIndex, showStraightLine }: FlightMapProps) {
    const points = flight.trackPoints;

    const speedThresholds = useMemo(() => computeSpeedThresholds(points), [points]);

    /**
     * Trasa dzielona jest na ciągłe odcinki o jednakowym przedziale prędkości.
     * Renderowanie jednej polilinii na przedział zamiast jednej na punkt utrzymuje
     * liczbę warstw Leaflet na poziomie kilku, a nie kilkuset.
     */
    const runsByBucket = useMemo(() => {
        const buckets: LatLng[][][] = SPEED_RAMP.map(() => []);

        let currentBucket = -1;
        let currentRun: LatLng[] = [];

        const flush = () => {
            if (currentRun.length >= 2) {
                buckets[currentBucket].push(currentRun);
            }
            currentRun = [];
        };

        for (let i = 1; i < points.length; i++) {
            const bucket = speedBucket(points[i].speedMetersPerMinute, speedThresholds);

            if (bucket !== currentBucket) {
                flush();
                currentBucket = bucket;
                currentRun = [[points[i - 1].latitude, points[i - 1].longitude]];
            }
            currentRun.push([points[i].latitude, points[i].longitude]);
        }
        flush();

        return buckets;
    }, [points, speedThresholds]);

    const bounds = useMemo<L.LatLngBoundsExpression | null>(() => {
        if (points.length === 0) return null;
        return points.map(point => [point.latitude, point.longitude] as LatLng);
    }, [points]);

    const activePoint = activeIndex !== null ? points[activeIndex] : null;

    const startIcon = useMemo(() => pinIcon('#1baf7a', '🏁'), []);
    const endIcon = useMemo(() => pinIcon('#e34948', '🏠'), []);

    if (points.length === 0) {
        return (
            <div className="h-full flex items-center justify-center text-sm text-gray-500">
                Ten lot nie zawiera punktów trasy.
            </div>
        );
    }

    return (
        <MapContainer
            center={[flight.startLatitude, flight.startLongitude]}
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

            {/* Linia odniesienia: najkrótsza droga z początku na koniec trasy. */}
            {showStraightLine && (
                <Polyline
                    positions={[
                        [flight.startLatitude, flight.startLongitude],
                        [flight.endLatitude, flight.endLongitude]
                    ]}
                    pathOptions={{ color: REFERENCE_COLOR, weight: 2, dashArray: '6 8', opacity: 0.75 }}
                />
            )}

            {/* Biała otoczka pod trasą — utrzymuje czytelność linii na kolorowych kafelkach mapy. */}
            <Polyline
                positions={runsByBucket.flat()}
                pathOptions={{ color: '#ffffff', weight: 7, opacity: 0.9, lineCap: 'round' }}
            />

            {runsByBucket.map((runs, bucket) =>
                runs.length > 0 ? (
                    <Polyline
                        key={`bucket-${bucket}`}
                        positions={runs}
                        pathOptions={{ color: SPEED_RAMP[bucket], weight: 4, opacity: 1, lineCap: 'round' }}
                    />
                ) : null
            )}

            <Marker position={[flight.startLatitude, flight.startLongitude]} icon={startIcon}>
                <Popup>
                    <strong>Początek trasy</strong>
                    <br />
                    {flight.releaseSite ?? 'Nazwa miejsca nie została podana'}
                    <br />
                    Godzina: {formatTime(flight.startTime)}
                </Popup>
            </Marker>

            <Marker position={[flight.endLatitude, flight.endLongitude]} icon={endIcon}>
                <Popup>
                    <strong>Koniec trasy</strong>
                    <br />
                    Godzina: {formatTime(flight.endTime)}
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