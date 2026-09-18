"use client";

import { useEffect, useState } from 'react';
import TextEditor from './TextEditor';
import Modal from './Modal';
import { API_URL, fetchWithAuth, readApiError } from '@/app/utils/apiClient';
import { isHtmlEmpty } from '@/app/utils/richText';

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
        if (!isOpen) return;

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

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        const cleanTitle = title.trim();

        if (cleanTitle.length < 3 || cleanTitle.length > 150) {
            setError('Tytuł musi mieć od 3 do 150 znaków.');
            return;
        }

        if (isHtmlEmpty(content)) {
            setError('Treść ogłoszenia nie może być pusta.');
            return;
        }

        setIsLoading(true);

        try {
            const response = await fetchWithAuth(announcementToEdit ? `${API_URL}/api/announcements/${announcementToEdit.id}` : `${API_URL}/api/announcements`, {
                method: announcementToEdit ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: cleanTitle, content, isPinned })
            });

            if (!response.ok) {
                setError(await readApiError(response, 'Wystąpił błąd podczas zapisywania ogłoszenia.'));
                return;
            }

            onSuccess();
            onClose();
        } catch (error) {
            setError(error instanceof Error ? error.message : 'Błąd połączenia z serwerem.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <Modal isOpen={isOpen} title={announcementToEdit ? 'Edytuj ogłoszenie' : 'Dodaj nowe ogłoszenie'} onClose={onClose} closeDisabled={isLoading} maxWidthClass="max-w-4xl">
            <form onSubmit={handleSubmit} className="p-6 space-y-6">
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">Tytuł ogłoszenia</label>
                        <input type="text" value={title} onChange={(e) => setTitle(e.target.value)}
                               className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-gray-900 focus:outline-none transition-all"
                               placeholder="Wpisz tytuł (min. 3 znaki)" required />
                    </div>

                    <div>
                        <label className="block text-sm font-bold text-gray-700 mb-1">Treść ogłoszenia</label>
                        <TextEditor content={content} onChange={setContent} ariaLabel="Treść ogłoszenia" />
                    </div>

                    <div className="flex items-center mt-2">
                        <input type="checkbox" id="isPinned" checked={isPinned} onChange={(e) => setIsPinned(e.target.checked)}
                               className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"/>
                        <label htmlFor="isPinned" className="ml-2 block text-sm font-medium text-gray-700">Przypnij ogłoszenie</label>
                    </div>

                    {error && <div className="bg-red-50 text-red-600 p-3 rounded-md text-sm font-medium border border-red-100">{error}</div>}
                </div>

                <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
                    <button type="button" onClick={onClose} disabled={isLoading}
                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50">
                        Anuluj
                    </button>
                    <button type="submit" disabled={isLoading}
                            className={`px-6 py-2 rounded-md text-sm font-bold text-white shadow-sm transition ${isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700'}`}>
                            {isLoading ? 'Zapisywanie...' : announcementToEdit ? 'Zapisz zmiany' : 'Opublikuj'}
                    </button>
                </div>
            </form>
        </Modal>
    );
}
