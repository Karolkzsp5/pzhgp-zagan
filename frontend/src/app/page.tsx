"use client";

import { useCallback, useEffect, useState } from 'react';
import DOMPurify from 'dompurify';
import { formatGlobalDate } from '@/app/utils/formatters';
import AnnouncementModal from '@/app/components/AnnouncementModal';
import ConfirmModal from "@/app/components/ConfirmModal"
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { useAuth } from '@/app/components/AuthProvider';
import useLatestRequest from '@/app/hooks/useLatestRequest';

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
    const { user, isAuthResolved } = useAuth();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [actionError, setActionError] = useState<string | null>(null);
    const { beginRequest, isLatestRequest } = useLatestRequest();

    const [announcements, setAnnouncements] = useState<Announcement[]>([]);
    const [editingAnnouncement, setEditingAnnouncement] = useState<Announcement | null>(null);
    const [announcementError, setAnnouncementError] = useState('');

    const [postToDelete, setPostToDelete] = useState<number | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchAnnouncements = useCallback(async (page = 0) => {
        const requestId = beginRequest();

        setIsLoading(true);
        setAnnouncementError('');

        try {
            const response = await fetchWithAuth(`${API_URL}/api/announcements?page=${page}&size=10`);

            if (!response.ok) {
                throw new Error(await readApiError(response, 'Nie udało się pobrać ogłoszeń.'));
            }

            const data = await response.json();

            if (!isLatestRequest(requestId)) {
                return;
            }

            setAnnouncements(data.content);
            setTotalPages(data.totalPages);
            setCurrentPage(data.number);
        } catch (error) {
            if (!isLatestRequest(requestId)) {
                return;
            }

            setAnnouncementError(error instanceof TypeError ? 'Nie udało się połączyć z serwerem.' :
                error instanceof Error ? error.message : 'Wystąpił błąd podczas pobierania ogłoszeń.'
            );
        } finally {
            if (isLatestRequest(requestId)) {
                setIsLoading(false);
            }
        }
    }, [beginRequest, isLatestRequest]);

    useEffect(() => {
        if (!isAuthResolved) return;

        void fetchAnnouncements(0);
    }, [fetchAnnouncements, isAuthResolved, user?.sub, user?.role]);

    const canAddAnnouncement = user?.role === 'ADMINISTRATOR' || user?.role === 'MODERATOR';

    const confirmDelete = async () => {
        if (postToDelete === null) return;

        const deletingId = postToDelete;
        setIsDeleting(true);

        try {
            const response = await fetchWithAuth(`${API_URL}/api/announcements/${deletingId}`, {
                    method: 'DELETE'
            });

            if (!response.ok) {
                const message = await readApiError(response, 'Nie udało się usunąć ogłoszenia.');

                setPostToDelete(null);
                setActionError(message);
                return;
            }

            const isLastOnPage = announcements.length === 1 && currentPage > 0;
            const targetPage = isLastOnPage ? currentPage - 1 : currentPage;

            setPostToDelete(null);
            await fetchAnnouncements(targetPage);
        } catch (error) {
            setPostToDelete(null);
            setActionError(error instanceof Error ? error.message : 'Nie udało się usunąć ogłoszenia.');
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

            <main className="flex-grow max-w-5xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                <section className="space-y-6">
                    <div className="flex items-center justify-between border-b border-gray-200 pb-2">
                        <h2 className="text-2xl font-bold text-gray-800">Najnowsze ogłoszenia</h2>

                        {canAddAnnouncement && (
                            <button
                                type="button"
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
                                <p className="text-sm text-red-700">
                                    {announcementError}
                                </p>
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
                                                    <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#2b7fff" className="shrink-0" aria-hidden="true">
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

                                        <div className="prose prose-sm sm:prose-base max-w-[820px] w-full px-4 box-border text-gray-600 mt-3 leading-relaxed prose-p:my-0"
                                            dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(post.content)}}
                                        />

                                        <div className="mt-4 pt-4 border-t border-gray-50 flex items-end justify-between">

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
                                                            type="button"
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
                                                            type="button"
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
                                    <div className="flex justify-center items-center space-x-4 mt-8 pt-4 border-t border-gray-200">
                                        <button
                                            type="button"
                                            onClick={() => void fetchAnnouncements(currentPage - 1)}
                                            disabled={isLoading || currentPage === 0}
                                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                        >
                                            Poprzednia
                                        </button>
                                        <span className="text-sm text-gray-600 font-medium">
                                            Strona {currentPage + 1} z {totalPages}
                                        </span>
                                        <button
                                            type="button"
                                            onClick={() => void fetchAnnouncements(currentPage + 1)}
                                            disabled={isLoading || currentPage === totalPages - 1}
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

            <ConfirmModal
                isOpen={actionError !== null}
                title="Nie udało się wykonać operacji"
                message={actionError ?? ''}
                isAlert
                variant="primary"
                confirmLabel="OK"
                onConfirm={() => setActionError(null)}
                onCancel={() => setActionError(null)}
            />
        </div>
    );
}