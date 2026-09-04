"use client";

import { useState, useEffect } from 'react';
import { getAuthToken } from '@/utils/jwt';
import { ForumCategoryDto } from '@/app/services/forumService';

interface CategoryModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    categoryToEdit?: ForumCategoryDto | null;
}

export default function CategoryModal({ isOpen, onClose, onSuccess, categoryToEdit }: CategoryModalProps) {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [sortOrder, setSortOrder] = useState<number | string>(1);

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (isOpen) {
            document.body.style.overflow = 'hidden';
        } else {
            document.body.style.overflow = 'unset';
        }

        if (categoryToEdit) {
            setName(categoryToEdit.name);
            setDescription(categoryToEdit.description || '');
            setSortOrder(categoryToEdit.sortOrder);
        } else {
            setName('');
            setDescription('');
            setSortOrder(1);
        }
        setError('');

        return () => {
            document.body.style.overflow = 'unset';
        };
    }, [categoryToEdit, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (name.length < 3 || name.length > 100) {
            setError('Nazwa kategorii musi mieć od 3 do 100 znaków.');
            return;
        }

        setIsLoading(true);
        const token = getAuthToken();

        const url = categoryToEdit
            ? `${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories/${categoryToEdit.id}`
            : `${process.env.NEXT_PUBLIC_API_URL}/api/forum/categories`;

        const method = categoryToEdit ? 'PUT' : 'POST';
        const finalSortOrder = sortOrder === '' ? 1 : Number(sortOrder);

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ name, description, sortOrder: finalSortOrder }),
            });

            if (response.ok) {
                onSuccess();
                onClose();
            } else {
                const errorData = await response.text();
                setError(errorData || 'Wystąpił błąd podczas zapisywania kategorii.');
            }
        } catch (err) {
            setError('Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 p-4 animate-fadeIn backdrop-blur-sm">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-lg overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50">
                    <h2 className="text-xl font-bold text-gray-800">
                        {categoryToEdit ? 'Edytuj kategorię' : 'Dodaj nową kategorię'}
                    </h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-5">
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">
                                Nazwa kategorii
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all"
                                placeholder="np. Wystawy i Loty"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">
                                Opis kategorii <span className="text-gray-400 font-normal">(Opcjonalny)</span>
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                rows={3}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all resize-none"
                                placeholder="Krótki opis tego, o czym dyskutuje się w tym dziale..."
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">
                                Kolejność sortowania (Sort Order)
                            </label>
                            <input
                                type="number"
                                value={sortOrder}
                                onChange={(e) => setSortOrder(e.target.value === '' ? '' : Number(e.target.value))}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all"
                                placeholder="1"
                                min="1"
                                required
                            />
                            <p className="text-xs text-gray-500 mt-1">
                                Kategorie z mniejszą liczbą będą wyświetlać się wyżej na liście.
                            </p>
                        </div>

                        {error && (
                            <div className="bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100">
                                {error}
                            </div>
                        )}
                    </div>

                    <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition"
                            disabled={isLoading}
                        >
                            Anuluj
                        </button>
                        <button
                            type="submit"
                            disabled={isLoading}
                            className={`px-6 py-2 rounded-md text-sm font-bold text-white shadow-sm transition ${
                                isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'
                            }`}
                        >
                            {isLoading ? 'Zapisywanie...' : (categoryToEdit ? 'Zapisz zmiany' : 'Dodaj kategorię')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}