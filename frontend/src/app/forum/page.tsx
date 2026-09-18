"use client";

import { useState, useEffect } from 'react';
import Link from 'next/link';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import { getAuthToken, decodeJwt, isJwtValid } from '@/app/utils/jwt';
import { fetchCategories, deleteCategory, ForumCategoryDto } from '@/app/services/forumService';
import CategoryModal from '@/app/components/CategoryModal';
import ForumGuard from '@/app/components/ForumGuard';
import ConfirmModal from '@/app/components/ConfirmModal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import EmptyState from '@/app/components/EmptyState';

export default function ForumPage() {
    const [categories, setCategories] = useState<ForumCategoryDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [userRole, setUserRole] = useState<string | null>(null);
    const [isCategoryModalOpen, setIsCategoryModalOpen] = useState(false);
    const [editingCategory, setEditingCategory] = useState<ForumCategoryDto | null>(null);

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const closeConfirmModal = () => setModalConfig(prev => ({ ...prev, isOpen: false }));
    const showAlert = (title: string, message: string) => {
        setModalConfig({ isOpen: true, title, message, isAlert: true, onConfirm: closeConfirmModal });
    };

    useEffect(() => {
        const token = getAuthToken();
        if (isJwtValid(token)) {
            const payload = decodeJwt(token!);
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
        } catch (error) {
            setError('Nie udało się pobrać danych forum. Sprawdź połączenie.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteCategory = (id: number) => {

        setModalConfig({
            isOpen: true,
            title: 'Usuń kategorię',
            message: 'Czy na pewno chcesz usunąć tę kategorię? Upewnij się, że nie zawiera ona żadnych wątków.',
            isAlert: false,
            onConfirm: async () => {
                closeConfirmModal();
                try {
                    await deleteCategory(id);
                    loadCategories();
                } catch (error: any) {
                    showAlert('Błąd', error.message || 'Wystąpił błąd podczas usuwania kategorii.');
                }
            }
        });
    };

    const canCreateCategory = userRole === 'ADMINISTRATOR' || userRole === 'MODERATOR';

    return (
        <ForumGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-gray-200 pb-5 mb-8">
                        <div className="flex-1 pr-2">
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
                                className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2 px-4 rounded-md shadow-sm transition shrink-0 whitespace-nowrap"
                            >
                                + Dodaj kategorię
                            </button>
                        )}
                    </div>

                    {isLoading ? (
                        <LoadingState />
                    ) : error ? (
                        <ErrorState message={error} onRetry={() => void loadCategories()} />
                    ) : categories.length === 0 ? (
                        <EmptyState>Brak dostępnych kategorii forum.</EmptyState>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {categories.map(category => (
                                <article key={category.id} className="relative bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md hover:border-blue-300 transition group">
                                    <Link href={`/forum/${category.id}`} className="block p-6 pr-24 h-full">
                                        <h2 className="text-xl font-bold text-blue-700 group-hover:text-blue-800">{category.name}</h2>
                                        {category.description && <p className="text-gray-600 text-sm mt-3">{category.description}</p>}
                                    </Link>

                                    {(category.canEdit || category.canDelete) && (
                                        <div className="absolute top-5 right-5 flex gap-2">
                                            {category.canEdit && (
                                                <button type="button" onClick={() => { setEditingCategory(category); setIsCategoryModalOpen(true); }} aria-label={`Edytuj kategorię ${category.name}`} className="text-gray-400 hover:text-blue-600 p-1 transition-colors">
                                                    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                                                        <path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h357l-80 80H200v560h560v-278l80-80v358q0 33-23.5 56.5T760-120H200Zm280-360ZM360-360v-170l367-367q12-12 27-18t30-6q16 0 30.5 6t26.5 18l56 57q11 12 17 26.5t6 29.5q0 15-5.5 29.5T897-728L530-360H360Zm481-424-56-56 56 56ZM440-440h56l232-232-28-28-29-28-231 231v57Zm260-260-29-28 29 28 28 28-28-28Z"/>
                                                    </svg>
                                                </button>
                                            )}

                                            {category.canDelete && (
                                                <button type="button" onClick={() => handleDeleteCategory(category.id)} aria-label={`Usuń kategorię ${category.name}`} className="text-gray-400 hover:text-red-600 p-1 transition-colors">
                                                    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                                                        <path d="M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z"/>
                                                    </svg>
                                                </button>
                                            )}
                                        </div>
                                    )}
                                </article>
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

                <ConfirmModal
                    isOpen={modalConfig.isOpen}
                    title={modalConfig.title}
                    message={modalConfig.message}
                    isAlert={modalConfig.isAlert}
                    onConfirm={modalConfig.onConfirm}
                    onCancel={closeConfirmModal}
                />
                <Footer />
            </div>
        </ForumGuard>
    );
}