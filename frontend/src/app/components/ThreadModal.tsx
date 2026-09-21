"use client";

import { useState, useEffect, type FormEvent } from 'react';
import TextEditor from './TextEditor';
import Modal from '@/app/components/Modal';
import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { isHtmlEmpty } from '@/app/utils/richText';

interface ThreadModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    categoryId: number;
}

export default function ThreadModal({ isOpen, onClose, onSuccess, categoryId }: ThreadModalProps) {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        setTitle('');
        setContent('');
        setError('');
    }, [isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        const cleanTitle = title.trim();
        if (cleanTitle.length < 5 || cleanTitle.length > 150) {
            setError('Tytuł wątku musi mieć od 5 do 150 znaków.');
            return;
        }

        if (isHtmlEmpty(content)) {
            setError('Treść pierwszej wiadomości nie może być pusta.');
            return;
        }

        setIsLoading(true);

        try {
            const response = await fetchWithAuth(`${API_URL}/api/forum/threads`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ categoryId, title: cleanTitle, initialPostContent: content })
            });

            if (!response.ok) {
                setError(await readApiError(response, 'Wystąpił błąd podczas tworzenia wątku.'));
                return;
            }

            onSuccess();
            onClose();
        } catch (error) {
            setError(
                error instanceof TypeError
                    ? 'Nie udało się połączyć z serwerem.'
                    : error instanceof Error
                        ? error.message
                        : 'Wystąpił błąd podczas tworzenia wątku.'
            );
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Modal
            isOpen={isOpen}
            title="Utwórz nowy wątek"
            onClose={onClose}
            closeDisabled={isLoading}
            maxWidthClass="max-w-4xl"
        >
            <form onSubmit={handleSubmit} className="p-6 space-y-6">
                <div className="space-y-4">
                    <div>
                        <label htmlFor="thread-title" className="block text-sm font-bold text-gray-700 mb-1">
                            Tytuł wątku
                        </label>
                        <input
                            id="thread-title"
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            minLength={5}
                            maxLength={150}
                            disabled={isLoading}
                            className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all disabled:opacity-50"
                            placeholder="Jasno opisz swój problem lub myśl..."
                            required
                        />
                    </div>
                     <div>
                         <p className="block text-sm font-bold text-gray-700 mb-1">
                             Treść pierwszej wiadomości
                         </p>
                         <TextEditor content={content} onChange={setContent} ariaLabel="Treść pierwszej wiadomości" />
                     </div>

                    {error && (
                        <div
                            role="alert"
                            className="bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100"
                        >
                            {error}
                        </div>
                    )}
                </div>

                <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isLoading}
                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
                    >
                        Anuluj
                    </button>
                    <button
                        type="submit"
                        disabled={isLoading}
                        className={`px-6 py-2 rounded-md text-sm font-bold text-white shadow-sm transition 
                        ${isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'}`}
                    >
                        {isLoading ? 'Tworzenie...' : 'Opublikuj wątek'}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
