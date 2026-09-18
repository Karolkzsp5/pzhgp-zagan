"use client";

import Modal from '@/app/components/Modal';

interface ConfirmModalProps {
    isOpen: boolean;
    title: string;
    message: string;
    onConfirm: () => void;
    onCancel: () => void;
    isAlert?: boolean;
    variant?: 'primary' | 'danger';
    confirmLabel?: string;
    isLoading?: boolean;
    error?: string | null;
}

export default function ConfirmModal({isOpen, title, message, onConfirm, onCancel, isAlert = false, variant = 'danger', confirmLabel, isLoading = false, error = null}: ConfirmModalProps) {
    const buttonClass = variant === 'danger' ? 'bg-red-600 hover:bg-red-700' : 'bg-blue-600 hover:bg-blue-700';

    return (
        <Modal isOpen={isOpen} title={title} onClose={onCancel} closeDisabled={isLoading}>
            <div className="p-6">
                <p className="text-sm text-gray-600">{message}</p>

                {error && (
                    <div className="mt-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md p-3">
                        {error}
                    </div>
                )}
            </div>

            <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex justify-end gap-3">
                {!isAlert && (
                    <button type="button" onClick={onCancel} disabled={isLoading}
                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                    >
                        Anuluj
                    </button>
                )}

                <button type="button" onClick={onConfirm} disabled={isLoading}
                        className={`px-5 py-2 rounded-md text-sm font-bold text-white transition disabled:opacity-50 ${
                        isAlert ? 'bg-blue-600 hover:bg-blue-700' : buttonClass
                    }`}
                >
                    {isLoading ? 'Proszę czekać...' : confirmLabel ?? (isAlert ? 'OK' : 'Potwierdź')}
                </button>
            </div>
        </Modal>
    );
}