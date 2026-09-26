"use client";

import { ReactNode, useEffect, useId, useRef } from 'react';

interface ModalProps {
    isOpen: boolean;
    title: string;
    children: ReactNode;
    onClose: () => void;
    closeDisabled?: boolean;
    maxWidthClass?: string;
}

export default function Modal({ isOpen, title, children, onClose, closeDisabled = false, maxWidthClass = 'max-w-md' }: ModalProps) {
    const titleId = useId();
    const panelRef = useRef<HTMLDivElement>(null);
    const onCloseRef = useRef(onClose);
    const closeDisabledRef = useRef(closeDisabled);

    useEffect(() => {
        onCloseRef.current = onClose;
    }, [onClose]);

    useEffect(() => {
        closeDisabledRef.current = closeDisabled;
    }, [closeDisabled]);

    useEffect(() => {
        if (!isOpen) return;

        const previousActiveElement = document.activeElement as HTMLElement | null;
        const previousOverflow = document.body.style.overflow;
        const focusableSelector = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])';
        const focusableElements = () => Array.from(panelRef.current?.querySelectorAll<HTMLElement>(focusableSelector) ?? []);

        document.body.style.overflow = 'hidden';

        const frameId = requestAnimationFrame(() => {
            const elements = focusableElements();
            if (elements.length > 0) elements[0].focus();
            else panelRef.current?.focus();
        });

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && !closeDisabledRef.current) {
                onCloseRef.current();
                return;
            }

            if (event.key !== 'Tab') return;

            const elements = focusableElements();

            if (elements.length === 0) {
                event.preventDefault();
                panelRef.current?.focus();
                return;
            }

            const first = elements[0];
            const last = elements[elements.length - 1];

            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        };

        document.addEventListener('keydown', handleKeyDown);

        return () => {
            cancelAnimationFrame(frameId);
            document.removeEventListener('keydown', handleKeyDown);
            document.body.style.overflow = previousOverflow;
            previousActiveElement?.focus();
        };
    }, [isOpen]);

    if (!isOpen) return null;

    return (
        <div
            className="fixed inset-0 z-[60] flex items-center justify-center bg-gray-900/50 backdrop-blur-sm p-4"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !closeDisabled) onClose();
            }}
        >
            <div ref={panelRef} tabIndex={-1} role="dialog" aria-modal="true" aria-labelledby={titleId}
                 className={`bg-white rounded-lg shadow-xl w-full ${maxWidthClass} max-h-[90vh] overflow-y-auto focus:outline-none`}
            >
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <h2 id={titleId} className="text-xl font-bold text-gray-900">{title}</h2>
                    <button type="button" onClick={onClose} disabled={closeDisabled} aria-label="Zamknij okno"
                            className="p-1 text-gray-400 hover:text-gray-700 rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 disabled:opacity-50">
                        <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                            <path d="m256-200-56-56 224-224-224-224 56-56 224 224 224-224 56 56-224 224 224 224-56 56-224-224-224 224Z" />
                        </svg>
                    </button>
                </div>
                {children}
            </div>
        </div>
    );
}
