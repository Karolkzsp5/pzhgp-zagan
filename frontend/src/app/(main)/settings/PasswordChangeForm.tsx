'use client';

import { FormEvent, useState } from 'react';
import PasswordStrengthMeter from '@/app/components/PasswordStrengthMeter';
import { accountService } from '@/app/services/accountService';
import { getErrorMessage } from '@/app/utils/apiClient';
import { validateStrongPassword } from '@/app/utils/password';

interface PasswordChangeFormProps {
    /** Wywoływane po zmianie hasła; backend zakończył już wtedy wszystkie sesje konta. */
    onPasswordChanged: () => void;
}

const INPUT_CLASS = 'mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 outline-none transition focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100';

export default function PasswordChangeForm({ onPasswordChanged }: PasswordChangeFormProps) {
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const [isSaving, setIsSaving] = useState(false);

    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError('');

        if (!currentPassword) {
            setError('Podaj obecne hasło.');
            return;
        }

        const passwordError = validateStrongPassword(newPassword);
        if (passwordError) {
            setError(passwordError);
            return;
        }

        if (newPassword !== confirmPassword) {
            setError('Podane hasła nie są identyczne.');
            return;
        }

        if (newPassword === currentPassword) {
            setError('Nowe hasło musi się różnić od obecnego.');
            return;
        }

        setIsSaving(true);
        try {
            await accountService.changePassword({ currentPassword, newPassword });
            onPasswordChanged();
        } catch (error) {
            setError(getErrorMessage(error, 'Nie udało się zmienić hasła.'));
            setIsSaving(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <div>
                <label htmlFor="current-password" className="block text-sm font-medium text-gray-700">Obecne hasło</label>
                <input id="current-password" type="password" autoComplete="current-password"
                       value={currentPassword}
                       onChange={(e) => setCurrentPassword(e.target.value)}
                       disabled={isSaving}
                       className={INPUT_CLASS} />
            </div>

            <div>
                <label htmlFor="new-password" className="block text-sm font-medium text-gray-700">Nowe hasło</label>
                <input id="new-password" type="password" autoComplete="new-password"
                       value={newPassword}
                       onChange={(e) => setNewPassword(e.target.value)}
                       disabled={isSaving}
                       className={INPUT_CLASS} />

                <PasswordStrengthMeter password={newPassword} />
            </div>

            <div>
                <label htmlFor="confirm-new-password" className="block text-sm font-medium text-gray-700">Powtórz nowe hasło</label>
                <input id="confirm-new-password" type="password" autoComplete="new-password"
                       value={confirmPassword}
                       onChange={(e) => setConfirmPassword(e.target.value)}
                       disabled={isSaving}
                       className={INPUT_CLASS} />
            </div>

            {error && (
                <p role="alert" className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">
                    {error}
                </p>
            )}

            <div className="flex justify-end">
                <button
                    type="submit"
                    disabled={isSaving}
                    className="px-5 py-2 bg-blue-600 text-white rounded-md text-sm font-bold hover:bg-blue-700 transition disabled:bg-blue-400 disabled:cursor-wait"
                >
                    {isSaving ? 'Zapisywanie...' : 'Zmień hasło'}
                </button>
            </div>
        </form>
    );
}