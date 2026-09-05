"use client";

import { useState, useEffect, use, useRef } from 'react';
import Link from 'next/link';
import DOMPurify from 'dompurify';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import TextEditor from '@/app/components/TextEditor';
import ForumGuard from '@/app/components/ForumGuard';
import { useRouter } from 'next/navigation';
import {
    fetchThreadById,
    fetchPostsByThread,
    createPost,
    deleteThread,
    toggleThreadStatus,
    deletePost,
    updatePost,
    ForumThreadDto,
    ForumPostDto
} from '@/app/services/forumService';

const getRoleDisplayName = (role?: string) => {
    switch (role) {
        case 'ADMINISTRATOR': return 'Administrator';
        case 'MODERATOR': return 'Moderator';
        case 'BREEDER': return 'Hodowca';
        default: return 'Hodowca';
    }
};

export default function ThreadViewPage({ params }: { params: Promise<{ threadId: string }> }) {
    const resolvedParams = use(params);
    const threadId = parseInt(resolvedParams.threadId);
    const router = useRouter();

    const [thread, setThread] = useState<(ForumThreadDto & { categoryId?: number; categoryName?: string }) | null>(null);
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

    useEffect(() => {
        loadThreadData();
    }, [threadId, currentPage]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (optionsRef.current && !optionsRef.current.contains(event.target as Node)) {
                setIsOptionsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const loadThreadData = async () => {
        setIsLoading(true);
        setError('');
        try {
            if (!thread) {
                const threadData = await fetchThreadById(threadId);
                setThread(threadData);
            }

            const postsData = await fetchPostsByThread(threadId, currentPage);
            setPosts(postsData.content);
            setTotalPages(postsData.totalPages);

            cancelEditing();

        } catch (err) {
            setError('Nie udało się pobrać dyskusji. Sprawdź połączenie.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleReplySubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setReplyError('');

        if (!replyContent || replyContent === '<p></p>') {
            setReplyError('Treść odpowiedzi nie może być pusta.');
            return;
        }

        setIsReplying(true);
        try {
            await createPost(threadId, replyContent);
            setReplyContent('');

            if (currentPage !== Math.max(0, totalPages - 1)) {
                setCurrentPage(Math.max(0, totalPages - 1));
            } else {
                loadThreadData();
            }
        } catch (err) {
            setReplyError('Wystąpił błąd podczas publikowania odpowiedzi.');
        } finally {
            setIsReplying(false);
        }
    };

    const handleDeleteThread = async () => {
        if (!window.confirm('Czy na pewno chcesz usunąć ten wątek wraz ze wszystkimi odpowiedziami? Operacja jest nieodwracalna.')) return;
        try {
            await deleteThread(threadId);
            router.push(thread?.categoryId ? `/forum/${thread.categoryId}` : '/forum');
        } catch (err: any) {
            alert(err.message || 'Wystąpił błąd podczas usuwania wątku.');
        }
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
        } catch (err: any) {
            alert(err.message || `Wystąpił błąd (${action}).`);
        }
    };

    const startEditing = (post: ForumPostDto) => {
        setEditingPostId(post.id);
        setEditContent(post.body);
        setEditError('');
    };

    const cancelEditing = () => {
        setEditingPostId(null);
        setEditContent('');
        setEditError('');
    };

    const handleEditSubmit = async (postId: number) => {
        if (!editContent || editContent === '<p></p>') {
            setEditError('Treść wpisu nie może być pusta.');
            return;
        }

        setIsEditSubmitting(true);
        setEditError('');

        try {
            await updatePost(postId, editContent);
            cancelEditing();
            await loadThreadData();
        } catch (err: any) {
            setEditError(err.message || 'Wystąpił błąd podczas zapisywania wpisu.');
        } finally {
            setIsEditSubmitting(false);
        }
    };

    const handleDeletePost = async (postId: number) => {
        if (!window.confirm('Czy na pewno chcesz usunąć ten wpis?')) return;
        try {
            await deletePost(postId);
            loadThreadData();
        } catch (err: any) {
            alert(err.message || 'Nie można usunąć jedynego wpisu. Spróbuj usunąć cały wątek.');
        }
    };

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('pl-PL', {
            day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute:'2-digit'
        });
    };

    return (
        <ForumGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-5xl mx-auto w-full py-8 px-4 sm:px-6 lg:px-8">
                    <nav className="flex text-sm text-gray-500 mb-6 font-medium">
                        <Link href="/forum" className="hover:text-blue-600 transition">Forum</Link>
                        <span className="mx-2">/</span>
                        {thread?.categoryId ? (
                            <Link href={`/forum/${thread.categoryId}`} className="hover:text-blue-600 transition">
                                Wróć do kategorii
                            </Link>
                        ) : (
                            <span className="text-gray-400">Kategoria</span>
                        )}
                        <span className="mx-2">/</span>
                        <span className="text-gray-800 line-clamp-1">{thread?.title || 'Ładowanie...'}</span>
                    </nav>

                    {error && (
                        <div className="bg-red-50 border-l-4 border-red-400 p-4 shadow-sm mb-6">
                            <p className="text-sm text-red-700">{error}</p>
                        </div>
                    )}

                    {thread && (
                        <div className="bg-white p-6 rounded-t-lg shadow-sm border border-gray-200 border-b-0 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                            <div>
                                <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                                    {thread.isPinned && (
                                        <svg className="w-6 h-6 text-blue-500 shrink-0" fill="currentColor" viewBox="0 -960 960 960">
                                            <path d="m640-480 80 80v80H520v240l-40 40-40-40v-240H240v-80l80-80v-280h-40v-80h400v80h-40v280Zm-286 80h252l-46-46v-314H400v314l-46 46Zm126 0Z"/>
                                        </svg>
                                    )}
                                    {thread.isLocked && (
                                        <svg className="w-6 h-6 text-amber-500 shrink-0" fill="currentColor" viewBox="0 -960 960 960">
                                            <path d="M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm296.5-143.5Q560-327 560-360t-23.5-56.5Q513-440 480-440t-56.5 23.5Q400-393 400-360t23.5 56.5Q447-280 480-280t56.5-23.5ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z"/>
                                        </svg>
                                    )}
                                    {thread.title}
                                </h1>
                                <div className="mt-2 text-sm text-gray-500">
                                    Rozpoczęte przez <span className="font-semibold text-gray-700">{thread.authorName}</span>, {formatDate(thread.createdAt)}
                                </div>
                            </div>

                            {(thread.canModerate || thread.canDelete) && (
                                <div className="relative" ref={optionsRef}>
                                    <button
                                        onClick={() => setIsOptionsOpen(!isOptionsOpen)}
                                        className="flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-semibold rounded-md transition"
                                    >
                                        Zarządzaj
                                        <svg className={`w-4 h-4 transition-transform ${isOptionsOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </button>

                                    {isOptionsOpen && (
                                        <div className="absolute left-0 sm:left-auto sm:right-0 mt-2 w-max min-w-[160px] bg-white border border-gray-200 rounded-md shadow-lg z-50 overflow-hidden">                                            {thread.canModerate && (
                                                <>
                                                    <button
                                                        onClick={() => { handleToggleStatus('PIN'); setIsOptionsOpen(false); }}
                                                        className="block w-full text-left px-4 py-3 text-sm text-gray-700 hover:bg-blue-50 transition"
                                                    >
                                                        {thread.isPinned ? 'Odepnij wątek' : 'Przypnij wątek'}
                                                    </button>
                                                    <button
                                                        onClick={() => { handleToggleStatus('LOCK'); setIsOptionsOpen(false); }}
                                                        className="block w-full text-left px-4 py-3 text-sm text-gray-700 hover:bg-amber-50 transition border-t border-gray-100"
                                                    >
                                                        {thread.isLocked ? 'Odblokuj wątek' : 'Zablokuj wątek'}
                                                    </button>
                                                </>
                                            )}
                                            {thread.canDelete && (
                                                <button
                                                    onClick={() => { handleDeleteThread(); setIsOptionsOpen(false); }}
                                                    className="block w-full text-left px-4 py-3 text-sm text-red-600 font-medium hover:bg-red-50 transition border-t border-gray-100"
                                                >
                                                    Usuń wątek
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                    <div className="bg-white shadow-sm border border-gray-200 rounded-b-lg overflow-hidden flex flex-col">
                        {isLoading && posts.length === 0 ? (
                            <div className="flex justify-center items-center py-20">
                                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
                            </div>
                        ) : (
                            <div className="divide-y divide-gray-200">
                                {posts.map((post) => (
                                    <div key={post.id} className="flex flex-col sm:flex-row p-0">
                                        <div className="bg-gray-50 sm:w-48 p-4 sm:p-6 sm:border-r border-gray-100 shrink-0">
                                            <div className="font-bold text-gray-900 break-words">{post.authorName}</div>
                                            <div className="text-xs text-gray-500 mt-1">
                                                {getRoleDisplayName(post.authorRole)}
                                            </div>
                                        </div>

                                        <div className="p-4 sm:p-6 grow flex flex-col min-w-0">
                                            <div className="text-xs text-gray-400 mb-4 pb-2 border-b border-gray-100 flex justify-between">
                                                <span>Napisano: {formatDate(post.createdAt)}</span>
                                                {post.editedAt && (
                                                    <span className="italic" title={formatDate(post.editedAt)}>
                                                        (Edytowano)
                                                    </span>
                                                )}
                                            </div>

                                            {editingPostId === post.id ? (
                                                <div className="mt-4">
                                                    <TextEditor content={editContent} onChange={setEditContent} />

                                                    {editError && (
                                                        <div className="mt-3 bg-red-50 text-red-600 p-2 rounded text-sm border border-red-100">
                                                            {editError}
                                                        </div>
                                                    )}

                                                    <div className="mt-3 flex justify-end gap-2">
                                                        <button
                                                            onClick={cancelEditing}
                                                            disabled={isEditSubmitting}
                                                            className="px-4 py-1.5 border border-gray-300 rounded text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
                                                        >
                                                            Anuluj
                                                        </button>
                                                        <button
                                                            onClick={() => handleEditSubmit(post.id)}
                                                            disabled={isEditSubmitting}
                                                            className={`px-4 py-1.5 rounded text-sm font-medium text-white transition shadow-sm ${
                                                                isEditSubmitting ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'
                                                            }`}
                                                        >
                                                            {isEditSubmitting ? 'Zapisywanie...' : 'Zapisz zmiany'}
                                                        </button>
                                                    </div>
                                                </div>
                                            ) : (
                                                <>
                                                    <div
                                                        className="prose prose-sm sm:prose-base max-w-none text-gray-800 break-words grow prose-p:my-0"
                                                        dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.body) }}
                                                    />

                                                    {(post.canEdit || post.canDelete) && (
                                                        <div className="mt-6 pt-3 flex justify-end gap-3 border-t border-gray-50">
                                                            {post.canEdit && (
                                                                <button
                                                                    onClick={() => startEditing(post)}
                                                                    className="text-xs font-medium text-gray-500 hover:text-blue-600 transition"
                                                                >
                                                                    Edytuj
                                                                </button>
                                                            )}
                                                            {post.canDelete && (
                                                                <button
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
                        )}
                    </div>

                    {totalPages > 1 && (
                        <div className="flex justify-center items-center space-x-4 mt-6">
                            <button
                                onClick={() => setCurrentPage(prev => prev - 1)}
                                disabled={currentPage === 0}
                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                            >
                                Poprzednia
                            </button>
                            <span className="text-sm text-gray-600 font-medium">Strona {currentPage + 1} z {totalPages}</span>
                            <button
                                onClick={() => setCurrentPage(prev => prev + 1)}
                                disabled={currentPage === totalPages - 1}
                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                            >
                                Następna
                            </button>
                        </div>
                    )}

                    <div className="mt-8 pt-8 border-t border-gray-200">
                        {thread?.isLocked ? (
                            <div className="bg-amber-50 p-6 rounded-lg text-center border border-amber-200">
                                <svg className="w-8 h-8 text-amber-500 mx-auto mb-2" fill="currentColor" viewBox="0 -960 960 960">
                                    <path d="M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm296.5-143.5Q560-327 560-360t-23.5-56.5Q513-440 480-440t-56.5 23.5Q400-393 400-360t23.5 56.5Q447-280 480-280t56.5-23.5ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z"/>
                                </svg>
                                <h3 className="text-lg font-bold text-amber-800">Wątek zablokowany</h3>
                                <p className="text-sm text-amber-700 mt-1">Ten wątek został zamknięty przez moderatora. Nie można dodawać nowych odpowiedzi.</p>
                            </div>
                        ) : (
                            <form onSubmit={handleReplySubmit} className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
                                <h3 className="text-lg font-bold text-gray-900 mb-4">Dodaj odpowiedź</h3>

                                <TextEditor content={replyContent} onChange={setReplyContent} />

                                {replyError && (
                                    <div className="mt-3 bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100">
                                        {replyError}
                                    </div>
                                )}

                                <div className="mt-4 flex justify-end">
                                    <button
                                        type="submit"
                                        disabled={isReplying}
                                        className={`px-8 py-2.5 rounded-md text-sm font-bold text-white shadow-sm transition ${
                                            isReplying ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'
                                        }`}
                                    >
                                        {isReplying ? 'Wysyłanie...' : 'Opublikuj odpowiedź'}
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                </main>

                <Footer />
            </div>
        </ForumGuard>
    );
}