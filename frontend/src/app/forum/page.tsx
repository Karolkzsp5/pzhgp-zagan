"use client";

import { useState, useEffect } from 'react';
import Link from 'next/link';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import { getAuthToken, decodeJwt } from '@/utils/jwt';
import { fetchCategories, deleteCategory, ForumCategoryDto } from '@/app/services/forumService';
import CategoryModal from '@/app/components/CategoryModal';
import ForumGuard from '@/app/components/ForumGuard';

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

    const handleDeleteCategory = async (id: number, e: React.MouseEvent) => {
        e.preventDefault();
        if (!window.confirm('Czy na pewno chcesz usunąć tę kategorię? Nie może zawierać wątków.')) return;

        try {
            await deleteCategory(id);
            loadCategories();
        } catch (err: any) {
            alert(err.message || 'Wystąpił błąd podczas usuwania kategorii.');
        }
    };

    const canCreateCategory = userRole === 'ADMINISTRATOR' || userRole === 'MODERATOR';

    return (
        <ForumGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between border-b border-gray-200 pb-5 mb-8">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Forum Hodowców</h1>
                            <p className="mt-2 text-sm text-gray-600">
                                Wybierz kategorię, aby przeglądać tematy lub rozpocząć nową dyskusję.
                            </p>
                        </div>

                        {canCreateCategory && (
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

                                        {(category.canEdit || category.canDelete) && (
                                            <div className="flex gap-2 items-center">
                                                {category.canEdit && (
                                                    <button
                                                        onClick={(e) => {
                                                            e.preventDefault();
                                                            setEditingCategory(category);
                                                            setIsCategoryModalOpen(true);
                                                        }}
                                                        className="text-gray-400 hover:text-blue-600 p-1 transition-colors"
                                                        title="Edytuj kategorię"
                                                    >
                                                        <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                                                            <path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h357l-80 80H200v560h560v-278l80-80v358q0 33-23.5 56.5T760-120H200Zm280-360ZM360-360v-170l367-367q12-12 27-18t30-6q16 0 30.5 6t26.5 18l56 57q11 12 17 26.5t6 29.5q0 15-5.5 29.5T897-728L530-360H360Zm481-424-56-56 56 56ZM440-440h56l232-232-28-28-29-28-231 231v57Zm260-260-29-28 29 28 28 28-28-28Z"/>
                                                        </svg>
                                                    </button>
                                                )}
                                                {category.canDelete && (
                                                    <button
                                                        onClick={(e) => handleDeleteCategory(category.id, e)}
                                                        className="text-gray-400 hover:text-red-600 p-1 transition-colors"
                                                        title="Usuń kategorię"
                                                    >
                                                        <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                                                            <path d="M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z"/>
                                                        </svg>
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>

                                    {category.description && (
                                        <p className="text-gray-600 text-sm grow">
                                            {category.description}
                                        </p>
                                    )}
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