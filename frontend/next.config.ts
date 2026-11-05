import type { NextConfig } from "next";

const apiUrl = process.env.NEXT_PUBLIC_API_URL;

const nextConfig: NextConfig = {
    allowedDevOrigins: apiUrl ? [new URL(apiUrl).hostname] : [],
};

export default nextConfig;