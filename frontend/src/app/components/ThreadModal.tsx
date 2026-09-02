"use client";

import { useState, useEffect } from 'react';
import TextEditor from './TextEditor';
import { getAuthToken } from '@/utils/jwt';

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
        if (isOpen) {
            document.body.style.overflow = 'hidden';
            setTitle('');
            setContent('');
            setError('');
        } else {
            document.body.style.overflow = 'unset';
        }
        return () => { document.body.style.overflow = 'unset'; };
    }, [isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (title.length < 5 || title.length > 150) {
            setError('Tytuł wątku musi mieć od 5 do 150 znaków.');
            return;
        }
        if (!content || content === '<p></p>') {
            setError('Treść wiadomości nie może być pusta.');
            return;
        }

        setIsLoading(true);
        const token = getAuthToken();

        try {
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/forum/topics`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    categoryId: categoryId,
                    title: title,
                    initialPostContent: content
                }),
            });

            if (response.ok) {
                onSuccess();
                onClose();
            } else {
                const errorData = await response.text();
                setError(errorData || 'Wystąpił błąd podczas tworzenia wątku.');
            }
        } catch (err) {
            setError('Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 p-4 animate-fadeIn backdrop-blur-sm">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
                <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50">
                    <h2 className="text-xl font-bold text-gray-800">Utwórz nowy wątek</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-6">
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">Tytuł wątku</label>
                            <input
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all"
                                placeholder="Jasno opisz swój problem lub myśl..."
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">Treść pierwszej wiadomości</label>
                            <TextEditor content={content} onChange={setContent} />
                        </div>

                        {error && (
                            <div className="bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100">
                                {error}
                            </div>
                        )}
                    </div>

                    <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
                        <button type="button" onClick={onClose} disabled={isLoading} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition">
                            Anuluj
                        </button>
                        <button type="submit" disabled={isLoading} className={`px-6 py-2 rounded-md text-sm font-bold text-white shadow-sm transition ${isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'}`}>
                            {isLoading ? 'Tworzenie...' : 'Opublikuj wątek'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}