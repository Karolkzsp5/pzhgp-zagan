"use client";

import { useState, useEffect } from 'react';
import Link from 'next/link';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { getAuthToken, decodeJwt } from '@/utils/jwt';
import { fetchCategories, ForumCategoryDto } from '../services/forumService';
import CategoryModal from '../components/CategoryModal';
import ForumGuard from '../components/ForumGuard';

export default function ForumPage() {
    const [categories, setCategories] = useState<ForumCategoryDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [userRole, setUserRole] = useState<string | null>(null);
    const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
    const [editingCategory, setEditingCategory] = useState<ForumCategoryDto | null>(null);

    useEffect(() => {
        const token = getAuthToken();
        if (token) {
            const payload = decodeJwt(token);
            if (payload) {
                setUserRole(payload.role || null);
            }
        }

        loadCategories();
    }, []);

    const loadCategories = async () => {
        setIsLoading(true);
        try {
            const data = await fetchCategories();
            setCategories(data);
        } catch (err) {
            setError('Nie udało się pobrać danych forum. Sprawdź połączenie.');
        } finally {
            setIsLoading(false);
        }
    };

    const canManageCategories = userRole === 'ADMINISTRATOR' || userRole === 'MODERATOR';

    return (
        <ForumGuard>
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />

            <main className="flex-grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                <div className="flex items-center justify-between border-b border-gray-200 pb-5 mb-8">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Forum Hodowców</h1>
                        <p className="mt-2 text-sm text-gray-600">
                            Wybierz kategorię, aby przeglądać tematy lub rozpocząć nową dyskusję.
                        </p>
                    </div>

                    {canManageCategories && (
                        <button
                            onClick={() => {
                                setEditingCategory(null);
                                setIsCategoryModalOpen(true);
                            }}
                            className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2 px-4 rounded-md shadow-sm transition"
                        >
                            + Dodaj kategorię
                        </button>
                    )}
                </div>

                {error && (
                    <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded mb-6">
                        <p className="text-sm text-red-700">{error}</p>
                    </div>
                )}

                {isLoading ? (
                    <div className="flex justify-center items-center py-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
                    </div>
                ) : categories.length === 0 && !error ? (
                    <div className="text-center py-16 bg-white rounded-lg shadow-sm border border-gray-100 text-gray-500">
                        Brak dostępnych kategorii forum.
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {categories.map((category) => (
                            <Link
                                href={`/forum/${category.id}`}
                                key={category.id}
                                className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 hover:shadow-md hover:border-blue-300 transition group flex flex-col h-full"
                            >
                                <div className="flex items-start justify-between mb-3">
                                    <h2 className="text-xl font-bold text-blue-700 group-hover:text-blue-800">
                                        {category.name}
                                    </h2>
                                    <div className="flex gap-2 items-center">
                                        {canManageCategories && (
                                            <button
                                                onClick={(e) => {
                                                    e.preventDefault();
                                                    setEditingCategory(category);
                                                    setIsCategoryModalOpen(true);
                                                }}
                                                className="text-gray-400 hover:text-blue-600 p-1 transition-colors"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                </svg>
                                            </button>
                                        )}
                                        <svg className="w-5 h-5 text-gray-400 group-hover:text-blue-500 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
                                        </svg>
                                    </div>
                                </div>
                                <p className="text-gray-600 text-sm flex-grow">
                                    {category.description || "Brak opisu kategorii."}
                                </p>
                            </Link>
                        ))}
                    </div>
                )}
            </main>
            <CategoryModal
                isOpen={isCategoryModalOpen}
                onClose={() => {
                    setIsCategoryModalOpen(false);
                    setEditingCategory(null);
                }}
                onSuccess={() => loadCategories()}
                categoryToEdit={editingCategory}
            />
            <Footer />
        </div>
        </ForumGuard>
    );
}