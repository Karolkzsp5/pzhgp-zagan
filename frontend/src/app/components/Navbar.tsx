"use client";

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { formatGlobalDate } from '@/app/utils/formatters';
import { decodeJwt, getAuthToken, isJwtValid, logout } from '@/app/utils/jwt';
import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';

interface NotificationDto {
    id: number;
    message: string;
    link: string | null;
    isRead: boolean;
    type: string;
    createdAt: string;
}

export default function Navbar() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [userName, setUserName] = useState('');
    const [userRole, setUserRole] = useState('');
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
    const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
    const [notifications, setNotifications] = useState<NotificationDto[]>([]);
    const [unreadCount, setUnreadCount] = useState(0);

    const dropdownRef = useRef<HTMLDivElement>(null);
    const mobileMenuRef = useRef<HTMLDivElement>(null);
    const mobileMenuButtonRef = useRef<HTMLButtonElement>(null);
    const notificationsRef = useRef<HTMLDivElement>(null);

    const router = useRouter();
    const pathname = usePathname();

    const fetchNotifications = useCallback(async () => {
        try {
            const [notifRes, countRes] = await Promise.all([
                fetchWithAuth(`${API_URL}/api/notifications`),
                fetchWithAuth(`${API_URL}/api/notifications/unread-count`)
            ]);

            if (!notifRes.ok) throw new Error(await readApiError(notifRes, 'Nie udało się pobrać powiadomień.'));
            if (!countRes.ok) throw new Error(await readApiError(countRes, 'Nie udało się pobrać liczby nieprzeczytanych powiadomień.'));

            setNotifications(await notifRes.json());
            setUnreadCount(await countRes.json());
        } catch (error) {
            console.error('Błąd pobierania powiadomień:', error);
        }
    }, []);

    useEffect(() => {
        const token = getAuthToken();

        if (isJwtValid(token)) {
            const payload = decodeJwt(token!);
            if (payload) {
                setIsLoggedIn(true);
                setUserName(payload.name || payload.sub?.split('@')[0] || 'Użytkowniku');
                setUserRole(payload.role);
                void fetchNotifications();
            }
        } else if (token) {
            logout();
        }

        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) setIsDropdownOpen(false);
            if (notificationsRef.current && !notificationsRef.current.contains(event.target as Node)) setIsNotificationsOpen(false);
            if (mobileMenuRef.current && !mobileMenuRef.current.contains(event.target as Node) && mobileMenuButtonRef.current && !mobileMenuButtonRef.current.contains(event.target as Node)) setIsMobileMenuOpen(false);
        };

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                setIsDropdownOpen(false);
                setIsNotificationsOpen(false);
                setIsMobileMenuOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [fetchNotifications]);

    const handleNotificationClick = async (notif: NotificationDto) => {
        if (!notif.isRead) {
            try {
                const response = await fetchWithAuth(`${API_URL}/api/notifications/${notif.id}/read`, { method: 'PUT' });
                if (!response.ok) throw new Error(await readApiError(response, 'Nie udało się oznaczyć powiadomienia jako przeczytane.'));

                setNotifications(prev => prev.map(n => n.id === notif.id ? { ...n, isRead: true } : n));
                setUnreadCount(prev => Math.max(0, prev - 1));
            } catch (error) {
                console.error('Błąd oznaczania jako przeczytane:', error);
            }
        }

        setIsNotificationsOpen(false);
        if (notif.link) router.push(notif.link);
    };

    const handleMarkAllAsRead = async () => {
        try {
            const response = await fetchWithAuth(`${API_URL}/api/notifications/read-all`, { method: 'PUT' });
            if (!response.ok) throw new Error(await readApiError(response, 'Nie udało się oznaczyć wszystkich powiadomień jako przeczytane.'));

            setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
            setUnreadCount(0);
        } catch (error) {
            console.error('Błąd oznaczania wszystkich jako przeczytane:', error);
        }
    };

    const handleLogout = () => {
        logout();
        setIsLoggedIn(false);
        setUserName('');
        setUserRole('');
        setNotifications([]);
        setUnreadCount(0);
        setIsDropdownOpen(false);
        setIsNotificationsOpen(false);
        setIsMobileMenuOpen(false);
        router.replace('/');
    };

    const isActive = (path: string) => {
        if (path === '/') return pathname === '/';
        return pathname === path || pathname.startsWith(`${path}/`);
    };

    const navLinks = [
        { name: 'Wyniki lotów', href: '/results', show: true },
        { name: 'Mapy lotów', href: '/flights', show: isLoggedIn },
        { name: 'Forum', href: '/forum', show: isLoggedIn },
        { name: 'Znalezione gołębie', href: '/found-pigeons', show: true },
        { name: 'Pogoda', href: '/weather', show: true },
        { name: 'Zarząd', href: '/board', show: true },
    ];

    return (
        <nav className="bg-blue-700 text-white shadow-md relative z-40" aria-label="Główna nawigacja">
            <div className="max-w-7xl mx-auto px-2 min-[375px]:px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between h-16 items-center">

                    <div className="flex items-center shrink-0">
                        {/* Mobile menu button */}
                        <button
                            type="button"
                            ref={mobileMenuButtonRef}
                            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                            className="md:hidden mr-1 min-[375px]:mr-2 p-1 text-white hover:text-gray-200 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white rounded"
                            aria-label="Menu główne"
                            aria-expanded={isMobileMenuOpen}
                            aria-controls="mobile-navigation"
                        >
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                {isMobileMenuOpen ? (
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                ) : (
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
                                )}
                            </svg>
                        </button>

                        <Link href="/" className="font-bold text-base sm:text-xl tracking-wide sm:tracking-wider hover:text-gray-200 transition whitespace-nowrap">
                            PZHGP Żagań
                        </Link>
                    </div>

                    <div className="hidden md:flex flex-1 justify-center gap-4 lg:gap-8">
                        {navLinks.filter(link => link.show).map((link) => (
                            <Link
                                key={link.name}
                                href={link.href}
                                className={`text-sm font-medium transition-all duration-200 py-1 whitespace-nowrap ${
                                    isActive(link.href)
                                        ? 'border-b-2 border-white text-white'
                                        : 'text-blue-100 hover:text-white border-b-2 border-transparent hover:border-blue-300'
                                }`}
                            >
                                {link.name}
                            </Link>
                        ))}
                    </div>

                    <div className="flex items-center justify-end shrink-0 space-x-1 min-[375px]:space-x-2 sm:space-x-4">
                        {!isLoggedIn ? (
                            <>
                                <Link href="/login" className="whitespace-nowrap hover:bg-blue-600 px-1.5 min-[375px]:px-2 sm:px-3 py-1.5 sm:py-2 rounded-md text-xs min-[375px]:text-sm font-medium transition">
                                    Zaloguj się
                                </Link>
                                <Link href="/register" className="whitespace-nowrap bg-white text-blue-700 hover:bg-gray-100 px-1.5 min-[375px]:px-2 sm:px-3 py-1.5 sm:py-2 rounded-md text-xs min-[375px]:text-sm font-bold transition shadow-sm">
                                    Rejestracja
                                </Link>
                            </>
                        ) : (
                            <div className="flex items-center space-x-1 sm:space-x-3">

                                {/* Notification icon */}
                                <div className="relative" ref={notificationsRef}>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setIsNotificationsOpen(!isNotificationsOpen);
                                            setIsDropdownOpen(false);
                                        }}
                                        className="p-2 rounded-full hover:bg-blue-800 transition relative focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
                                        aria-label="Powiadomienia"
                                        aria-expanded={isNotificationsOpen}
                                        aria-controls="notifications-panel"
                                        aria-haspopup="true"
                                    >
                                        <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                                        </svg>

                                        {unreadCount > 0 && (
                                            <span className="absolute top-1 right-1 inline-flex items-center justify-center px-1.5 py-0.5 text-xs font-bold leading-none text-white transform translate-x-1/4 -translate-y-1/4 bg-red-600 rounded-full">
                                                {unreadCount > 99 ? '99+' : unreadCount}
                                            </span>
                                        )}
                                    </button>

                                    {/* Notification modal */}
                                    {isNotificationsOpen && (
                                        <div id="notifications-panel" className="absolute -right-14 sm:right-0 mt-2 w-[300px] sm:w-96 bg-white rounded-md shadow-2xl py-2 border border-gray-100 z-50 animate-fadeIn text-gray-800">
                                            <div className="px-4 py-2 border-b border-gray-100 flex justify-between items-center">
                                                <h3 className="font-bold text-sm text-gray-900">Powiadomienia</h3>
                                                {unreadCount > 0 && (
                                                    <button
                                                        type="button"
                                                        onClick={handleMarkAllAsRead}
                                                        className="text-xs text-blue-600 hover:text-blue-800 font-medium"
                                                    >
                                                        Oznacz jako przeczytane
                                                    </button>
                                                )}
                                            </div>

                                            <div className="max-h-80 overflow-y-auto">
                                                {notifications.length === 0 ? (
                                                    <div className="px-4 py-6 text-center text-sm text-gray-500">
                                                        Brak nowych powiadomień.
                                                    </div>
                                                ) : (
                                                    notifications.map((notif) => (
                                                        <button
                                                            type="button"
                                                            key={notif.id}
                                                            onClick={() =>
                                                                void handleNotificationClick(notif)}
                                                            className={`block w-full text-left px-4 py-3 border-b border-gray-50 transition hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-500 
                                                            ${!notif.isRead ? 'bg-blue-50/50' : 'bg-white'}`}>
                                                                <span className="flex justify-between items-start mb-1">
                                                                    <span className="text-[10px] text-gray-400 font-medium">{formatGlobalDate(notif.createdAt)}</span>
                                                                    {!notif.isRead && <span className="h-2 w-2 bg-blue-600 rounded-full"></span>}
                                                                </span>
                                                            <span className={`block text-sm ${!notif.isRead ? 'text-gray-900 font-semibold' : 'text-gray-600'}`}>{notif.message}</span>
                                                        </button>
                                                    ))
                                                )}
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* User profile */}
                                <div className="relative" ref={dropdownRef}>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            setIsDropdownOpen(!isDropdownOpen);
                                            setIsNotificationsOpen(false);
                                        }}
                                        className="flex items-center space-x-2 bg-blue-800 hover:bg-blue-900 border border-blue-600 px-3 sm:px-4 py-2 rounded-md text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white"
                                        aria-expanded={isDropdownOpen}
                                        aria-controls="profile-dropdown"
                                        aria-haspopup="true"
                                    >

                                        <span className="hidden sm:inline">Witaj, <strong className="font-semibold">{userName}</strong></span>
                                        <span className="sm:hidden font-semibold">{userName}</span>
                                        <svg className={`w-4 h-4 transition-transform duration-200 ${isDropdownOpen ? 'transform rotate-180' : ''}`}
                                            fill="none"
                                            stroke="currentColor"
                                            viewBox="0 0 24 24"
                                        >
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </button>

                                    {isDropdownOpen && (
                                        <div id="profile-dropdown" className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-lg py-1 border border-gray-100 z-50 animate-fadeIn">
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

                                            <Link
                                                href="/settings"
                                                onClick={() => setIsDropdownOpen(false)}
                                                className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-blue-600 transition"
                                            >
                                                <svg className="w-4 h-4 mr-2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                </svg>
                                                Ustawienia
                                            </Link>

                                            <div className="border-t border-gray-100 my-1"></div>

                                            <button
                                                type="button"
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
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Mobile menu */}
            {isMobileMenuOpen && (
                <div
                    id="mobile-navigation"
                    ref={mobileMenuRef}
                    className="md:hidden bg-blue-800 border-t border-blue-600 absolute w-full left-0 z-50 shadow-xl animate-fadeIn"
                >
                    <div className="flex flex-col">
                        {navLinks.filter(link => link.show).map((link) => (
                            <Link
                                key={link.name}
                                href={link.href}
                                onClick={() => setIsMobileMenuOpen(false)}
                                className={`block px-5 py-4 text-base font-medium border-b border-blue-700/50 last:border-0 transition ${
                                    isActive(link.href)
                                        ? 'bg-blue-900 text-white'
                                        : 'text-blue-100 hover:bg-blue-700 hover:text-white'
                                }`}
                            >
                                {link.name}
                            </Link>
                        ))}
                    </div>
                </div>
            )}
        </nav>
    );
}
