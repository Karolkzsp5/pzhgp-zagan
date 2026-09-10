"use client";

import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';

export default function WeatherPage() {
    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />

            <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                <div className="mb-8 border-b border-gray-200 pb-5">
                    <h1 className="text-3xl font-bold text-gray-900">Radar pogodowy</h1>
                    <p className="mt-2 text-sm text-gray-600">
                        Sprawdź aktualne warunki atmosferyczne przed planowanym lotem.
                    </p>
                </div>

                {/* Map container */}
                <div className="w-full h-[600px] lg:h-[700px] rounded-lg overflow-hidden shadow-md border border-gray-200 bg-white">
                    <iframe
                        className="w-full h-full border-0"
                        src="https://embed.windy.com/embed.html?type=map&location=coordinates&metricRain=mm&metricTemp=°C&metricWind=m/s&zoom=6&overlay=wind&product=ecmwf&level=surface&lat=50.986&lon=12.722&detailLat=51.619&detailLon=15.308&detail=true&message=true"
                        loading="lazy"
                        title="Radar pogodowy Windy dla Oddziału Żagań"
                    ></iframe>
                </div>
            </main>

            <Footer />
        </div>
    );
}