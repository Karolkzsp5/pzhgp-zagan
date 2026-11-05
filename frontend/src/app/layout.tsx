import type { Metadata } from "next";
import { Geist } from "next/font/google";
import AuthProvider from '@/app/components/AuthProvider';
import { TITLE_TEMPLATE } from '@/app/utils/metadata';
import "./globals.css";

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin", "latin-ext"] });

export const metadata: Metadata = {
  title: { template: TITLE_TEMPLATE, default: "PZHGP Żagań" },
  description: "System do obsługi oddziału PZHGP Żagań"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
      <html lang="pl" className={`${geistSans.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col font-sans">
      <AuthProvider>
          {children}
      </AuthProvider>
      </body>
      </html>
  );
}