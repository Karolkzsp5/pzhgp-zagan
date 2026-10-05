"use client";

import { ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

interface ActionMenuProps {
    isOpen: boolean;
    onToggle: () => void;
    onClose: () => void;
    ariaLabel: string;
    children: ReactNode;
    buttonDataCy?: string;
    menuDataCy?: string;
}

interface MenuPosition {
    top: number;
    left: number;
}

export default function ActionMenu({isOpen, onToggle, onClose, ariaLabel, children, buttonDataCy, menuDataCy}: ActionMenuProps) {
    const buttonRef = useRef<HTMLButtonElement>(null);
    const menuRef = useRef<HTMLDivElement>(null);

    const [position, setPosition] = useState<MenuPosition | null>(null);

    const updatePosition = useCallback(() => {
        const button = buttonRef.current;
        const menu = menuRef.current;

        if (!button || !menu) return;

        const buttonRect = button.getBoundingClientRect();
        const menuWidth = menu.offsetWidth;
        const menuHeight = menu.offsetHeight;

        const gap = 4;
        const viewportPadding = 8;

        const availableBelow =
            window.innerHeight - buttonRect.bottom;

        const shouldOpenAbove =
            availableBelow < menuHeight + gap + viewportPadding &&
            buttonRect.top >= menuHeight + gap + viewportPadding;

        const top = shouldOpenAbove
            ? buttonRect.top - menuHeight - gap
            : buttonRect.bottom + gap;

        const preferredLeft = buttonRect.right - menuWidth;

        const left = Math.min(
            Math.max(viewportPadding, preferredLeft),
            window.innerWidth - menuWidth - viewportPadding
        );

        setPosition({ top, left });
    }, []);

    useEffect(() => {
        if (!isOpen) {
            setPosition(null);
            return;
        }

        const frameId = requestAnimationFrame(updatePosition);

        const handleMouseDown = (event: MouseEvent) => {
            const target = event.target as Node;

            if (
                buttonRef.current?.contains(target) ||
                menuRef.current?.contains(target)
            ) {
                return;
            }

            onClose();
        };

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        const handleScrollOrResize = () => {
            updatePosition();
        };

        document.addEventListener('mousedown', handleMouseDown);
        document.addEventListener('keydown', handleKeyDown);

        window.addEventListener('resize', handleScrollOrResize);
        window.addEventListener('scroll', handleScrollOrResize, true);

        return () => {
            cancelAnimationFrame(frameId);

            document.removeEventListener('mousedown', handleMouseDown);
            document.removeEventListener('keydown', handleKeyDown);

            window.removeEventListener('resize', handleScrollOrResize);
            window.removeEventListener('scroll', handleScrollOrResize, true);
        };
    }, [isOpen, onClose, updatePosition]);

    return (
        <>
            <button
                ref={buttonRef}
                type="button"
                data-cy={buttonDataCy}
                aria-label={ariaLabel}
                aria-expanded={isOpen}
                aria-haspopup="menu"
                onClick={event => {
                    event.stopPropagation();
                    onToggle();
                }}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-full hover:bg-gray-100 transition focus:outline-none"
            >
                <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="w-5 h-5"
                    viewBox="0 -960 960 960"
                    fill="currentColor"
                    aria-hidden="true"
                >
                    <path d="M479.79-192Q450-192 429-213.21t-21-51Q408-294 429.21-315t51-21Q510-336 531-314.79t21 51Q552-234 530.79-213t-51 21Zm0-216Q450-408 429-429.21t-21-51Q408-510 429.21-531t51-21Q510-552 531-530.79t21 51Q552-450 530.79-429t-51 21Zm0-216Q450-624 429-645.21t-21-51Q408-726 429.21-747t51-21Q510-768 531-746.79t21 51Q552-666 530.79-645t-51 21Z" />
                </svg>
            </button>

            {isOpen &&
                typeof document !== 'undefined' &&
                createPortal(
                    <div
                        ref={menuRef}
                        data-cy={menuDataCy}
                        role="menu"
                        style={{
                            top: position?.top ?? 0,
                            left: position?.left ?? 0,
                            visibility: position ? 'visible' : 'hidden'
                        }}
                        className="fixed w-48 bg-white rounded-md shadow-xl py-1 z-50 border border-gray-300 flex flex-col whitespace-nowrap divide-y divide-gray-100"
                    >
                        {children}
                    </div>,
                    document.body
                )}
        </>
    );
}