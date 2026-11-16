import type { NextConfig } from 'next';

const apiUrl = process.env.NEXT_PUBLIC_API_URL;

const nextConfig: NextConfig = {
    // Obraz Dockera uruchamia samodzielny serwer z katalogu .next/standalone, bez pełnego node_modules.
    // Lokalnie zwykły build działa bez tego trybu, który na Windowsie może wymagać uprawnień administratora.
    output: process.env.NEXT_STANDALONE_OUTPUT === 'true' ? 'standalone' : undefined,
    allowedDevOrigins: apiUrl ? [new URL(apiUrl).hostname] : [],
};

export default nextConfig;