"use client";

import { FormEvent, useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Navbar from '@/app/components/Navbar';
import Footer from '@/app/components/Footer';
import AuthGuard from '@/app/components/AuthGuard';
import Modal from '@/app/components/Modal';
import ConfirmModal from '@/app/components/ConfirmModal';
import LoadingState from '@/app/components/LoadingState';
import ErrorState from '@/app/components/ErrorState';
import EmptyState from '@/app/components/EmptyState';
import { flightPlanService } from '@/app/services/flightPlanService';
import { FlightPlanSummaryDto } from '@/app/types/flightPlan';
import { decodeJwt, getAuthToken, isJwtValid } from '@/app/utils/jwt';

export default function FlightPlansPage() {
    const [plans, setPlans] = useState<FlightPlanSummaryDto[]>([]);
    const [userRole, setUserRole] = useState<string | null>(null);

    const [isLoading, setIsLoading] = useState(true);
    const [loadError, setLoadError] = useState('');

    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newPlanYear, setNewPlanYear] = useState(String(new Date().getFullYear()));
    const [isSaving, setIsSaving] = useState(false);
    const [formError, setFormError] = useState('');

    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        isAlert: false,
        onConfirm: () => {}
    });

    const canManagePlans =
        userRole === 'ADMINISTRATOR' || userRole === 'MODERATOR';

    const closeConfirmModal = () => {
        setModalConfig(previous => ({
            ...previous,
            isOpen: false
        }));
    };

    const showAlert = (title: string, message: string) => {
        setModalConfig({
            isOpen: true,
            title,
            message,
            isAlert: true,
            onConfirm: closeConfirmModal
        });
    };

    const loadPlans = useCallback(async () => {
        setIsLoading(true);
        setLoadError('');

        try {
            setPlans(await flightPlanService.getAllPlans());
        } catch (error) {
            setLoadError(
                error instanceof Error ? error.message : 'Nie udało się pobrać planów lotów.'
            );
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        const token = getAuthToken();

        if (isJwtValid(token)) {
            const payload = decodeJwt(token!);
            setUserRole(payload?.role ?? null);
        }

        void loadPlans();
    }, [loadPlans]);

    const openCreateModal = () => {
        setNewPlanYear(String(new Date().getFullYear()));
        setFormError('');
        setIsCreateModalOpen(true);
    };

    const closeCreateModal = () => {
        if (isSaving) return;

        setIsCreateModalOpen(false);
        setFormError('');
    };

    const handleCreatePlan = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        const year = Number(newPlanYear);

        if (!Number.isInteger(year) || year < 2000 || year > 2100) {
            setFormError('Rok planu musi mieścić się w zakresie od 2000 do 2100.');
            return;
        }

        setIsSaving(true);
        setFormError('');

        try {
            await flightPlanService.createPlan(year);
            setIsCreateModalOpen(false);
            await loadPlans();
        } catch (error) {
            setFormError(
                error instanceof Error ? error.message : 'Nie udało się utworzyć planu lotów.'
            );
        } finally {
            setIsSaving(false);
        }
    };

    const handleDeletePlan = (plan: FlightPlanSummaryDto) => {
        setModalConfig({
            isOpen: true,
            title: 'Usuń plan lotów',
            message: `Czy na pewno chcesz usunąć plan lotów na rok ${plan.year}?`,
            isAlert: false,
            onConfirm: async () => {
                closeConfirmModal();

                try {
                    await flightPlanService.deletePlan(plan.year);
                    await loadPlans();
                } catch (error) {
                    showAlert(
                        'Nie udało się usunąć planu',
                        error instanceof Error
                            ? error.message
                            : 'Wystąpił błąd podczas usuwania planu lotów.'
                    );
                }
            }
        });
    };

    return (
        <AuthGuard>
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Navbar />

                <main className="grow max-w-7xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-gray-200 pb-5 mb-8">
                        <div className="flex-1 pr-2">
                            <h1
                                className="text-3xl font-bold text-gray-900"
                                data-cy="flight-plans-title"
                            >
                                Plany i wyniki lotów
                            </h1>

                            <p className="mt-2 text-sm text-gray-600">
                                Wybierz rok, aby wyświetlić plan oraz dostępne wyniki lotów.
                            </p>
                        </div>

                        {canManagePlans && (
                            <button
                                type="button"
                                onClick={openCreateModal}
                                data-cy="add-flight-plan-button"
                                className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold py-2 px-4 rounded-md shadow-sm transition shrink-0 whitespace-nowrap"
                            >
                                + Dodaj plan
                            </button>
                        )}
                    </div>

                    {isLoading ? (
                        <LoadingState />
                    ) : loadError ? (
                        <ErrorState
                            message={loadError}
                            onRetry={() => void loadPlans()}
                        />
                    ) : plans.length === 0 ? (
                        <EmptyState>
                            Brak dostępnych planów lotów.
                        </EmptyState>
                    ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                            {plans.map(plan => (
                                <article
                                    key={plan.id}
                                    data-cy={`flight-plan-card-${plan.year}`}
                                    className="relative bg-white rounded-lg shadow-sm border border-gray-200 hover:shadow-md hover:border-blue-300 transition group"
                                >
                                    <Link href={`/flight-plans/${plan.year}`} className="block p-6 pr-16">
                                        <div className="flex items-center gap-4">
                                            <div className="flex items-center justify-center w-12 h-12 rounded-lg bg-blue-50 text-blue-700 shrink-0">
                                                <svg
                                                    xmlns="http://www.w3.org/2000/svg"
                                                    className="w-7 h-7"
                                                    viewBox="0 -960 960 960"
                                                    fill="currentColor"
                                                    aria-hidden="true"
                                                >
                                                    <path d="M200-80q-33 0-56.5-23.5T120-160v-560q0-33 23.5-56.5T200-800h40v-80h80v80h320v-80h80v80h40q33 0 56.5 23.5T840-720v560q0 33-23.5 56.5T760-80H200Zm0-80h560v-400H200v400Zm0-480h560v-80H200v80Zm0 0v-80 80Z" />
                                                </svg>
                                            </div>

                                            <div>
                                                <h2 className="text-2xl font-bold text-blue-700 group-hover:text-blue-800">
                                                    {plan.year}
                                                </h2>
                                            </div>
                                        </div>
                                    </Link>

                                    {canManagePlans && (
                                        <button
                                            type="button"
                                            onClick={() => handleDeletePlan(plan)}
                                            data-cy={`delete-flight-plan-${plan.year}`}
                                            aria-label={`Usuń plan lotów ${plan.year}`}
                                            title="Usuń plan"
                                            className="absolute top-5 right-5 text-gray-400 hover:text-red-600 p-1 transition-colors"
                                        >
                                            <svg
                                                xmlns="http://www.w3.org/2000/svg"
                                                className="w-5 h-5"
                                                viewBox="0 -960 960 960"
                                                fill="currentColor"
                                                aria-hidden="true"
                                            >
                                                <path d="M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z" />
                                            </svg>
                                        </button>
                                    )}
                                </article>
                            ))}
                        </div>
                    )}
                </main>

                <Footer />

                <Modal
                    isOpen={isCreateModalOpen}
                    title="Dodaj plan lotów"
                    onClose={closeCreateModal}
                    closeDisabled={isSaving}
                >
                    <form
                        onSubmit={handleCreatePlan}
                        className="p-6 space-y-5"
                    >
                        <div>
                            <label
                                htmlFor="flight-plan-year"
                                className="block text-sm font-medium text-gray-700 mb-1"
                            >
                                Rok
                            </label>

                            <input
                                id="flight-plan-year"
                                type="number"
                                min={2000}
                                max={2100}
                                value={newPlanYear}
                                onChange={event => setNewPlanYear(event.target.value)}
                                disabled={isSaving}
                                data-cy="flight-plan-year-input"
                                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                                required
                            />
                        </div>

                        {formError && (
                            <p
                                className="text-sm text-red-600"
                                role="alert"
                                data-cy="flight-plan-form-error"
                            >
                                {formError}
                            </p>
                        )}

                        <div className="flex justify-end gap-3 pt-2">
                            <button
                                type="button"
                                onClick={closeCreateModal}
                                disabled={isSaving}
                                className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
                            >
                                Anuluj
                            </button>

                            <button
                                type="submit"
                                disabled={isSaving}
                                data-cy="save-flight-plan-button"
                                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-sm font-bold transition disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {isSaving ? 'Zapisywanie...' : 'Dodaj plan'}
                            </button>
                        </div>
                    </form>
                </Modal>

                <ConfirmModal
                    isOpen={modalConfig.isOpen}
                    title={modalConfig.title}
                    message={modalConfig.message}
                    isAlert={modalConfig.isAlert}
                    onConfirm={modalConfig.onConfirm}
                    onCancel={closeConfirmModal}
                />
            </div>
        </AuthGuard>
    );
}