"use client";

import { useState, useEffect } from 'react';
import Link from 'next/link';
import DOMPurify from 'dompurify';
import { formatGlobalDate } from '@/app/utils/formatters';
import AnnouncementModal from './components/AnnouncementModal';
import ConfirmModal from "@/app/components/ConfirmModal"
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import { getAuthToken, decodeJwt, isJwtValid } from '@/app/utils/jwt';
import { API_URL } from '@/app/utils/apiClient';

interface Announcement {
    id: number;
    title: string;
    content: string;
    authorName: string;
    isPinned: boolean;
    createdAt: string;
    updatedAt: string | null;
    canEdit: boolean;
    canDelete: boolean;
}

export default function HomePage() {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    const [userRole, setUserRole] = useState<string | null>(null);
    const [announcements, setAnnouncements] = useState<Announcement[]>([]);
    const [editingAnnouncement, setEditingAnnouncement] = useState<Announcement | null>(null);
    const [announcementError, setAnnouncementError] = useState('');

    const [postToDelete, setPostToDelete] = useState<number | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchAnnouncements = async (page = 0) => {
        setIsLoading(true);
        setAnnouncementError('');

        const token = getAuthToken();

        try {
            const response = await fetch(
                `${API_URL}/api/announcements?page=${page}&size=10`,
                {
                    headers: token ? { Authorization: `Bearer ${token}` } : {}
                }
            );

            if (!response.ok) {
                throw new Error('Nie udało się pobrać ogłoszeń.');
            }

            const data = await response.json();
            setAnnouncements(data.content);
            setTotalPages(data.totalPages);
            setCurrentPage(data.number);
        } catch (error) {
            console.error('Błąd pobierania ogłoszeń:', error);
            setAnnouncementError(
                error instanceof TypeError
                    ? 'Nie udało połączyć się z serwerem'
                    : error instanceof Error
                    ? error.message : 'Wystąpił błąd podczas pobierania ogłoszeń'
            );
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        const token = getAuthToken();

        if (isJwtValid(token)) {
            const payload = decodeJwt(token!);
            setUserRole(payload?.role || null);
        } else {
            setUserRole(null);
        }

        void fetchAnnouncements(0);
    }, []);

    const canAddAnnouncement = userRole === 'ADMINISTRATOR' || userRole === 'MODERATOR';

    const confirmDelete = async () => {
        if (!postToDelete) return;

        setIsDeleting(true);
        const token = getAuthToken();

        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/announcements/${postToDelete}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const isLastOnPage = announcements.length === 1 && currentPage > 0;
                const targetPage = isLastOnPage ? currentPage - 1 : currentPage;

                setPostToDelete(null);
                await fetchAnnouncements(targetPage);
            } else {
                alert('Wystąpił błąd podczas usuwania ogłoszenia.');
            }

        } catch (error) {
            console.error('Błąd serwera:', error);
            alert('Brak połączenia z serwerem.');
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar/>

            <header className="bg-white shadow">
                <div className="max-w-7xl mx-auto py-12 px-4 sm:px-6 lg:px-8 text-center">
                    <h1 className="text-4xl font-extrabold text-gray-900 sm:text-5xl sm:tracking-tight lg:text-6xl">
                        Oddział 0369 Żagań
                    </h1>
                    <p className="max-w-xl mt-5 mx-auto text-xl text-gray-500">
                        Oficjalny portal Polskiego Związku Hodowców Gołębi Pocztowych.
                    </p>
                </div>
            </header>

            <main
                className="flex-grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8 grid grid-cols-1 lg:grid-cols-3 gap-8">

                <section className="lg:col-span-2 space-y-6">
                    <div className="flex items-center justify-between border-b pb-2">
                        <h2 className="text-2xl font-bold text-gray-800">Najnowsze ogłoszenia</h2>

                        {canAddAnnouncement && (
                            <button
                                onClick={() => {
                                    setEditingAnnouncement(null);
                                    setIsModalOpen(true);
                                }}
                                className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2 px-4 rounded-md shadow-sm transition"
                            >
                                + Dodaj ogłoszenie
                            </button>
                        )}
                    </div>

                    <div className="space-y-6">
                        {isLoading ? (
                            <div className="text-center py-12 text-gray-500">
                                Ładowanie ogłoszeń...
                            </div>
                        ) : announcementError ? (
                            <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
                                <p className="text-sm text-red-700">{announcementError}</p>
                                <button
                                    type="button"
                                    onClick={() => void fetchAnnouncements(currentPage)}
                                    className="mt-4 px-4 py-2 border border-red-200 rounded-md text-sm font-medium text-red-700 hover:bg-red-100 transition"
                                >
                                    Spróbuj ponownie
                                </button>
                            </div>
                        ) : announcements.length === 0 ? (
                            <div className="text-center py-12 bg-white rounded-lg shadow-sm border border-gray-100 text-gray-500">
                                Brak aktualnych ogłoszeń.
                            </div>
                        ) : (
                            <>
                                {announcements.map((post) => (
                                    <article
                                        key={post.id}
                                        className={`bg-white p-6 rounded-lg shadow-sm border hover:shadow-md transition duration-200 ${
                                            post.isPinned ? 'border-blue-500 ring-1 ring-blue-500' : 'border-gray-100'
                                        }`}
                                    >
                                        <div className="flex justify-between items-start mb-4">
                                            <div className="flex items-center gap-3">
                                                <h3 className="text-xl font-bold text-blue-700">{post.title}</h3>
                                            </div>

                                            <div className="flex items-center gap-2 ml-4">
                                                {post.isPinned && (
                                                    <svg xmlns="http://www.w3.org/2000/svg" height="24px"
                                                         viewBox="0 -960 960 960" width="24px" fill="#2b7fff"
                                                         className="shrink-0"
                                                    >
                                                        <path d="m640-480 80 80v80H520v240l-40 40-40-40v-240H240v-80l80-80v-280h-40v-80h400v80h-40v280Zm-286 80h252l-46-46v-314H400v314l-46 46Zm126 0Z"/>
                                                    </svg>
                                                )}
                                                <span
                                                    className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded whitespace-nowrap">
                                                        {new Date(post.createdAt).toLocaleDateString('pl-PL', {
                                                        day: '2-digit',
                                                        month: '2-digit',
                                                        year: 'numeric'
                                                  })}
                                                </span>
                                            </div>
                                        </div>

                                        <div
                                            className="prose prose-sm sm:prose-base max-w-none text-gray-600 mt-3 leading-relaxed"
                                            dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(post.content)}}
                                        />

                                        <div
                                            className="mt-4 pt-4 border-t border-gray-50 flex items-end justify-between">

                                            <div className="flex flex-col gap-1">
                                                <div className="text-sm text-gray-400 font-medium">
                                                    Dodał: <span className="text-gray-600">{post.authorName}</span>
                                                </div>
                                                {post.updatedAt && (
                                                    <div className="text-xs text-gray-400 italic">
                                                        Edytowano: {formatGlobalDate(post.updatedAt)}
                                                    </div>
                                                )}
                                            </div>

                                            {/* Edit and delete buttons */}
                                            {(post.canEdit || post.canDelete) && (
                                                <div className="flex items-center gap-2">
                                                    {post.canEdit && (
                                                        <button
                                                            onClick={() => {
                                                                setEditingAnnouncement(post);
                                                                setIsModalOpen(true);
                                                            }}
                                                            className="text-sm bg-gray-100 hover:bg-gray-200 text-gray-700 px-3 py-1 rounded transition font-medium border border-gray-200"
                                                        >
                                                            Edytuj
                                                        </button>
                                                    )}
                                                    {post.canDelete && (
                                                        <button
                                                            onClick={() => setPostToDelete(post.id)}
                                                            className="text-sm bg-white hover:bg-red-50 text-red-600 px-3 py-1 rounded transition font-medium border border-red-200"
                                                        >
                                                            Usuń
                                                        </button>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    </article>
                                ))}

                                {/* Pagination buttons */}
                                {totalPages > 1 && (
                                    <div
                                        className="flex justify-center items-center space-x-4 mt-8 pt-4 border-t border-gray-200">
                                        <button
                                            onClick={() => void fetchAnnouncements(currentPage - 1)}
                                            disabled={currentPage === 0}
                                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                        >
                                            Poprzednia
                                        </button>
                                        <span className="text-sm text-gray-600 font-medium">Strona {currentPage + 1} z {totalPages}</span>
                                        <button
                                            onClick={() => void fetchAnnouncements(currentPage + 1)}
                                            disabled={currentPage === totalPages - 1}
                                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                        >
                                            Następna
                                        </button>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </section>

                <aside className="space-y-6">
                    <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-100">
                        <h3 className="text-lg font-bold text-gray-800 border-b pb-2 mb-4">Szybki dostęp</h3>
                        <ul className="space-y-3">
                            <li>
                                <Link href="/flight-plan"
                                      className="flex items-center text-gray-600 hover:text-blue-600 group transition">
                                    <span
                                        className="w-2 h-2 bg-blue-500 rounded-full mr-3 group-hover:scale-150 transition-transform"></span>
                                    Plan lotów
                                </Link>
                            </li>
                            <li>
                                <Link href="/download"
                                      className="flex items-center text-gray-600 hover:text-blue-600 group transition">
                                    <span
                                        className="w-2 h-2 bg-blue-500 rounded-full mr-3 group-hover:scale-150 transition-transform"></span>
                                    Dokumenty do pobrania
                                </Link>
                            </li>
                            <li>
                                <Link href="/sections"
                                      className="flex items-center text-gray-600 hover:text-blue-600 group transition">
                                    <span
                                        className="w-2 h-2 bg-blue-500 rounded-full mr-3 group-hover:scale-150 transition-transform"></span>
                                    Wykaz sekcji
                                </Link>
                            </li>
                        </ul>
                    </div>
                </aside>
            </main>

            <Footer/>

            <AnnouncementModal
                isOpen={isModalOpen}
                onClose={() => {
                    setIsModalOpen(false);
                    setEditingAnnouncement(null);
                }}
                onSuccess={() => void fetchAnnouncements(0)}
                announcementToEdit={editingAnnouncement}
            />

            <ConfirmModal
                isOpen={postToDelete !== null}
                title="Usuń ogłoszenie"
                message="Czy na pewno chcesz trwale usunąć to ogłoszenie? Tej operacji nie można cofnąć."
                variant="danger"
                confirmLabel="Usuń"
                isLoading={isDeleting}
                onCancel={() => {
                    if (!isDeleting) {
                        setPostToDelete(null);
                    }
                }}
                onConfirm={() => void confirmDelete()}
            />
        </div>
    );
}