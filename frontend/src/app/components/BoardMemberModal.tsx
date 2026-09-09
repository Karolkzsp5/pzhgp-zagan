"use client";

import React, { useState, useEffect } from 'react';
import { BoardMemberDto, BoardMemberRequest, BoardRole, BoardRoleTranslations } from '@/app/types/board';
import { boardService } from '@/app/services/boardService';
import { getAuthToken } from '@/utils/jwt';

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
    const [error, setError] = useState<string | null>(null);

    const formatPhoneNumber = (val: string) => {
        const digits = val.replace(/\D/g, '').slice(0, 9);
        return digits.replace(/(\d{3})(?=\d)/g, '$1 ').trim();
    };

    const oddzialSortWeights: Record<string, number> = {
        'PREZES': 1, 'WICEPREZES_DS_LOTOWYCH': 2, 'WICEPREZES_DS_FINANSOWYCH': 3,
        'WICEPREZES_DS_GOSPODARCZYCH': 4, 'SEKRETARZ': 5, 'CZLONEK_ZARZADU': 6
    };
    const sectionSortWeights: Record<string, number> = {
        'PREZES': 1, 'SKARBNIK': 2, 'SEKRETARZ': 3
    };

    const allowedRoles = (managedSectionId === 0 || managedSectionId === null)
        ? Object.entries(BoardRoleTranslations)
            .filter(([key]) => key !== 'SKARBNIK')
            .sort((a, b) => (oddzialSortWeights[a[0]] || 99) - (oddzialSortWeights[b[0]] || 99))
        : Object.entries(BoardRoleTranslations)
            .filter(([key]) => ['PREZES', 'SKARBNIK', 'SEKRETARZ'].includes(key))
            .sort((a, b) => (sectionSortWeights[a[0]] || 99) - (sectionSortWeights[b[0]] || 99));

    const availableBreeders = (managedSectionId && managedSectionId !== 0)
        ? breeders.filter(b => b.sectionId === managedSectionId)
        : breeders;

    useEffect(() => {
        if (managedSectionId !== 0 && managedSectionId !== null) {
            if (!['PREZES', 'SKARBNIK', 'SEKRETARZ'].includes(role)) {
                setRole(BoardRole.PREZES);
            }

            if (breederId) {
                const selectedBreeder = breeders.find(b => b.id === breederId);
                if (selectedBreeder && selectedBreeder.sectionId !== managedSectionId) {
                    setBreederId(null);
                }
            }
        } else {
            if (role === ('SKARBNIK' as BoardRole)) {
                setRole(BoardRole.PREZES);
            }
        }
    }, [managedSectionId, role, breederId, breeders]);

    useEffect(() => {
        if (!isOpen) return;

        const fetchDictionaries = async () => {
            const token = getAuthToken();
            try {
                const [sectionsRes, breedersRes] = await Promise.all([
                    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/sections`),
                    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/admin/registered`, {
                        headers: { 'Authorization': `Bearer ${token}` }
                    })
                ]);

                if (!sectionsRes.ok || !breedersRes.ok) {
                    throw new Error('Nie udało się pobrać danych formularza. Spróbuj ponownie.');
                }

                setSections(await sectionsRes.json());

                const allBreeders: Breeder[] = await breedersRes.json();
                setBreeders(allBreeders.filter(b => b.status === 'ACTIVE'));

            } catch (err: any) {
                setError(err.message || 'Wystąpił problem z połączeniem.');
            }
        };

        fetchDictionaries();
    }, [isOpen]);

    useEffect(() => {
        if (memberToEdit) {
            setRole(memberToEdit.role);
            setManagedSectionId(memberToEdit.managedSectionId);
            setContactPhone(memberToEdit.publicPhoneNumber ? formatPhoneNumber(memberToEdit.publicPhoneNumber) : '');

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
            managedSectionId: managedSectionId === 0 ? null : managedSectionId,
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
        } catch (err: any) {
            setError(err.message || 'Wystąpił błąd podczas zapisywania');
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity overflow-y-auto">
            <div className="bg-white rounded-lg shadow-2xl max-w-lg w-full p-6 relative max-h-[90vh] overflow-y-auto">
                <h3 className="text-2xl font-bold text-gray-900 mb-6 border-b pb-2">
                    {memberToEdit ? 'Edytuj członka zarządu' : 'Dodaj członka zarządu'}
                </h3>

                <form onSubmit={handleSubmit} className="space-y-5">

                    <div className="grid grid-cols-1 gap-4">
                        <div>
                            <label className="block text-sm font-semibold text-gray-800 mb-1">Zarząd (Oddział czy Sekcja)</label>
                            <select
                                value={managedSectionId || 0}
                                onChange={(e) => setManagedSectionId(Number(e.target.value) === 0 ? null : Number(e.target.value))}
                                className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                            >
                                <option value={0}>Zarząd Oddziału</option>
                                {sections.map(sec => (
                                    <option key={sec.id} value={sec.id}>Zarząd Sekcji: {sec.name}</option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-semibold text-gray-800 mb-1">Stanowisko</label>
                            <select
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
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
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
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Imię</label>
                                    <input
                                        type="text"
                                        maxLength={32}
                                        required={!isRegistered}
                                        value={customName}
                                        onChange={(e) => setCustomName(e.target.value)}
                                        className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Nazwisko</label>
                                    <input
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
                        <label className="block text-sm font-semibold text-gray-800 mb-1">Publiczny nr telefonu (opcjonalny)</label>
                        <p className="text-xs text-gray-600 mb-2">
                            Wypełnienie tego pola sprawi, że numer będzie widoczny na stronie dla wszystkich.
                        </p>
                        <input
                            type="text"
                            placeholder="np. 123 456 789"
                            maxLength={11}
                            value={contactPhone}
                            onChange={(e) => setContactPhone(formatPhoneNumber(e.target.value))}
                            className="w-full bg-white text-gray-900 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm p-2.5"
                        />
                    </div>

                    {error && (
                        <div className="p-4 my-4 rounded transition-all duration-300 text-sm bg-red-100 text-red-700">
                            {error}
                        </div>
                    )}

                    <div className="flex justify-end space-x-3 pt-4 border-t mt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm"
                            disabled={isLoading}
                        >
                            Anuluj
                        </button>
                        <button
                            type="submit"
                            className="px-4 py-2 text-sm font-bold text-white bg-blue-600 rounded-md hover:bg-blue-700 transition shadow-sm flex items-center"
                            disabled={isLoading}
                        >
                            {isLoading ? 'Zapisywanie...' : 'Zapisz'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}