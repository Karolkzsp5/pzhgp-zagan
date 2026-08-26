"use client";

import { useState, useEffect } from 'react';
import TextEditor from './TextEditor';
import { getAuthToken } from '@/utils/jwt';

interface Announcement {
    id: number;
    title: string;
    content: string;
    isPinned: boolean;
}

interface AnnouncementModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    announcementToEdit?: Announcement | null;
}

export default function AnnouncementModal({ isOpen, onClose, onSuccess, announcementToEdit }: AnnouncementModalProps) {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [isPinned, setIsPinned] = useState(false);

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (announcementToEdit) {
            setTitle(announcementToEdit.title);
            setContent(announcementToEdit.content);
            setIsPinned(announcementToEdit.isPinned);
        } else {
            setTitle('');
            setContent('');
            setIsPinned(false);
        }
        setError('');
    }, [announcementToEdit, isOpen]);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (title.length < 3 || title.length > 150) {
            setError('Tytuł musi mieć od 3 do 150 znaków.');
            return;
        }
        if (!content || content === '<p></p>') {
            setError('Treść ogłoszenia nie może być pusta.');
            return;
        }

        setIsLoading(true);
        const token = getAuthToken();

        const url = announcementToEdit
            ? `${process.env.NEXT_PUBLIC_API_URL}/api/announcements/${announcementToEdit.id}`
            : `${process.env.NEXT_PUBLIC_API_URL}/api/announcements`;

        const method = announcementToEdit ? 'PUT' : 'POST';

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ title, content, isPinned }),
            });

            if (response.ok) {
                onSuccess();
                onClose();
            } else {
                const errorData = await response.text();
                setError(errorData || 'Wystąpił błąd podczas zapisywania ogłoszenia.');
            }
        } catch (err) {
            setError('Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-50/50 bg-opacity-50 animate-fadeIn p-4">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
                <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50 rounded-t-lg">
                    <h2 className="text-xl font-bold text-gray-800">
                        {announcementToEdit ? 'Edytuj ogłoszenie' : 'Dodaj nowe ogłoszenie'}
                    </h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-6">=
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">
                                Tytuł ogłoszenia *
                            </label>
                            <input
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                                placeholder="Wpisz tytuł (min. 3 znaki)"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-bold text-gray-700 mb-1">
                                Treść ogłoszenia *
                            </label>
                            <TextEditor content={content} onChange={setContent} />
                        </div>

                        <div className="flex items-center mt-2">
                            <input
                                type="checkbox"
                                id="isPinned"
                                checked={isPinned}
                                onChange={(e) => setIsPinned(e.target.checked)}
                                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                            />
                            <label htmlFor="isPinned" className="ml-2 block text-sm font-medium text-gray-700">
                                Przypnij ogłoszenie
                            </label>
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
                            {isLoading ? 'Zapisywanie...' : (announcementToEdit ? 'Zapisz zmiany' : 'Opublikuj')}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}