"use client";

import { useState, useEffect, useRef } from 'react';
import { useEditor, EditorContent } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Highlight from '@tiptap/extension-highlight';
import TextAlign from '@tiptap/extension-text-align';

interface TextEditorProps {
    content: string;
    onChange: (content: string) => void;
}

const HIGHLIGHT_COLORS = [
    { color: '#bbf7d0', name: 'Zielony' },
    { color: '#bfdbfe', name: 'Niebieski' },
    { color: '#fecaca', name: 'Czerwony' },
    { color: '#fed7aa', name: 'Pomarańczowy' },
    { color: '#fef08a', name: 'Żółty' },
    { color: '#e9d5ff', name: 'Fioletowy' },
];

const ToolbarButton = ({ onClick, isActive = false, disabled = false, children, title }: any) => (
    <button
        type="button"
        onMouseDown={(e) => {
            e.preventDefault();
            if (onClick) onClick();
        }}
        disabled={disabled}
        title={title}
        className={`p-1.5 rounded-md flex items-center justify-center transition-colors
            ${isActive ? 'bg-gray-200 text-gray-900' : 'text-gray-500 hover:bg-gray-100 hover:text-gray-900'}
            ${disabled ? 'opacity-30 cursor-not-allowed hover:bg-transparent hover:text-gray-500' : ''}
        `}
    >
        {children}
    </button>
);

const Divider = () => <div className="w-px h-5 bg-gray-300 mx-1 self-center"></div>;

const MenuBar = ({ editor }: { editor: any }) => {
    const [, setUpdateCount] = useState(0);

    const [isColorPickerOpen, setIsColorPickerOpen] = useState(false);
    const [isAlignPickerOpen, setIsAlignPickerOpen] = useState(false);

    const colorPickerRef = useRef<HTMLDivElement>(null);
    const alignPickerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (colorPickerRef.current && !colorPickerRef.current.contains(event.target as Node)) {
                setIsColorPickerOpen(false);
            }
            if (alignPickerRef.current && !alignPickerRef.current.contains(event.target as Node)) {
                setIsAlignPickerOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    useEffect(() => {
        if (!editor) return;
        const forceUpdate = () => setUpdateCount((prev) => prev + 1);

        editor.on('transaction', forceUpdate);
        editor.on('selectionUpdate', forceUpdate);

        return () => {
            editor.off('transaction', forceUpdate);
            editor.off('selectionUpdate', forceUpdate);
        };
    }, [editor]);

    if (!editor) return null;

    const activeAlign = ['center', 'right', 'justify'].find(align => editor.isActive({ textAlign: align })) || 'left';

    return (
        <div className="flex flex-wrap items-center gap-1 p-1.5 border-b border-gray-200 bg-white rounded-t-md">

            {/* Undo/Redo */}
            <ToolbarButton onClick={() => editor.chain().focus().undo().run()} disabled={!editor.can().chain().focus().undo().run()} title="Cofnij">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M280-200v-80h284q63 0 109.5-40T720-420q0-60-46.5-100T564-560H312l104 104-56 56-200-200 200-200 56 56-104 104h252q97 0 166.5 63T800-420q0 94-69.5 157T564-200H280Z"/></svg>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().redo().run()} disabled={!editor.can().chain().focus().redo().run()} title="Ponów">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M396-200q-97 0-166.5-63T160-420q0-94 69.5-157T396-640h252L544-744l56-56 200 200-200 200-56-56 104-104H396q-63 0-109.5 40T240-420q0 60 46.5 100T396-280h284v80H396Z"/></svg>
            </ToolbarButton>

            <Divider />

            {/* Text style */}
            <ToolbarButton onClick={() => editor.chain().focus().toggleBold().run()} isActive={editor.isActive('bold')} title="Pogrubienie">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M272-200v-560h221q65 0 120 40t55 111q0 51-23 78.5T602-491q25 11 55.5 41t30.5 90q0 89-65 124.5T501-200H272Zm121-112h104q48 0 58.5-24.5T566-372q0-11-10.5-35.5T494-432H393v120Zm0-228h93q33 0 48-17t15-38q0-24-17-39t-44-15h-95v109Z"/></svg>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleItalic().run()} isActive={editor.isActive('italic')} title="Kursywa">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M200-200v-100h160l120-360H320v-100h400v100H580L460-300h140v100H200Z"/></svg>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleUnderline().run()} isActive={editor.isActive('underline')} title="Podkreślenie">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M200-160v-80h560v80H200Zm280-160q-83 0-141.5-58.5T280-520v-280h80v280q0 50 35 85t85 35q50 0 85-35t35-85v-280h80v280q0 83-58.5 141.5T480-320Z"/></svg>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleStrike().run()} isActive={editor.isActive('strike')} title="Przekreślenie">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M486-160q-76 0-135-45t-85-123l88-38q14 48 48.5 79t85.5 31q42 0 76-20t34-64q0-18-7-33t-19-27h112q5 14 7.5 28.5T694-340q0 86-61.5 133T486-160ZM80-480v-80h800v80H80Zm402-326q66 0 115.5 32.5T674-674l-88 39q-9-29-33.5-52T484-710q-41 0-68 18.5T386-640h-96q2-69 54.5-117.5T482-806Z"/></svg>
            </ToolbarButton>

            <div className="relative ml-1" ref={colorPickerRef}>
                <ToolbarButton
                    onClick={() => setIsColorPickerOpen(!isColorPickerOpen)}
                    isActive={editor.isActive('highlight') || isColorPickerOpen}
                    title="Kolor zaznaczenia"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor">
                        <path d="M96 0v-192h768V0H96Zm168-360h51l279-279-26-27-25-24-279 279v51Zm-72 72v-152.92L594-843q11-11 23.84-16 12.83-5 27-5 14.16 0 27.16 5t24.1 15.94L747-792q11 11 16 24t5 27.4q0 13.49-4.95 26.54-4.95 13.05-15.75 23.85L345-288H192Zm503-455-51-49 51 49ZM594-639l-26-27-25-24 51 51Z"/>
                    </svg>
                </ToolbarButton>

                {isColorPickerOpen && (
                    <div className="absolute top-full right-0 mt-1 z-50 flex items-center gap-1.5 p-2 bg-white border border-gray-200 rounded-md shadow-lg">
                        {HIGHLIGHT_COLORS.map(({ color, name }) => {
                            const isActive = editor.isActive('highlight', { color });
                            return (
                                <button
                                    key={color}
                                    type="button"
                                    onMouseDown={(e) => {
                                        e.preventDefault();
                                        if (isActive) {
                                            editor.chain().focus().unsetHighlight().run();
                                        } else {
                                            editor.chain().focus().setHighlight({ color }).run();
                                        }
                                        setIsColorPickerOpen(false);
                                    }}
                                    className={`w-6 h-6 rounded-full border border-gray-300 transition-transform shadow-sm
                                        ${isActive ? 'ring-2 ring-offset-1 ring-gray-900 scale-110' : 'hover:scale-110'}
                                    `}
                                    style={{ backgroundColor: color }}
                                    title={`${name} (kliknij aby ${isActive ? 'odznaczyć' : 'wybrać'})`}
                                />
                            );
                        })}
                    </div>
                )}
            </div>

            <Divider />

            {/* Lists */}
            <ToolbarButton onClick={() => editor.chain().focus().toggleBulletList().run()} isActive={editor.isActive('bulletList')} title="Lista punktowana">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M360-200v-80h480v80H360Zm0-240v-80h480v80H360Zm0-240v-80h480v80H360ZM200-160q-33 0-56.5-23.5T120-240q0-33 23.5-56.5T200-320q33 0 56.5 23.5T280-240q0 33-23.5 56.5T200-160Zm0-240q-33 0-56.5-23.5T120-480q0-33 23.5-56.5T200-560q33 0 56.5 23.5T280-480q0 33-23.5 56.5T200-400Zm-56.5-263.5Q120-687 120-720t23.5-56.5Q167-800 200-800t56.5 23.5Q280-753 280-720t-23.5 56.5Q233-640 200-640t-56.5-23.5Z"/></svg>
            </ToolbarButton>
            <ToolbarButton onClick={() => editor.chain().focus().toggleOrderedList().run()} isActive={editor.isActive('orderedList')} title="Lista numerowana">
                <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M120-80v-60h100v-30h-60v-60h60v-30H120v-60h120q17 0 28.5 11.5T280-280v40q0 17-11.5 28.5T240-200q17 0 28.5 11.5T280-160v40q0 17-11.5 28.5T240-80H120Zm0-280v-110q0-17 11.5-28.5T160-510h60v-30H120v-60h120q17 0 28.5 11.5T280-560v70q0 17-11.5 28.5T240-450h-60v30h100v60H120Zm60-280v-180h-60v-60h120v240h-60Zm180 440v-80h480v80H360Zm0-240v-80h480v80H360Zm0-240v-80h480v80H360Z"/></svg>
            </ToolbarButton>

            <Divider />

            <div className="relative ml-1" ref={alignPickerRef}>
                <ToolbarButton
                    onClick={() => setIsAlignPickerOpen(!isAlignPickerOpen)}
                    isActive={isAlignPickerOpen}
                    title="Wyrównanie tekstu"
                >
                    {activeAlign === 'left' && <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M120-160v-80h480v80H120Zm0-160v-80h720v80H120Zm0-160v-80h480v80H120Zm0-160v-80h720v80H120Z"/></svg>}
                    {activeAlign === 'center' && <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M240-160v-80h480v80H240Zm-120-160v-80h720v80H120Zm120-160v-80h480v80H240Zm-120-160v-80h720v80H120Z"/></svg>}
                    {activeAlign === 'right' && <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M360-160v-80h480v80H360Zm-240-160v-80h720v80H120Zm240-160v-80h480v80H360Zm-240-160v-80h720v80H120Z"/></svg>}
                    {activeAlign === 'justify' && <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M120-160v-80h720v80H120Zm0-160v-80h720v80H120Zm0-160v-80h720v80H120Zm0-160v-80h720v80H120Z"/></svg>}
                </ToolbarButton>

                {isAlignPickerOpen && (
                    <div className="absolute top-full left-0 mt-1 z-10 flex items-center gap-1 p-1 bg-white border border-gray-200 rounded-md shadow-lg">
                        <ToolbarButton
                            onClick={() => { editor.chain().focus().setTextAlign('left').run(); setIsAlignPickerOpen(false); }}
                            isActive={activeAlign === 'left'} title="Do lewej">
                            <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M120-160v-80h480v80H120Zm0-160v-80h720v80H120Zm0-160v-80h480v80H120Zm0-160v-80h720v80H120Z"/></svg>
                        </ToolbarButton>
                        <ToolbarButton
                            onClick={() => { editor.chain().focus().setTextAlign('center').run(); setIsAlignPickerOpen(false); }}
                            isActive={activeAlign === 'center'} title="Do środka">
                            <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M240-160v-80h480v80H240Zm-120-160v-80h720v80H120Zm120-160v-80h480v80H240Zm-120-160v-80h720v80H120Z"/></svg>
                        </ToolbarButton>
                        <ToolbarButton
                            onClick={() => { editor.chain().focus().setTextAlign('right').run(); setIsAlignPickerOpen(false); }}
                            isActive={activeAlign === 'right'} title="Do prawej">
                            <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M360-160v-80h480v80H360Zm-240-160v-80h720v80H120Zm240-160v-80h480v80H360Zm-240-160v-80h720v80H120Z"/></svg>
                        </ToolbarButton>
                        <ToolbarButton
                            onClick={() => { editor.chain().focus().setTextAlign('justify').run(); setIsAlignPickerOpen(false); }}
                            isActive={activeAlign === 'justify'} title="Wyjustuj">
                            <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="currentColor"><path d="M120-160v-80h720v80H120Zm0-160v-80h720v80H120Zm0-160v-80h720v80H120Zm0-160v-80h720v80H120Z"/></svg>
                        </ToolbarButton>
                    </div>
                )}
            </div>
        </div>
    );
};

export default function TextEditor({ content, onChange }: TextEditorProps) {
    const editor = useEditor({
        extensions: [
            StarterKit,
            Underline,
            Highlight.configure({ multicolor: true }),
            TextAlign.configure({ types: ['heading', 'paragraph'] }),
        ],
        content: content,
        immediatelyRender: false,
        editorProps: {
            attributes: {
                class: 'prose prose-sm sm:prose-base prose-p:m-0 max-w-none focus:outline-none min-h-[200px] p-4',
            },
        },
        onUpdate: ({ editor }) => {
            onChange(editor.getHTML());
        },
    });

    return (
        <div className="border border-gray-300 rounded-md shadow-sm bg-white overflow-hidden focus-within:ring-1 focus-within:ring-gray-900 focus-within:border-gray-900 transition-all">
            <MenuBar editor={editor} />
            <EditorContent editor={editor} />
        </div>
    );
}