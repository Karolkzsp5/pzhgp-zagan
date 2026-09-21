"use client";

import { useState, useEffect } from 'react';
import { ForumCategoryDto, createCategory, updateCategory } from '@/app/services/forumService';
import Modal from '@/app/components/Modal';

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
    }, [categoryToEdit, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        const cleanName = name.trim();
        if (cleanName.length < 3 || cleanName.length > 100) {
            setError('Nazwa kategorii musi mieć od 3 do 100 znaków.');
            return;
        }

        const finalSortOrder = Number(sortOrder);
        if (!Number.isInteger(finalSortOrder) || finalSortOrder < 1) {
            setError('Kolejność wyświetlania musi być dodatnią liczbą całkowitą (min. 1).');
            return;
        }

        setIsLoading(true);

        try {
            if (categoryToEdit) {
                await updateCategory(categoryToEdit.id, cleanName, description.trim(), finalSortOrder);
            } else {
                await createCategory(cleanName, description.trim(), finalSortOrder);
            }
            onSuccess();
            onClose();
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Wystąpił błąd podczas zapisywania kategorii.'
            );
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            title={categoryToEdit ? 'Edytuj kategorię' : 'Dodaj nową kategorię'}
            onClose={onClose}
            closeDisabled={isLoading}
            maxWidthClass="max-w-lg"
        >
            <form onSubmit={handleSubmit} className="p-6 space-y-5">
                <div className="space-y-4">
                    <div>
                        <label htmlFor="category-name" className="block text-sm font-bold text-gray-700 mb-1">
                            Nazwa kategorii
                        </label>
                        <input
                            id="category-name"
                            type="text"
                            value={name}
                            minLength={3}
                            maxLength={100}
                            disabled={isLoading}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all"
                            placeholder="np. Wystawy i Loty"
                            required
                        />
                    </div>

                    <div>
                        <label htmlFor="category-description" className="block text-sm font-bold text-gray-700 mb-1">
                            Opis kategorii <span className="text-gray-400 font-normal">(Opcjonalny)</span>
                        </label>
                        <textarea
                            id="category-description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            rows={3}
                            disabled={isLoading}
                            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all resize-none disabled:opacity-50"
                            placeholder="Krótki opis tego, o czym dyskutuje się w tym dziale..."
                        />
                    </div>

                    <div>
                        <label htmlFor="category-sort-order" className="block text-sm font-bold text-gray-700 mb-1">
                            Kolejność wyświetlania
                        </label>
                        <input
                            id="category-sort-order"
                            type="number"
                            value={sortOrder}
                            onChange={(e) => setSortOrder(e.target.value === '' ? '' : Number(e.target.value))}
                            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all disabled:opacity-50"
                            placeholder="1"
                            min={1}
                            step={1}
                            disabled={isLoading}
                            required
                        />
                        <p className="mt-1 text-xs text-gray-500">
                            Mniejsza liczba oznacza wyższą pozycję kategorii na liście.
                        </p>
                    </div>

                    {error && (
                        <div role="alert" className="bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100">
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
        </Modal>
    );
}