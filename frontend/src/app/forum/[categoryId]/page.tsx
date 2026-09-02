"use client";

import { useState, useEffect, use } from 'react';
import Link from 'next/link';
import Navbar from '../../components/Navbar';
import Footer from '../../components/Footer';
import ThreadModal from '../../components/ThreadModal';
import ForumGuard from '../../components/ForumGuard';
import { fetchCategoryById, fetchThreadsByCategory, ForumCategoryDto, ForumThreadDto } from '../../services/forumService';

export default function CategoryViewPage({ params }: { params: Promise<{ categoryId: string }> }) {
    const resolvedParams = use(params);
    const categoryId = parseInt(resolvedParams.categoryId);

    const [category, setCategory] = useState<ForumCategoryDto | null>(null);
    const [threads, setThreads] = useState<ForumThreadDto[]>([]);

    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [isThreadModalOpen, setIsThreadModalOpen] = useState(false);

    useEffect(() => {
        loadData();
    }, [categoryId, currentPage]);

    const loadData = async () => {
        setIsLoading(true);
        setError('');
        try {
            if (!category) {
                const catData = await fetchCategoryById(categoryId);
                setCategory(catData);
            }

            const pageData = await fetchThreadsByCategory(categoryId, currentPage);
            setThreads(pageData.content);
            setTotalPages(pageData.totalPages);

        } catch (err) {
            setError('Nie udało się pobrać danych. Sprawdź połączenie z serwerem.');
        } finally {
            setIsLoading(false);
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

                <main className="flex-grow max-w-7xl mx-auto w-full py-8 px-4 sm:px-6 lg:px-8">
                    <nav className="flex text-sm text-gray-500 mb-6 font-medium">
                        <Link href="/forum" className="hover:text-blue-600 transition">Forum</Link>
                        <span className="mx-2">/</span>
                        <span className="text-gray-800">{category?.name || 'Ładowanie...'}</span>
                    </nav>

                    <div className="flex flex-col sm:flex-row sm:items-center justify-between bg-white p-6 rounded-t-lg shadow-sm border border-gray-200 border-b-0">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">{category?.name}</h1>
                            <p className="mt-1 text-sm text-gray-500">{category?.description}</p>
                        </div>

                        <button
                            onClick={() => setIsThreadModalOpen(true)}
                            className="mt-4 sm:mt-0 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2.5 px-5 rounded-md shadow-sm transition whitespace-nowrap"
                        >
                            + Nowy wątek
                        </button>
                    </div>

                    {error && (
                        <div className="bg-red-50 border-l-4 border-red-400 p-4 shadow-sm border border-gray-200 border-t-0">
                            <p className="text-sm text-red-700">{error}</p>
                        </div>
                    )}

                    <div className="bg-white shadow-sm border border-gray-200 rounded-b-lg overflow-hidden">
                        {isLoading ? (
                            <div className="flex justify-center items-center py-20">
                                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
                            </div>
                        ) : threads.length === 0 ? (
                            <div className="text-center py-16 text-gray-500">
                                Brak wątków w tym dziale. Bądź pierwszy i rozpocznij dyskusję!
                            </div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200">
                                    <thead className="bg-gray-50">
                                    <tr>
                                        <th scope="col" className="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase tracking-wider w-1/2">
                                            Wątek
                                        </th>
                                        <th scope="col" className="px-6 py-3 text-center text-xs font-bold text-gray-500 uppercase tracking-wider">
                                            Statystyki
                                        </th>
                                        <th scope="col" className="px-6 py-3 text-right text-xs font-bold text-gray-500 uppercase tracking-wider">
                                            Ostatni wpis
                                        </th>
                                    </tr>
                                    </thead>
                                    <tbody className="bg-white divide-y divide-gray-200">
                                    {threads.map((thread) => (
                                        <tr key={thread.id} className="hover:bg-blue-50/50 transition group">
                                            <td className="px-6 py-4">
                                                <div className="flex items-center">
                                                    {thread.isPinned && (
                                                        <svg className="w-4 h-4 text-blue-500 mr-2 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                                            <path d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" />
                                                        </svg>
                                                    )}
                                                    {thread.isLocked && (
                                                        <svg className="w-4 h-4 text-red-500 mr-2 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                                                            <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                                                        </svg>
                                                    )}
                                                    <div>
                                                        <Link href={`/forum/thread/${thread.id}`} className="text-base font-bold text-blue-700 hover:underline">
                                                            {thread.title}
                                                        </Link>
                                                        <div className="text-xs text-gray-500 mt-1">
                                                            Autor: <span className="font-medium text-gray-700">{thread.authorName}</span> • {formatDate(thread.createdAt)}
                                                        </div>
                                                    </div>
                                                </div>
                                            </td>
                                            <td className="px-6 py-4 text-center whitespace-nowrap">
                                                <div className="text-sm text-gray-900 font-medium">{thread.repliesCount} odp.</div>
                                                <div className="text-xs text-gray-500">{thread.views} wyśw.</div>
                                            </td>
                                            <td className="px-6 py-4 text-right whitespace-nowrap">
                                                <div className="text-sm text-gray-900 font-medium">
                                                    {formatDate(thread.lastPostAt)}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>

                    {totalPages > 1 && (
                        <div className="flex justify-center items-center space-x-4 mt-8">
                            <button
                                onClick={() => setCurrentPage(prev => prev - 1)}
                                disabled={currentPage === 0}
                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                            >
                                Poprzednia
                            </button>
                            <span className="text-sm text-gray-600 font-medium">
                                Strona {currentPage + 1} z {totalPages}
                            </span>
                            <button
                                onClick={() => setCurrentPage(prev => prev + 1)}
                                disabled={currentPage === totalPages - 1}
                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
                            >
                                Następna
                            </button>
                        </div>
                    )}
                </main>

                <Footer />

                <ThreadModal
                    isOpen={isThreadModalOpen}
                    onClose={() => setIsThreadModalOpen(false)}
                    onSuccess={() => {
                        setCurrentPage(0);
                        loadData();
                    }}
                    categoryId={categoryId}
                />
            </div>
        </ForumGuard>
    );
}