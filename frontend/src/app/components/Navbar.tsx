"use client";

import { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

export default function Navbar() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [userName, setUserName] = useState('');
    const [userRole, setUserRole] = useState('');
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);
    const router = useRouter();

    useEffect(() => {
        const token = localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');

        if (token) {
            try {
                const payloadBase64 = token.split('.')[1];
                const decodedJson = atob(payloadBase64);
                const payload = JSON.parse(decodedJson);

                setIsLoggedIn(true);
                setUserName(payload.name || payload.sub?.split('@')[0] || 'Użytkowniku');
                setUserRole(payload.role);
            } catch (error) {
                console.error("Błąd dekodowania tokenu", error);
            }
        }

        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsDropdownOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    const handleLogout = () => {
        localStorage.removeItem('jwt_token');
        sessionStorage.removeItem('jwt_token');
        setIsLoggedIn(false);
        setUserName('');
        setUserRole('');
        setIsDropdownOpen(false);
        router.push('/');
    };

    return (
        <nav className="bg-blue-700 text-white shadow-md relative z-40">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between h-16 items-center">
                    <Link href="/" className="flex-shrink-0 font-bold text-xl tracking-wider hover:text-gray-200 transition">
                        PZHGP Żagań
                    </Link>

                    <div className="flex items-center space-x-4">
                        {!isLoggedIn ? (
                            <>
                                <Link href="/login" className="hover:bg-blue-600 px-3 py-2 rounded-md text-sm font-medium transition">
                                    Zaloguj się
                                </Link>
                                <Link href="/register" className="bg-white text-blue-700 hover:bg-gray-100 px-3 py-2 rounded-md text-sm font-bold transition shadow-sm">
                                    Rejestracja
                                </Link>
                            </>
                        ) : (
                            <div className="relative" ref={dropdownRef}>
                                <button
                                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                                    className="flex items-center space-x-2 bg-blue-800 hover:bg-blue-900 border border-blue-600 px-4 py-2 rounded-md text-sm font-medium transition focus:outline-none"
                                >
                                    <span>Witaj, <strong className="font-semibold">{userName}</strong></span>
                                    <svg
                                        className={`w-4 h-4 transition-transform duration-200 ${isDropdownOpen ? 'transform rotate-180' : ''}`}
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </button>

                                {isDropdownOpen && (
                                    <div className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-lg py-1 border border-gray-100 z-50 animate-fadeIn">

                                        <Link
                                            href="/settings"
                                            onClick={() => setIsDropdownOpen(false)}
                                            className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-blue-600 transition"
                                        >
                                            <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                            </svg>
                                            Ustawienia konta
                                        </Link>

                                        {userRole === 'ADMINISTRATOR' && (
                                            <Link
                                                href="/admin"
                                                onClick={() => setIsDropdownOpen(false)}
                                                className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-blue-600 transition"
                                            >
                                                <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                                                </svg>
                                                Panel administratora
                                            </Link>
                                        )}

                                        <div className="border-t border-gray-100 my-1"></div>

                                        <button
                                            onClick={handleLogout}
                                            className="w-full flex items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition text-left"
                                        >
                                            <svg className="w-4 h-4 mr-2 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                            </svg>
                                            Wyloguj się
                                        </button>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </nav>
    );
}