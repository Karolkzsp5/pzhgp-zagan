"use client";

import React, { useEffect, useState } from 'react';
import Navbar from "@/app/components/Navbar";
import Footer from '@/app/components/Footer';
import BoardMemberModal from '@/app/components/BoardMemberModal';
import ConfirmModal from '@/app/components/ConfirmModal';
import { getAuthToken, decodeJwt } from '@/utils/jwt';
import { boardService } from '@/app/services/boardService';
import { BoardMemberDto, BoardRoleTranslations } from '@/app/types/board';

interface SectionDto {
    id: number;
    name: string;
}

export default function BoardPage() {
    const [members, setMembers] = useState<BoardMemberDto[]>([]);
    const [sections, setSections] = useState<SectionDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [isAdmin, setIsAdmin] = useState(false);

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [memberToEdit, setMemberToEdit] = useState<BoardMemberDto | null>(null);

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const closeConfirmModal = () => setModalConfig(prev => ({ ...prev, isOpen: false }));
    const showAlert = (title: string, message: string) => {
        setModalConfig({ isOpen: true, title, message, isAlert: true, onConfirm: closeConfirmModal });
    };

    useEffect(() => {
        const token = getAuthToken();
        if (token) {
            const payload = decodeJwt(token);
            if (payload && payload.role === 'ADMINISTRATOR') {
                setIsAdmin(true);
            }
        }
        fetchData();
    }, []);

    const fetchData = async () => {
        setIsLoading(true);
        try {
            const [boardData, sectionsResponse] = await Promise.all([
                boardService.getAllBoardMembers(),
                fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/sections`)
            ]);

            setMembers(boardData);

            if (sectionsResponse.ok) {
                setSections(await sectionsResponse.json());
            } else {
                console.error('Nie udało się pobrać listy sekcji z serwera.');
            }
        } catch (err) {
            setError('Błąd połączenia z serwerem podczas pobierania danych.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleDelete = (id: number) => {
        setModalConfig({
            isOpen: true,
            title: 'Usuń członka zarządu',
            message: 'Czy na pewno chcesz usunąć tę osobę ze stanowiska w zarządzie?',
            isAlert: false,
            onConfirm: async () => {
                closeConfirmModal();
                try {
                    await boardService.deleteBoardMember(id);
                    fetchData();
                } catch (err: any) {
                    showAlert('Błąd', 'Wystąpił błąd podczas usuwania. Spróbuj ponownie.');
                }
            }
        });
    };

    const openAddModal = () => {
        setMemberToEdit(null);
        setIsModalOpen(true);
    };

    const openEditModal = (member: BoardMemberDto) => {
        setMemberToEdit(member);
        setIsModalOpen(true);
    };

    const handleModalSaved = () => {
        setIsModalOpen(false);
        fetchData();
    };

    const oddzialSortWeights: Record<string, number> = {
        'PREZES': 1,
        'WICEPREZES_DS_LOTOWYCH': 2,
        'WICEPREZES_DS_FINANSOWYCH': 3,
        'WICEPREZES_DS_GOSPODARCZYCH': 4,
        'SEKRETARZ': 5,
        'CZLONEK_ZARZADU': 6
    };

    const sectionSortWeights: Record<string, number> = {
        'PREZES': 1,
        'SKARBNIK': 2,
        'SEKRETARZ': 3
    };

    const sortMembers = (membersList: BoardMemberDto[], weights: Record<string, number>) => {
        return [...membersList].sort((a, b) => {
            const weightA = weights[a.role] || 99;
            const weightB = weights[b.role] || 99;

            if (weightA !== weightB) {
                return weightA - weightB;
            }

            const lastNameCompare = a.lastName.localeCompare(b.lastName, 'pl');
            if (lastNameCompare !== 0) return lastNameCompare;

            return a.firstName.localeCompare(b.firstName, 'pl');
        });
    };

    const oddzialMembers = sortMembers(
        members.filter(m => m.managedSectionId === null),
        oddzialSortWeights
    );

    const MemberCard = ({ member }: { member: BoardMemberDto }) => (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 flex flex-col relative hover:shadow-md transition">
            {isAdmin && (
                <div className="absolute top-4 right-4 flex gap-2 items-center">
                    <button
                        onClick={() => openEditModal(member)}
                        className="text-gray-400 hover:text-blue-600 p-1 transition-colors"
                        title="Edytuj"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                            <path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h357l-80 80H200v560h560v-278l80-80v358q0 33-23.5 56.5T760-120H200Zm280-360ZM360-360v-170l367-367q12-12 27-18t30-6q16 0 30.5 6t26.5 18l56 57q11 12 17 26.5t6 29.5q0 15-5.5 29.5T897-728L530-360H360Zm481-424-56-56 56 56ZM440-440h56l232-232-28-28-29-28-231 231v57Zm260-260-29-28 29 28 28 28-28-28Z"/>
                        </svg>
                    </button>
                    <button
                        onClick={() => handleDelete(member.id)}
                        className="text-gray-400 hover:text-red-600 p-1 transition-colors"
                        title="Usuń"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 -960 960 960" fill="currentColor">
                            <path d="M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z"/>
                        </svg>
                    </button>
                </div>
            )}

            <h3 className="text-xl font-bold text-gray-900 pr-16">
                {member.firstName} {member.lastName}
            </h3>
            <p className="text-sm font-semibold text-blue-600 mt-1 mb-4">
                {BoardRoleTranslations[member.role]}
            </p>

            {member.publicPhoneNumber ? (
                <div className="flex items-center text-gray-700 text-sm mt-auto">
                    <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 mr-2 shrink-0" viewBox="0 -960 960 960" fill="currentColor">
                        <path d="M798-120q-125 0-247-54.5T329-329Q229-429 174.5-551T120-798q0-18 12-30t30-12h162q14 0 25 9.5t13 22.5l26 140q2 16-1 27t-11 19l-97 98q20 37 47.5 71.5T387-386q31 31 65 57.5t72 48.5l94-94q9-9 23.5-13.5T670-390l138 28q14 4 23 14.5t9 23.5v162q0 18-12 30t-30 12ZM241-600l66-66-17-94h-89q5 41 14 81t26 79Zm358 358q39 17 79.5 27t81.5 13v-88l-94-19-67 67ZM241-600Zm358 358Z"/>
                    </svg>
                    <a href={`tel:+48${member.publicPhoneNumber}`} className="hover:text-blue-600 transition font-medium">
                        {member.publicPhoneNumber.replace(/(\d{3})(\d{3})(\d{3})/, '$1 $2 $3')}
                    </a>
                </div>
            ) : (
                <div className="h-[20px] mt-auto"></div>
            )}
        </div>
    );

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Navbar />

            <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-gray-200 pb-5 mb-8">
                    <div className="flex-1 pr-2">
                        <h1 className="text-3xl font-bold text-gray-900">Zarząd</h1>
                        <p className="mt-2 text-sm text-gray-600">Struktura organizacyjna oddziału i sekcji.</p>
                    </div>
                    {isAdmin && (
                        <button
                            onClick={openAddModal}
                            className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2 px-4 rounded-md shadow-sm transition shrink-0 whitespace-nowrap"
                        >
                            + Dodaj członka
                        </button>
                    )}
                </div>

                {error && (
                    <div className="bg-red-50 border-l-4 border-red-400 p-4 rounded mb-6">
                        <p className="text-sm text-red-700">{error}</p>
                    </div>
                )}

                {isLoading ? (
                    <div className="flex justify-center items-center py-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-700"></div>
                    </div>
                ) : (
                    <>
                        <section className="mb-12">
                            <h2 className="text-xl font-bold text-gray-900 mb-6">
                                Zarząd Oddziału:
                            </h2>
                            {oddzialMembers.length > 0 ? (
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {oddzialMembers.map(member => (
                                        <MemberCard key={member.id} member={member} />
                                    ))}
                                </div>
                            ) : (
                                <div className="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-500">
                                    Brak dodanych członków zarządu oddziału.
                                </div>
                            )}
                        </section>

                        {sections.map(section => {
                            const sectionMembers = sortMembers(
                                members.filter(m => m.managedSectionId === section.id),
                                sectionSortWeights
                            );

                            return (
                                <section key={section.id} className="mb-12">
                                    <h2 className="text-xl font-bold text-gray-900 mb-6">
                                        Zarząd Sekcji: {section.name}
                                    </h2>
                                    {sectionMembers.length > 0 ? (
                                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                            {sectionMembers.map(member => (
                                                <MemberCard key={member.id} member={member} />
                                            ))}
                                        </div>
                                    ) : (
                                        <div className="bg-white rounded-lg border border-gray-200 p-8 text-center text-gray-500">
                                            Brak dodanych członków zarządu sekcji.
                                        </div>
                                    )}
                                </section>
                            );
                        })}
                    </>
                )}
            </main>

            <BoardMemberModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSaved={handleModalSaved}
                memberToEdit={memberToEdit}
            />

            <ConfirmModal
                isOpen={modalConfig.isOpen}
                title={modalConfig.title}
                message={modalConfig.message}
                isAlert={modalConfig.isAlert}
                onConfirm={modalConfig.onConfirm}
                onCancel={closeConfirmModal}
            />

            <Footer />
        </div>
    );
}