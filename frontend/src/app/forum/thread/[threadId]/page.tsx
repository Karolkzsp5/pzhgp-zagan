"use client";

import { useState, useEffect, use, useRef, useCallback } from 'react';
import Link from 'next/link';
import DOMPurify from 'dompurify';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import TextEditor from '@/app/components/TextEditor';
import ForumGuard from '@/app/components/ForumGuard';
import { useRouter } from 'next/navigation';
import ConfirmModal from '@/app/components/ConfirmModal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import { formatGlobalDate, formatRole } from '@/app/utils/formatters';
import { isHtmlEmpty } from '@/app/utils/richText';
import {
    fetchThreadById,
    fetchPostsByThread,
    createPost,
    deleteThread,
    toggleThreadStatus,
    deletePost,
    updatePost,
    updateThreadTitle,
    ForumThreadDto,
    ForumPostDto
} from '@/app/services/forumService';

export default function ThreadViewPage({ params }: { params: Promise<{ threadId: string }> }) {
    const resolvedParams = use(params);
    const threadId = Number(resolvedParams.threadId);
    const isValidThreadId = Number.isInteger(threadId) && threadId > 0;
    const router = useRouter();

    const [thread, setThread] = useState<(ForumThreadDto & { categoryName?: string }) | null>(null);
    const [posts, setPosts] = useState<ForumPostDto[]>([]);

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    const [replyContent, setReplyContent] = useState('');
    const [isReplying, setIsReplying] = useState(false);
    const [replyError, setReplyError] = useState('');

    const [editingPostId, setEditingPostId] = useState<number | null>(null);
    const [editContent, setEditContent] = useState('');
    const [isEditSubmitting, setIsEditSubmitting] = useState(false);
    const [editError, setEditError] = useState('');

    const [isOptionsOpen, setIsOptionsOpen] = useState(false);
    const optionsRef = useRef<HTMLDivElement>(null);

    const [isEditingTitle, setIsEditingTitle] = useState(false);
    const [editTitleContent, setEditTitleContent] = useState('');
    const [isTitleSubmitting, setIsTitleSubmitting] = useState(false);

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const closeConfirmModal = () => setModalConfig(prev => ({ ...prev, isOpen: false }));

    const showAlert = (title: string, message: string) => {
        setModalConfig({
            isOpen: true,
            title,
            message,
            isAlert: true,
            onConfirm: closeConfirmModal
        });
    };

    const cancelEditing = () => {
        setEditingPostId(null);
        setEditContent('');
        setEditError('');
    };

    const loadThreadData = useCallback(async () => {
        setIsLoading(true);
        setError('');

        try {
            const [threadData, postsData] = await Promise.all([
                fetchThreadById(threadId),
                fetchPostsByThread(threadId, currentPage)
            ]);

            setThread(threadData);
            setTotalPages(postsData.totalPages);

            if (postsData.content.length === 0 && currentPage > 0) {
                setCurrentPage(prev => prev - 1);
                return;
            }

            setPosts(postsData.content);
            setEditingPostId(null);
            setEditContent('');
            setEditError('');
        } catch (error) {
            setError(
                error instanceof TypeError
                    ? 'Nie udało połączyć się z serwerem.'
                    : error instanceof Error
                        ? error.message
                        : 'Nie udało się pobrać dyskusji.'
            );
        } finally {
            setIsLoading(false);
        }
    }, [threadId, currentPage]);

    useEffect(() => {
        if (isValidThreadId) {
            void loadThreadData();
        }
    }, [isValidThreadId, loadThreadData]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (optionsRef.current && !optionsRef.current.contains(event.target as Node)) {
                setIsOptionsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleReplySubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setReplyError('');

        if (isHtmlEmpty(replyContent)) {
            setReplyError('Treść odpowiedzi nie może być pusta.');
            return;
        }

        setIsReplying(true);

        try {
            await createPost(threadId, replyContent);
            setReplyContent('');

            const updatedThread = await fetchThreadById(threadId);
            setThread(updatedThread);

            const totalPostsCount = (updatedThread.repliesCount || 0) + 1;
            const targetPage = Math.max(0, Math.ceil(totalPostsCount / 20) - 1);

            if (currentPage !== targetPage) {
                setCurrentPage(targetPage);
            } else {
                await loadThreadData();
            }
        } catch (error) {
            setReplyError(
                error instanceof Error
                    ? error.message
                    : 'Wystąpił błąd podczas publikowania odpowiedzi.'
            );
        } finally {
            setIsReplying(false);
        }
    };

    const handleDeleteThread = () => {
        setModalConfig({
            isOpen: true,
            title: 'Usuń wątek',
            message: 'Czy na pewno chcesz usunąć ten wątek wraz ze wszystkimi odpowiedziami? Operacja jest nieodwracalna.',
            isAlert: false,
            onConfirm: async () => {
                closeConfirmModal();

                try {
                    await deleteThread(threadId);
                    router.push(thread ? `/forum/${thread.categoryId}` : '/forum');
                } catch (error) {
                    showAlert(
                        'Błąd',
                        error instanceof Error
                            ? error.message
                            : 'Wystąpił błąd podczas usuwania wątku.'
                    );
                }
            }
        });
    };

    const handleToggleStatus = async (action: 'LOCK' | 'PIN') => {
        try {
            await toggleThreadStatus(threadId, action);

            setThread(prev => {
                if (!prev) return prev;

                return {
                    ...prev,
                    isLocked: action === 'LOCK' ? !prev.isLocked : prev.isLocked,
                    isPinned: action === 'PIN' ? !prev.isPinned : prev.isPinned
                };
            });
        } catch (error) {
            showAlert(
                'Błąd',
                error instanceof Error
                    ? error.message
                    : `Wystąpił błąd (${action}).`
            );
        }
    };

    const handleTitleEditSubmit = async () => {
        const cleanTitle = editTitleContent.trim();

        if (cleanTitle.length < 5 || cleanTitle.length > 150) {
            showAlert('Błąd walidacji', 'Tytuł wątku musi mieć od 5 do 150 znaków.');
            return;
        }

        setIsTitleSubmitting(true);

        try {
            await updateThreadTitle(threadId, cleanTitle);
            setThread(prev => prev ? { ...prev, title: cleanTitle } : prev);
            setIsEditingTitle(false);
        } catch (error) {
            showAlert(
                'Błąd edycji',
                error instanceof Error
                    ? error.message
                    : 'Wystąpił błąd podczas zapisywania tytułu.'
            );
        } finally {
            setIsTitleSubmitting(false);
        }
    };

    const startEditing = (post: ForumPostDto) => {
        setEditingPostId(post.id);
        setEditContent(post.body);
        setEditError('');
    };

    const handleEditSubmit = async (postId: number) => {
        if (isHtmlEmpty(editContent)) {
            setEditError('Treść wpisu nie może być pusta.');
            return;
        }

        setIsEditSubmitting(true);
        setEditError('');

        try {
            await updatePost(postId, editContent);
            cancelEditing();
            await loadThreadData();
        } catch (error) {
            setEditError(
                error instanceof Error
                    ? error.message
                    : 'Wystąpił błąd podczas zapisywania wpisu.'
            );
        } finally {
            setIsEditSubmitting(false);
        }
    };

    const handleDeletePost = (postId: number) => {
        setModalConfig({
            isOpen: true,
            title: 'Usuń wpis',
            message: 'Czy na pewno chcesz usunąć ten wpis?',
            isAlert: false,
            onConfirm: async () => {
                closeConfirmModal();

                try {
                    await deletePost(postId);
                    await loadThreadData();
                } catch (error) {
                    showAlert(
                        'Nie można usunąć',
                        error instanceof Error
                            ? error.message
                            : 'Nie można usunąć jedynego wpisu. Spróbuj usunąć cały wątek.'
                    );
                }
            }
        });
    };

    if (!isValidThreadId) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="text-center py-16 text-red-600 font-medium text-xl">
                    Nieprawidłowy adres URL. Wątek nie istnieje.
                </div>
            </div>
        );
    }

    return (
        <ForumGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-5xl mx-auto w-full py-8 px-4 sm:px-6 lg:px-8">
                    <nav className="flex text-sm text-gray-500 mb-6 font-medium">
                        <Link href="/forum" className="hover:text-blue-600 transition">
                            Forum
                        </Link>

                        <span className="mx-2">/</span>

                        {thread?.categoryId ? (
                            <Link href={`/forum/${thread.categoryId}`} className="hover:text-blue-600 transition">
                                Wróć do kategorii
                            </Link>
                        ) : (
                            <span className="text-gray-400">
                                Kategoria
                            </span>
                        )}

                        <span className="mx-2">/</span>
                        <span className="text-gray-800 line-clamp-1">{thread?.title || 'Wątek'}</span>
                    </nav>

                    {isLoading ? (
                        <LoadingState />
                    ) : error ? (
                        <ErrorState message={error} onRetry={() => void loadThreadData()} />
                    ) : thread ? (
                        <>
                            <div className="bg-white p-6 rounded-t-lg shadow-sm border border-gray-200 border-b-0 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                                <div className="flex-grow w-full sm:w-auto">
                                    {isEditingTitle ? (
                                        <div className="flex items-center gap-2 w-full max-w-2xl">
                                            <input
                                                type="text"
                                                value={editTitleContent}
                                                onChange={(e) => setEditTitleContent(e.target.value)}
                                                className="flex-grow px-3 py-1.5 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 outline-none font-bold text-xl text-gray-900"
                                                disabled={isTitleSubmitting}
                                                aria-label="Tytuł wątku"
                                                autoFocus
                                            />

                                            <button
                                                type="button"
                                                onClick={() => void handleTitleEditSubmit()}
                                                disabled={isTitleSubmitting}
                                                className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-md transition shadow-sm disabled:opacity-50"
                                            >
                                                Zapisz
                                            </button>

                                            <button
                                                type="button"
                                                onClick={() => setIsEditingTitle(false)}
                                                disabled={isTitleSubmitting}
                                                className="px-4 py-1.5 border border-gray-300 hover:bg-gray-50 text-gray-700 text-sm font-medium rounded-md transition disabled:opacity-50"
                                            >
                                                Anuluj
                                            </button>
                                        </div>
                                    ) : (
                                        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2 flex-wrap">
                                            {thread.isPinned && (
                                                <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6 text-blue-500 shrink-0" fill="currentColor" viewBox="0 -960 960 960" aria-hidden="true">
                                                    <path d="m640-480 80 80v80H520v240l-40 40-40-40v-240H240v-80l80-80v-280h-40v-80h400v80h-40v280Zm-286 80h252l-46-46v-314H400v314l-46 46Zm126 0Z"/>
                                                </svg>
                                            )}

                                            {thread.isLocked && (
                                                <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6 text-amber-500 shrink-0" fill="currentColor" viewBox="0 -960 960 960" aria-hidden="true">
                                                    <path d="M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm296.5-143.5Q560-327 560-360t-23.5-56.5Q513-440 480-440t-56.5 23.5Q400-393 400-360t23.5 56.5Q447-280 480-280t56.5-23.5ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z"/>
                                                </svg>
                                            )}

                                            {thread.title}

                                            {thread.canEdit && (
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        setEditTitleContent(thread.title);
                                                        setIsEditingTitle(true);
                                                    }}
                                                    className="ml-1 p-1 text-gray-400 hover:text-blue-600 transition rounded-full hover:bg-blue-50"
                                                    aria-label="Edytuj tytuł wątku"
                                                    title="Edytuj tytuł wątku"
                                                >
                                                    <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="currentColor" aria-hidden="true">
                                                        <path d="M200-200h57l391-391-57-57-391 391v57Zm-80 80v-170l528-527q12-11 26.5-17t30.5-6q16 0 31 6t26 18l55 56q12 11 17.5 26t5.5 30q0 16-5.5 30.5T817-647L290-120H120Zm640-584-56-56 56 56Zm-141 85-28-29 57 57-29-28Z"/>
                                                    </svg>
                                                </button>
                                            )}
                                        </h1>
                                    )}

                                    <div className="mt-2 text-sm text-gray-500">
                                        Rozpoczęte przez{' '}
                                        <span className="font-semibold text-gray-700">{thread.authorName}</span>
                                        , {formatGlobalDate(thread.createdAt)}
                                    </div>
                                </div>

                                {(thread.canModerate || thread.canDelete) && (
                                    <div className="relative" ref={optionsRef}>
                                        <button
                                            type="button"
                                            onClick={() => setIsOptionsOpen(!isOptionsOpen)}
                                            className="flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-semibold rounded-md transition"
                                            aria-expanded={isOptionsOpen}
                                            aria-haspopup="menu"
                                        >
                                            Zarządzaj

                                            <svg xmlns="http://www.w3.org/2000/svg" className={`w-6 h-6 transition-transform duration-200 ${isOptionsOpen ? 'rotate-180' : ''}`} viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                                                <path d="M480-344 240-584l56-56 184 184 184-184 56 56-240 240Z" />
                                            </svg>
                                        </button>

                                        {isOptionsOpen && (
                                            <div className="absolute left-0 sm:left-auto sm:right-0 mt-2 w-max min-w-[160px] bg-white border border-gray-200 rounded-md shadow-lg z-50 overflow-hidden"
                                                 role="menu">
                                                {thread.canModerate && (
                                                    <>
                                                        <button
                                                            type="button"
                                                            onClick={() => {
                                                                void handleToggleStatus('PIN');
                                                                setIsOptionsOpen(false);
                                                            }}
                                                            className="block w-full text-left px-4 py-3 text-sm text-gray-700 hover:bg-blue-50 transition"
                                                            role="menuitem"
                                                        >
                                                            {thread.isPinned ? 'Odepnij wątek' : 'Przypnij wątek'}
                                                        </button>

                                                        <button
                                                            type="button"
                                                            onClick={() => {
                                                                void handleToggleStatus('LOCK');
                                                                setIsOptionsOpen(false);
                                                            }}
                                                            className="block w-full text-left px-4 py-3 text-sm text-gray-700 hover:bg-amber-50 transition border-t border-gray-100"
                                                            role="menuitem"
                                                        >
                                                            {thread.isLocked ? 'Odblokuj wątek' : 'Zablokuj wątek'}
                                                        </button>
                                                    </>
                                                )}

                                                {thread.canDelete && (
                                                    <button
                                                        type="button"
                                                        onClick={() => {
                                                            handleDeleteThread();
                                                            setIsOptionsOpen(false);
                                                        }}
                                                        className="block w-full text-left px-4 py-3 text-sm text-red-600 font-medium hover:bg-red-50 transition border-t border-gray-100"
                                                        role="menuitem"
                                                    >
                                                        Usuń wątek
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>

                            <div className="bg-white shadow-sm border border-gray-200 rounded-b-lg overflow-hidden flex flex-col">
                                <div className="divide-y divide-gray-200">
                                    {posts.map((post) => (
                                        <div key={post.id} className="flex flex-col sm:flex-row p-0">
                                            <div className="bg-gray-50 sm:w-48 p-4 sm:p-6 sm:border-r border-gray-100 shrink-0">
                                                <div className="font-bold text-gray-900 break-words">
                                                    {post.authorName}
                                                </div>

                                                <div className="text-xs text-gray-500 mt-1">
                                                    {formatRole(post.authorRole)}
                                                </div>
                                            </div>

                                            <div className="p-4 sm:p-6 grow flex flex-col min-w-0">
                                                <div className="text-xs text-gray-400 mb-4 pb-2 border-b border-gray-100 flex justify-between">
                                                    <span>Napisano: {formatGlobalDate(post.createdAt)}</span>

                                                    {post.editedAt && (
                                                        <span className="italic" title={formatGlobalDate(post.editedAt)}>
                                                            (Edytowano)
                                                        </span>
                                                    )}
                                                </div>

                                                {editingPostId === post.id ? (
                                                    <div className="mt-4">
                                                        <TextEditor
                                                            content={editContent}
                                                            onChange={setEditContent}
                                                            ariaLabel="Edytowana treść wpisu"
                                                        />

                                                        {editError && (
                                                            <div className="mt-3 bg-red-50 text-red-600 p-2 rounded text-sm border border-red-100" role="alert">
                                                                {editError}
                                                            </div>
                                                        )}

                                                        <div className="mt-3 flex justify-end gap-2">
                                                            <button
                                                                type="button"
                                                                onClick={cancelEditing}
                                                                disabled={isEditSubmitting}
                                                                className="px-4 py-1.5 border border-gray-300 rounded text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
                                                            >
                                                                Anuluj
                                                            </button>

                                                            <button
                                                                type="button"
                                                                onClick={() => void handleEditSubmit(post.id)}
                                                                disabled={isEditSubmitting}
                                                                className={`px-4 py-1.5 rounded text-sm font-medium text-white transition shadow-sm ${
                                                                    isEditSubmitting
                                                                        ? 'bg-blue-400 cursor-not-allowed'
                                                                        : 'bg-blue-600 hover:bg-blue-700'
                                                                }`}
                                                            >
                                                                {isEditSubmitting ? 'Zapisywanie...' : 'Zapisz zmiany'}
                                                            </button>
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <>
                                                        <div className="prose prose-sm sm:prose-base max-w-none text-gray-800 break-words grow prose-p:my-0"
                                                            dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.body) }}
                                                        />

                                                        {(post.canEdit || post.canDelete) && (
                                                            <div className="mt-6 pt-3 flex justify-end gap-3 border-t border-gray-50">
                                                                {post.canEdit && (
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => startEditing(post)}
                                                                        className="text-xs font-medium text-gray-500 hover:text-blue-600 transition"
                                                                    >
                                                                        Edytuj
                                                                    </button>
                                                                )}

                                                                {post.canDelete && (
                                                                    <button
                                                                        type="button"
                                                                        onClick={() => handleDeletePost(post.id)}
                                                                        className="text-xs font-medium text-gray-500 hover:text-red-600 transition"
                                                                    >
                                                                        Usuń
                                                                    </button>
                                                                )}
                                                            </div>
                                                        )}
                                                    </>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {totalPages > 1 && (
                                <div className="flex justify-center items-center space-x-4 mt-6">
                                    <button
                                        type="button"
                                        onClick={() => setCurrentPage(prev => prev - 1)}
                                        disabled={currentPage === 0}
                                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                    >
                                        Poprzednia
                                    </button>

                                    <span className="text-sm text-gray-600 font-medium">Strona {currentPage + 1} z {totalPages}</span>

                                    <button
                                        type="button"
                                        onClick={() => setCurrentPage(prev => prev + 1)}
                                        disabled={currentPage === totalPages - 1}
                                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                                    >
                                        Następna
                                    </button>
                                </div>
                            )}

                            <div className="mt-8 pt-8 border-t border-gray-200">
                                {thread.isLocked ? (
                                    <div className="bg-amber-50 p-6 rounded-lg text-center border border-amber-200">
                                        <svg xmlns="http://www.w3.org/2000/svg" className="w-8 h-8 text-amber-500 mx-auto mb-2" fill="currentColor" viewBox="0 -960 960 960" aria-hidden="true">
                                            <path d="M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm296.5-143.5Q560-327 560-360t-23.5-56.5Q513-440 480-440t-56.5 23.5Q400-393 400-360t23.5 56.5Q447-280 480-280t56.5-23.5ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z"/>
                                        </svg>

                                        <h3 className="text-lg font-bold text-amber-800">
                                            Wątek zablokowany
                                        </h3>

                                        <p className="text-sm text-amber-700 mt-1">
                                            Ten wątek został zamknięty przez moderatora. Nie można dodawać nowych odpowiedzi.
                                        </p>
                                    </div>
                                ) : (
                                    <form onSubmit={handleReplySubmit} className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                                        <h3 className="text-lg font-bold text-gray-900 mb-4">
                                            Dodaj odpowiedź
                                        </h3>

                                        <TextEditor
                                            content={replyContent}
                                            onChange={setReplyContent}
                                            ariaLabel="Treść odpowiedzi"
                                        />

                                        {replyError && (
                                            <div
                                                className="mt-3 bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100"
                                                role="alert"
                                            >
                                                {replyError}
                                            </div>
                                        )}

                                        <div className="mt-4 flex justify-end">
                                            <button
                                                type="submit"
                                                disabled={isReplying}
                                                className={`px-8 py-2.5 rounded-md text-sm font-bold text-white shadow-sm transition ${
                                                    isReplying
                                                        ? 'bg-blue-400 cursor-not-allowed'
                                                        : 'bg-blue-600 hover:bg-blue-700'
                                                }`}
                                            >
                                                {isReplying ? 'Wysyłanie...' : 'Opublikuj odpowiedź'}
                                            </button>
                                        </div>
                                    </form>
                                )}
                            </div>
                        </>
                    ) : null}
                </main>

                <Footer />

                <ConfirmModal
                    isOpen={modalConfig.isOpen}
                    title={modalConfig.title}
                    message={modalConfig.message}
                    isAlert={modalConfig.isAlert}
                    variant={modalConfig.isAlert ? 'primary' : 'danger'}
                    onConfirm={modalConfig.onConfirm}
                    onCancel={closeConfirmModal}
                />
            </div>
        </ForumGuard>
    );
}