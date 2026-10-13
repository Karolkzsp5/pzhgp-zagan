"use client";

import { ReactNode, useCallback, useEffect, useId, useRef, useState } from 'react';
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
    const menuId = useId();
    const onCloseRef = useRef(onClose);

    useEffect(() => {
        onCloseRef.current = onClose;
    }, [onClose]);

    const getMenuItems = useCallback(() => {
        return Array.from(menuRef.current?.querySelectorAll<HTMLElement>('[role="menuitem"]:not([disabled])') ?? []);
    }, []);

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

        const availableBelow = window.innerHeight - buttonRect.bottom;
        const shouldOpenAbove = availableBelow < menuHeight + gap + viewportPadding && buttonRect.top >= menuHeight + gap + viewportPadding;
        const top = shouldOpenAbove ? buttonRect.top - menuHeight - gap : buttonRect.bottom + gap;
        const preferredLeft = buttonRect.right - menuWidth;
        const left = Math.min(Math.max(viewportPadding, preferredLeft), window.innerWidth - menuWidth - viewportPadding);

        setPosition({ top, left });
    }, []);

    useEffect(() => {
        if (!isOpen) {
            setPosition(null);
            return;
        }

        let focusFrameId: number | null = null;
        const frameId = requestAnimationFrame(() => {
            updatePosition();

            focusFrameId = requestAnimationFrame(() => {
                const firstItem = getMenuItems()[0];

                firstItem?.focus({
                    preventScroll: true
                });
            });
        });

        const handleMouseDown = (event: MouseEvent) => {
            const target = event.target as Node;

            if (buttonRef.current?.contains(target) || menuRef.current?.contains(target)) {
                return;
            }

            onCloseRef.current();
        };

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                event.preventDefault();

                onCloseRef.current();
                buttonRef.current?.focus();
                return;
            }

            if (event.key === 'Tab') {
                onCloseRef.current();
                return;
            }

            const items = getMenuItems();

            if (items.length === 0) {
                return;
            }

            const currentIndex = items.indexOf(document.activeElement as HTMLElement);
            let nextIndex: number | null = null;

            if (event.key === 'ArrowDown') {
                nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % items.length;
            } else if (event.key === 'ArrowUp') {
                nextIndex = currentIndex < 0 ? items.length - 1 : (currentIndex - 1 + items.length) % items.length;
            } else if (event.key === 'Home') {
                nextIndex = 0;
            } else if (event.key === 'End') {
                nextIndex = items.length - 1;
            }

            if (nextIndex !== null) {
                event.preventDefault();
                items[nextIndex].focus();
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

            if (focusFrameId !== null) {
                cancelAnimationFrame(focusFrameId);
            }

            document.removeEventListener('mousedown', handleMouseDown);
            document.removeEventListener('keydown', handleKeyDown);
            window.removeEventListener('resize', handleScrollOrResize);
            window.removeEventListener('scroll', handleScrollOrResize, true);
        };
    }, [getMenuItems, isOpen, updatePosition]);

    return (
        <>
            <button
                ref={buttonRef}
                type="button"
                data-cy={buttonDataCy}
                aria-label={ariaLabel}
                aria-expanded={isOpen}
                aria-haspopup="menu"
                aria-controls={isOpen ? menuId : undefined}
                onClick={event => {event.stopPropagation();onToggle();}}
                className="text-gray-400 hover:text-gray-600 p-1.5 rounded-full hover:bg-gray-100 transition focus:outline-none"
            >
                <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor" aria-hidden="true">
                    <path d="M479.79-192Q450-192 429-213.21t-21-51Q408-294 429.21-315t51-21Q510-336 531-314.79t21 51Q552-234 530.79-213t-51 21Zm0-216Q450-408 429-429.21t-21-51Q408-510 429.21-531t51-21Q510-552 531-530.79t21 51Q552-450 530.79-429t-51 21Zm0-216Q450-624 429-645.21t-21-51Q408-726 429.21-747t51-21Q510-768 531-746.79t21 51Q552-666 530.79-645t-51 21Z" />
                </svg>
            </button>

            {isOpen &&
                typeof document !== 'undefined' &&
                createPortal(
                    <div
                        id={menuId}
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
                )
            }
        </>
    );
}