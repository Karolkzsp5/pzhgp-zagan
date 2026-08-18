"use client";

import Link from 'next/link';

export default function Footer() {
    const currentYear = new Date().getFullYear();

    return (
        <footer className="bg-white border-t border-gray-200 mt-auto" data-cy="footer">
            <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
                <div className="md:flex md:items-center md:justify-between">

                    <div className="flex justify-center md:justify-start space-x-6 md:order-2 text-sm text-gray-500">
                        <Link href="/contact" data-cy="footer-contact" className="hover:text-blue-600 transition">
                            Kontakt
                        </Link>
                        <Link href="/privacy" data-cy="footer-privacy" className="hover:text-blue-600 transition">
                            Polityka Prywatności
                        </Link>
                    </div>

                    <div className="mt-4 md:mt-0 md:order-1">
                        <p className="text-center text-sm text-gray-500" data-cy="footer-copyright">
                            &copy; {currentYear} Karol Kondracki. Wszelkie prawa zastrzeżone.
                        </p>
                    </div>
                </div>
            </div>
        </footer>
    );
}