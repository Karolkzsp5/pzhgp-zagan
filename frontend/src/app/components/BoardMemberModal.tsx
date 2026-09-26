"use client";

import React, { useState, useEffect } from 'react';
import { BoardMemberDto, BoardMemberRequest, BoardRole, BoardRoleTranslations, BRANCH_ROLE_ORDER, SECTION_ROLE_ORDER, SECTION_ROLES } from '@/app/types/board';
import { boardService } from '@/app/services/boardService';
import { API_URL, fetchWithAuth } from '@/app/utils/apiClient';
import { formatPhoneInput } from '@/app/utils/formatters';
import Modal from '@/app/components/Modal';

interface Section {
    id: number;
    name: string;
}

interface Breeder {
    id: number;
    name: string;
    surname: string;
    status: string;
    sectionId: number;
}

interface BoardMemberModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSaved: () => void;
    memberToEdit: BoardMemberDto | null;
}

export default function BoardMemberModal({ isOpen, onClose, onSaved, memberToEdit }: BoardMemberModalProps) {
    const [role, setRole] = useState<BoardRole>(BoardRole.CZLONEK_ZARZADU);
    const [managedSectionId, setManagedSectionId] = useState<number | null>(null);

    const [isRegistered, setIsRegistered] = useState<boolean>(true);
    const [breederId, setBreederId] = useState<number | null>(null);
    const [customName, setCustomName] = useState('');
    const [customSurname, setCustomSurname] = useState('');
    const [contactPhone, setContactPhone] = useState('');

    const [sections, setSections] = useState<Section[]>([]);
    const [breeders, setBreeders] = useState<Breeder[]>([]);

    const [isLoading, setIsLoading] = useState(false);
    const [isDictionaryLoading, setIsDictionaryLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const allowedRoles = managedSectionId === null
        ? Object.entries(BoardRoleTranslations)
            .filter(([key]) => key !== BoardRole.SKARBNIK)
            .sort((a, b) => (BRANCH_ROLE_ORDER[a[0] as BoardRole] ?? 99) - (BRANCH_ROLE_ORDER[b[0] as BoardRole] ?? 99))
        : Object.entries(BoardRoleTranslations)
            .filter(([key]) => SECTION_ROLES
            .includes(key as BoardRole))
            .sort((a, b) => (SECTION_ROLE_ORDER[a[0] as BoardRole] ?? 99) - (SECTION_ROLE_ORDER[b[0] as BoardRole] ?? 99));

    const availableBreeders = managedSectionId !== null
        ? breeders.filter(b => b.sectionId === managedSectionId)
        : breeders;

    const handleBoardChange = (value: string) => {
        const newSectionId = Number(value) === 0 ? null : Number(value);

        setManagedSectionId(newSectionId);

        if (newSectionId !== null) {
            if (!SECTION_ROLES.includes(role)) {
                setRole(BoardRole.PREZES);
            }

            if (breederId) {
                const selectedBreeder = breeders.find(b => b.id === breederId);

                if (selectedBreeder && selectedBreeder.sectionId !== newSectionId) {
                    setBreederId(null);
                }
            }
        } else if (role === BoardRole.SKARBNIK) {
            setRole(BoardRole.PREZES);
        }
    };

    useEffect(() => {

        const fetchDictionaries = async () => {
            setIsDictionaryLoading(true);

            try {
                const [sectionsRes, breedersRes] = await Promise.all([
                    fetchWithAuth(`${API_URL}/api/sections`),
                    fetchWithAuth(`${API_URL}/api/admin/registered`)
                ]);

                if (!sectionsRes.ok || !breedersRes.ok) {
                    throw new Error('Nie udało się pobrać danych formularza. Spróbuj ponownie.');
                }

                setSections(await sectionsRes.json());

                const allBreeders: Breeder[] = await breedersRes.json();
                setBreeders(allBreeders.filter(b => b.status === 'ACTIVE'));

            } catch (error) {
                console.error('Błąd podczas pobierania danych formularza zarządu:', error);

                setError(
                    error instanceof Error
                        ? error.message
                        : 'Wystąpił problem z połączeniem.'
                );
            } finally {
                setIsDictionaryLoading(false);
            }
        };

        void fetchDictionaries();
    }, [isOpen]);

    useEffect(() => {
        if (memberToEdit) {
            setRole(memberToEdit.role);
            setManagedSectionId(memberToEdit.managedSectionId);
            setContactPhone(memberToEdit.publicPhoneNumber ? formatPhoneInput(memberToEdit.publicPhoneNumber) : '');

            if (memberToEdit.breederId) {
                setIsRegistered(true);
                setBreederId(memberToEdit.breederId);
                setCustomName('');
                setCustomSurname('');
            } else {
                setIsRegistered(false);
                setBreederId(null);
                setCustomName(memberToEdit.firstName);
                setCustomSurname(memberToEdit.lastName);
            }
        } else {
            setRole(BoardRole.CZLONEK_ZARZADU);
            setManagedSectionId(null);
            setIsRegistered(true);
            setBreederId(null);
            setCustomName('');
            setCustomSurname('');
            setContactPhone('');
        }
        setError(null);
    }, [memberToEdit, isOpen]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        setError(null);

        const cleanPhone = contactPhone.replace(/\s+/g, '');

        if (cleanPhone.length > 0 && cleanPhone.length !== 9) {
            setError('Numer telefonu musi składać się dokładnie z 9 cyfr.');
            setIsLoading(false);
            return;
        }

        const request: BoardMemberRequest = {
            role,
            managedSectionId,
            breederId: isRegistered ? breederId : null,
            customName: !isRegistered ? customName : null,
            customSurname: !isRegistered ? customSurname : null,
            contactPhone: cleanPhone === '' ? null : cleanPhone
        };

        try {
            if (memberToEdit) {
                await boardService.updateBoardMember(memberToEdit.id, request);
            } else {
                await boardService.createBoardMember(request);
            }
            onSaved();
        } catch (error) {
            setError(
                error instanceof Error
                    ? error.message
                    : 'Wystąpił błąd podczas zapisywania.'
            );
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <Modal
            isOpen={isOpen}
            title={memberToEdit ? 'Edytuj członka zarządu' : 'Dodaj członka zarządu'}
            onClose={onClose}
            closeDisabled={isLoading}
            maxWidthClass="max-w-lg"
        >
            <div className="relative">
                {isDictionaryLoading && (
                    <div className="absolute inset-0 z-20 bg-white/80 flex items-center justify-center rounded-lg">
                        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-700"></div>
                    </div>
                )}
                <form onSubmit={handleSubmit} className="p-6 space-y-5">

                    <div className="grid grid-cols-1 gap-4">
                        <div>
                            <label htmlFor="board-type" className="block text-sm font-semibold text-gray-800 mb-1">Zarząd (Oddział czy Sekcja)</label>
                            <select
                                id="board-type"
                                value={managedSectionId || 0}
                                onChange={(e) => handleBoardChange(e.target.value)}
                                className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                            >
                                <option value={0}>Zarząd Oddziału</option>
                                {sections.map(sec => (
                                    <option key={sec.id} value={sec.id}>Zarząd Sekcji: {sec.name}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label htmlFor="board-role" className="block text-sm font-semibold text-gray-800 mb-1">Stanowisko</label>
                            <select
                                id="board-role"
                                required
                                value={role}
                                onChange={(e) => setRole(e.target.value as BoardRole)}
                                className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                            >
                                {allowedRoles.map(([key, label]) => (
                                    <option key={key} value={key}>{label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <hr className="my-4 border-gray-200" />

                    <div>
                        <label className="block text-sm font-semibold text-gray-800 mb-3">Dane osoby pełniącej funkcję</label>
                        <div className="flex gap-6 mb-4">
                            <label htmlFor="board-breeder" className="flex items-center gap-2 cursor-pointer">
                                <input
                                    id="board-breeder"
                                    type="radio"
                                    name="personSource"
                                    checked={isRegistered}
                                    onChange={() => setIsRegistered(true)}
                                    className="text-blue-600 focus:ring-blue-500 w-4 h-4"
                                />
                                <span className="text-sm font-medium text-gray-900">Konto w systemie</span>
                            </label>
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="radio"
                                    name="personSource"
                                    checked={!isRegistered}
                                    onChange={() => setIsRegistered(false)}
                                    className="text-blue-600 focus:ring-blue-500 w-4 h-4"
                                />
                                <span className="text-sm font-medium text-gray-900">Osoba bez konta</span>
                            </label>
                        </div>

                        {isRegistered ? (
                            <div>
                                <select
                                    required={isRegistered}
                                    value={breederId || ''}
                                    onChange={(e) => setBreederId(Number(e.target.value))}
                                    className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                                >
                                    <option value="" disabled hidden>-- Wybierz hodowcę --</option>
                                    {availableBreeders.length === 0 ? (
                                        <option value="" disabled>Brak hodowców w tej sekcji</option>
                                    ) : (
                                        availableBreeders.map(b => (
                                            <option key={b.id} value={b.id}>{b.name} {b.surname}</option>
                                        ))
                                    )}
                                </select>
                            </div>
                        ) : (
                            <div className="grid grid-cols-2 gap-4 bg-gray-50 p-4 rounded-md border border-gray-200">
                                <div>
                                    <label htmlFor="custom-name" className="block text-sm font-medium text-gray-700 mb-1">Imię</label>
                                    <input
                                        id="custom-name"
                                        type="text"
                                        maxLength={32}
                                        required={!isRegistered}
                                        value={customName}
                                        onChange={(e) => setCustomName(e.target.value)}
                                        className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                                    />
                                </div>
                                <div>
                                    <label htmlFor="custom-surname" className="block text-sm font-medium text-gray-700 mb-1">Nazwisko</label>
                                    <input
                                        id="custom-surname"
                                        type="text"
                                        maxLength={64}
                                        required={!isRegistered}
                                        value={customSurname}
                                        onChange={(e) => setCustomSurname(e.target.value)}
                                        className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                                    />
                                </div>
                            </div>
                        )}
                    </div>

                    <hr className="my-4 border-gray-200" />

                    <div>
                        <label htmlFor="contact-phone" className="block text-sm font-semibold text-gray-800 mb-1">Publiczny nr telefonu (opcjonalny)</label>
                        <p className="text-xs text-gray-600 mb-2">
                            Wypełnienie tego pola sprawi, że numer będzie widoczny na stronie dla wszystkich.
                        </p>
                        <input
                            id="contact-phone"
                            type="text"
                            placeholder="np. 123 456 789"
                            maxLength={11}
                            value={contactPhone}
                            onChange={(e) => setContactPhone(formatPhoneInput(e.target.value))}
                            className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                        />
                    </div>

                    {error && (
                        <div role="alert"
                            className="p-4 my-4 rounded transition-all duration-300 text-sm bg-red-100 text-red-700"
                        >
                            {error}
                        </div>
                    )}

                    <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100 mt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm"
                            disabled={isLoading || isDictionaryLoading}
                        >
                            Anuluj
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 text-sm font-bold text-white bg-blue-600 rounded-md hover:bg-blue-700 transition shadow-sm flex items-center"
                            disabled={isLoading || isDictionaryLoading}
                        >
                            {isLoading ? 'Zapisywanie...' : 'Zapisz'}
                        </button>
                    </div>
                </form>
            </div>
        </Modal>
    );
}
