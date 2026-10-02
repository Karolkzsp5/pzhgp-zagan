"use client";

import { FormEvent, useEffect, useState } from 'react';
import Modal from '@/app/components/Modal';
import { flightPlanService } from '@/app/services/flightPlanService';
import {FlightPlanEntryDto, FlightPlanEntryRequest, PigeonAgeGroup} from '@/app/types/flightPlan';

interface FlightPlanEntryModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSaved: () => void | Promise<void>;
    year: number;
    pigeonAgeGroup: PigeonAgeGroup;
    defaultSortOrder: number;
    entryToEdit: FlightPlanEntryDto | null;
}

export default function FlightPlanEntryModal({isOpen, onClose, onSaved, year, pigeonAgeGroup, defaultSortOrder, entryToEdit}: FlightPlanEntryModalProps) {
    const [scheduledDate, setScheduledDate] = useState('');
    const [location, setLocation] = useState('');
    const [distanceKm, setDistanceKm] = useState('');
    const [category, setCategory] = useState('');
    const [listType, setListType] = useState('');
    const [sortOrder, setSortOrder] = useState('');

    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState('');

    const isEditing = entryToEdit !== null;

    useEffect(() => {
        if (!isOpen) return;

        if (entryToEdit) {
            setScheduledDate(entryToEdit.scheduledDate);
            setLocation(entryToEdit.location);
            setDistanceKm(String(entryToEdit.distanceKm));
            setCategory(entryToEdit.category ?? '');
            setListType(entryToEdit.listType);
            setSortOrder(String(entryToEdit.sortOrder));
        } else {
            setScheduledDate('');
            setLocation('');
            setDistanceKm('');
            setCategory('');
            setListType('');
            setSortOrder(String(defaultSortOrder));
        }

        setError('');
    }, [isOpen, entryToEdit, defaultSortOrder]);

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        const parsedDistance = Number(distanceKm);
        const parsedSortOrder = Number(sortOrder);

        if (!scheduledDate) {
            setError('Data planowanego lotu jest wymagana.');
            return;
        }

        if (!location.trim()) {
            setError('Miejscowość jest wymagana.');
            return;
        }

        if (!Number.isInteger(parsedDistance) || parsedDistance < 1 || parsedDistance > 3000) {
            setError('Dystans musi mieścić się w zakresie od 1 do 3000 km.');
            return;
        }

        if (!listType.trim()) {
            setError('Rodzaj listy jest wymagany.');
            return;
        }

        if (!Number.isInteger(parsedSortOrder) || parsedSortOrder < 1) {
            setError('Kolejność lotu musi wynosić co najmniej 1.');
            return;
        }

        const request: FlightPlanEntryRequest = {
            pigeonAgeGroup,
            scheduledDate,
            location: location.trim(),
            distanceKm: parsedDistance,
            category: category.trim() || null,
            listType: listType.trim(),
            sortOrder: parsedSortOrder
        };

        setIsSaving(true);
        setError('');

        try {
            if (entryToEdit) {
                await flightPlanService.updateEntry(entryToEdit.id, request);
            } else {
                await flightPlanService.addEntry(year, request);
            }

            await onSaved();
            onClose();
        } catch (error) {
            setError(error instanceof Error ? error.message : 'Nie udało się zapisać lotu.');
        } finally {
            setIsSaving(false);
        }
    };

    const ageGroupLabel = pigeonAgeGroup === 'ADULT' ? 'Gołębie dorosłe' : 'Gołębie młode';

    return (
        <Modal
            isOpen={isOpen}
            title={isEditing ? 'Edytuj lot' : 'Dodaj lot'}
            onClose={onClose}
            closeDisabled={isSaving}
            maxWidthClass="max-w-lg"
        >
            <form onSubmit={handleSubmit} className="p-6 space-y-5">
                <div>
                    <span className="block text-sm font-medium text-gray-700 mb-1">
                        Grupa
                    </span>

                    <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-700">
                        {ageGroupLabel}
                    </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <label htmlFor="flight-date" className="block text-sm font-medium text-gray-700 mb-1">
                            Data
                        </label>

                        <input
                            id="flight-date"
                            type="date"
                            min={`${year}-01-01`}
                            max={`${year}-12-31`}
                            value={scheduledDate}
                            onChange={event => setScheduledDate(event.target.value)}
                            disabled={isSaving}
                            required
                            className="w-full px-3 py-2 border border-gray-300 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                        />
                    </div>

                    <div>
                        <label htmlFor="flight-sort-order" className="block text-sm font-medium text-gray-700 mb-1">
                            Lp.
                        </label>

                        <input
                            id="flight-sort-order"
                            type="number"
                            min={1}
                            value={sortOrder}
                            onChange={event => setSortOrder(event.target.value)}
                            disabled={isSaving}
                            required
                            className="w-full px-3 py-2 border border-gray-300 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                        />
                    </div>
                </div>

                <div>
                    <label htmlFor="flight-location" className="block text-sm font-medium text-gray-700 mb-1">
                        Miejscowość
                    </label>

                    <input
                        id="flight-location"
                        type="text"
                        maxLength={100}
                        value={location}
                        onChange={event => setLocation(event.target.value)}
                        disabled={isSaving}
                        required
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                    />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                        <label htmlFor="flight-distance" className="block text-sm font-medium text-gray-700 mb-1">
                            Dystans (km)
                        </label>

                        <input
                            id="flight-distance"
                            type="number"
                            min={1}
                            max={3000}
                            value={distanceKm}
                            onChange={event => setDistanceKm(event.target.value)}
                            disabled={isSaving}
                            required
                            className="w-full px-3 py-2 border border-gray-300 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                        />
                    </div>

                    <div>
                        <label htmlFor="flight-category" className="block text-sm font-medium text-gray-700 mb-1">
                            Kategoria
                            <span className="ml-1 text-gray-400 font-normal">
                                (opcjonalna)
                            </span>
                        </label>

                        <input
                            id="flight-category"
                            type="text"
                            maxLength={30}
                            value={category}
                            onChange={event => setCategory(event.target.value)}
                            disabled={isSaving}
                            placeholder="np. A/B"
                            className="w-full px-3 py-2 border border-gray-300 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                        />
                    </div>
                </div>

                <div>
                    <label htmlFor="flight-list-type" className="block text-sm font-medium text-gray-700 mb-1">
                        Rodzaj listy
                    </label>

                    <input
                        id="flight-list-type"
                        type="text"
                        maxLength={100}
                        value={listType}
                        onChange={event => setListType(event.target.value)}
                        disabled={isSaving}
                        placeholder="np. Oddziałowa"
                        required
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
                    />
                </div>

                {error && (
                    <p className="text-sm text-red-600" role="alert">
                        {error}
                    </p>
                )}

                <div className="flex justify-end gap-3 pt-2">
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={isSaving}
                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition disabled:opacity-50"
                    >
                        Anuluj
                    </button>

                    <button
                        type="submit"
                        disabled={isSaving}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-sm font-bold transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isSaving ? 'Zapisywanie...' : isEditing ? 'Zapisz zmiany' : 'Dodaj lot'}
                    </button>
                </div>
            </form>
        </Modal>
    );
}