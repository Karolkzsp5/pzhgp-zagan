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
                            className="lg:hidden mr-1 min-[375px]:mr-2 p-1 text-white hover:text-gray-200 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white rounded"
                            aria-label="Menu główne"
                            aria-expanded={isMobileMenuOpen}
                            aria-controls="mobile-navigation"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                                {isMobileMenuOpen ? (
                                    <path d="m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z" />
                                ) : (
                                    <path d="M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z" />
                                )}
                            </svg>

                        </button>

                        <Link href="/" className="font-bold text-base sm:text-xl tracking-wide sm:tracking-wider hover:text-gray-200 transition whitespace-nowrap">
                            PZHGP Żagań
                        </Link>
                    </div>

                    <div className="hidden lg:flex flex-1 justify-center gap-4 xl:gap-8">
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
                                        <svg xmlns="http://www.w3.org/2000/svg" className="w-7 h-7 text-white" viewBox="0 -960 960 960" fill="currentColor">
                                            <path d="M192-216v-72h48v-240q0-87 53.5-153T432-763v-53q0-20 14-34t34-14q20 0 34 14t14 34v53q85 16 138.5 82T720-528v240h48v72H192Zm288-276Zm-.21 396Q450-96 429-117.15T408-168h144q0 30-21.21 51t-51 21ZM312-288h336v-240q0-70-49-119t-119-49q-70 0-119 49t-49 119v240Z"/>
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
                                        <svg xmlns="http://www.w3.org/2000/svg" className={`w-5 h-5 transition-transform duration-200 ${isDropdownOpen ? 'rotate-180' : ''}`}
                                             viewBox="0 -960 960 960"
                                             fill="currentColor"
                                             aria-hidden="true"
                                        >
                                            <path d="M480-344 240-584l56-56 184 184 184-184 56 56-240 240Z" />
                                        </svg>
                                    </button>

                                    {isDropdownOpen && (
                                        <div id="profile-dropdown" className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-lg py-1 border border-gray-100 z-50 animate-fadeIn">
                                            {userRole === 'ADMINISTRATOR' && (
                                                <>
                                                    <Link
                                                        href="/admin"
                                                        onClick={() => setIsDropdownOpen(false)}
                                                        className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-blue-600 transition"
                                                    >
                                                        <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 mr-2 text-gray-400 shrink-0" viewBox="0 -960 960 960" fill="currentColor"><path d="m438-339 220-221-51-51-169 170-85-85-51 51 136 136Zm42 243q-135-33-223.5-152.84Q168-368.69 168-515v-229l312-120 312 120v229q0 146.31-88.5 266.16Q615-129 480-96Zm0-75q104-32.25 172-129t68-215v-180l-240-92-240 92v180q0 118.25 68 215t172 129Zm0-308Z"/></svg>

                                                        Panel administratora
                                                    </Link>

                                                    <Link
                                                        href="/found-pigeons/admin"
                                                        onClick={() => setIsDropdownOpen(false)}
                                                        className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-blue-600 transition"
                                                    >
                                                        <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 mr-2 text-gray-400 shrink-0" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                                                            <path d="M216-144q-29.7 0-50.85-21.15Q144-186.3 144-216v-528q0-29.7 21.15-50.85Q186.3-816 216-816h171q8-31 33.5-51.5T480-888q34 0 59.5 20.5T573-816h171q29.7 0 50.85 21.15Q816-773.7 816-744v528q0 29.7-21.15 50.85Q773.7-144 744-144H216Zm0-72h528v-528H216v528Zm72-72h288v-72H288v72Zm0-156h384v-72H288v72Zm0-156h384v-72H288v72Zm209-175q7-7 7-17t-7-17q-7-7-17-7t-17 7q-7 7-7 17t7 17q7 7 17 7t17-7ZM216-216v-528 528Z"/>
                                                        </svg>

                                                        Panel zgłoszeń
                                                    </Link>
                                                </>
                                            )}

                                            <Link
                                                href="/settings"
                                                onClick={() => setIsDropdownOpen(false)}
                                                className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:text-blue-600 transition"
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 mr-2 text-gray-400 shrink-0" viewBox="0 -960 960 960" fill="currentColor">
                                                    <path d="m403-96-22-114q-23-9-44.5-21T296-259l-110 37-77-133 87-76q-2-12-3-24t-1-25q0-13 1-25t3-24l-87-76 77-133 110 37q19-16 40.5-28t44.5-21l22-114h154l22 114q23 9 44.5 21t40.5 28l110-37 77 133-87 76q2 12 3 24t1 25q0 13-1 25t-3 24l87 76-77 133-110-37q-19 16-40.5 28T579-210L557-96H403Zm59-72h36l19-99q38-7 71-26t57-48l96 32 18-30-76-67q6-17 9.5-35.5T696-480q0-20-3.5-38.5T683-554l76-67-18-30-96 32q-24-29-57-48t-71-26l-19-99h-36l-19 99q-38 7-71 26t-57 48l-96-32-18 30 76 67q-6 17-9.5 35.5T264-480q0 20 3.5 38.5T277-406l-76 67 18 30 96-32q24 29 57 48t71 26l19 99Zm18-168q60 0 102-42t42-102q0-60-42-102t-102-42q-60 0-102 42t-42 102q0 60 42 102t102 42Zm0-144Z"/>
                                                </svg>
                                                Ustawienia
                                            </Link>

                                            <div className="border-t border-gray-100 my-1"></div>

                                            <button
                                                type="button"
                                                onClick={handleLogout}
                                                className="w-full flex items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition text-left"
                                            >
                                                <svg xmlns="http://www.w3.org/2000/svg" className="w-4 h-4 mr-2 text-red-600 shrink-0" viewBox="0 -960 960 960" fill="currentColor">
                                                    <path d="M216-144q-29.7 0-50.85-21.15Q144-186.3 144-216v-528q0-29.7 21.15-50.85Q186.3-816 216-816h264v72H216v528h264v72H216Zm432-168-51-51 81-81H384v-72h294l-81-81 51-51 168 168-168 168Z"/>
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
                    className="lg:hidden bg-blue-800 border-t border-blue-600 absolute w-full left-0 z-50 shadow-xl animate-fadeIn"
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
