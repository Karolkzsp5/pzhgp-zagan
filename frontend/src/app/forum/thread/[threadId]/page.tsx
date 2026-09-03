"use client";

import { useState, useEffect, use } from 'react';
import Link from 'next/link';
import DOMPurify from 'dompurify';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import TextEditor from '@/app/components/TextEditor';
import ForumGuard from '@/app/components/ForumGuard';
import { fetchThreadById, fetchPostsByThread, createPost, ForumThreadDto, ForumPostDto } from '@/app/services/forumService';

export default function ThreadViewPage({ params }: { params: Promise<{ threadId: string }> }) {
    const resolvedParams = use(params);
    const threadId = parseInt(resolvedParams.threadId);

    const [thread, setThread] = useState<(ForumThreadDto & { categoryId?: number; categoryName?: string }) | null>(null);
    const [posts, setPosts] = useState<ForumPostDto[]>([]);

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');

    const [replyContent, setReplyContent] = useState('');
    const [isReplying, setIsReplying] = useState(false);
    const [replyError, setReplyError] = useState('');

    useEffect(() => {
        loadThreadData();
    }, [threadId, currentPage]);

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
                        <div className="bg-white p-6 rounded-t-lg shadow-sm border border-gray-200 border-b-0 flex items-center justify-between">
                            <div>
                                <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
                                    {thread.isPinned && (
                                        <svg className="w-5 h-5 text-blue-500 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                            <path d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" />
                                        </svg>
                                    )}
                                    {thread.isLocked && (
                                        <svg className="w-5 h-5 text-red-500 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                            <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                                        </svg>
                                    )}
                                    {thread.title}
                                </h1>
                                <div className="mt-2 text-sm text-gray-500">
                                    Rozpoczęte przez <span className="font-semibold text-gray-700">{thread.authorName}</span>, {formatDate(thread.createdAt)}
                                </div>
                            </div>
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
                                            <div className="font-bold text-gray-900 wrap-break-word">{post.authorName}</div>
                                            <div className="text-xs text-gray-500 mt-1">
                                                Użytkownik
                                            </div>
                                        </div>

                                        <div className="p-4 sm:p-6 grow flex flex-col min-w-0">
                                            <div className="text-xs text-gray-400 mb-4 pb-2 border-b border-gray-100">
                                                Napisano: {formatDate(post.createdAt)}
                                            </div>

                                            <div
                                                className="prose prose-sm sm:prose-base max-w-none text-gray-800 wrap-break-wgrowgrow"
                                                dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(post.body) }}
                                            />

                                            {(post.canEdit || post.canDelete) && (
                                                <div className="mt-6 pt-3 flex justify-end gap-3 border-t border-gray-50">
                                                    {post.canEdit && <button className="text-xs font-medium text-gray-500 hover:text-blue-600 transition">Edytuj</button>}
                                                    {post.canDelete && <button className="text-xs font-medium text-gray-500 hover:text-red-600 transition">Usuń</button>}
                                                </div>
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
                                <svg className="w-8 h-8 text-amber-500 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
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