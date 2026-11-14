'use client';

import { ReactNode, useCallback, useEffect, useState } from 'react';
import AuthGuard from '@/app/components/AuthGuard';
import { useAuth } from '@/app/components/AuthProvider';
import ConfirmModal from '@/app/components/ConfirmModal';
import ErrorState from '@/app/components/ErrorState';
import LoadingState from '@/app/components/LoadingState';
import useConfirmDialog from '@/app/hooks/useConfirmDialog';
import { accountService } from '@/app/services/accountService';
import { AccountDto } from '@/app/types/account';
import { getErrorMessage } from '@/app/utils/apiClient';
import { invalidateAuthSession } from '@/app/utils/authSession';
import { downloadFile } from '@/app/utils/download';
import { formatAccountAge, formatInstantDate, formatLocalDate, formatRole } from '@/app/utils/formatters';
import ContactDetailsForm from './ContactDetailsForm';
import PasswordChangeForm from './PasswordChangeForm';

function SettingsSection({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
    return (
        <section className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-bold text-gray-900">{title}</h2>
            {description && <p className="mt-1 text-sm text-gray-600">{description}</p>}
            <div className="mt-5">{children}</div>
        </section>
    );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
    return (
        <div>
            <dt className="text-xs text-gray-500 uppercase tracking-wider">{label}</dt>
            <dd className="mt-1 text-sm font-medium text-gray-800">{children}</dd>
        </div>
    );
}

/**
 * Serwer usunął już ciasteczko sesji. Pozostałe karty dostają sygnał wylogowania, a pełne przeładowanie
 * na stronę logowania czyści stan tej karty.
 */
const goToLogin = (notice: 'password-changed' | 'logged-out-everywhere') => {
    invalidateAuthSession(false);
    window.location.replace(`/login?notice=${notice}`);
};

export default function SettingsPage() {
    const { isLoggedIn } = useAuth();
    const [account, setAccount] = useState<AccountDto | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [isExporting, setIsExporting] = useState(false);

    const { dialog, showAlert, showConfirm } = useConfirmDialog();

    const loadAccount = useCallback(async () => {
        setIsLoading(true);
        setError('');

        try {
            setAccount(await accountService.getAccount());
        } catch (error) {
            setError(getErrorMessage(error, 'Nie udało się pobrać danych konta.'));
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        if (!isLoggedIn) return;

        void loadAccount();
    }, [isLoggedIn, loadAccount]);

    const handleLogoutAllDevices = () => {
        showConfirm({
            title: 'Wyloguj ze wszystkich urządzeń',
            message: 'Zostaniesz wylogowany na wszystkich urządzeniach, także na tym. Aby dalej korzystać z serwisu, zaloguj się ponownie.',
            onConfirm: async () => {
                try {
                    await accountService.logoutAllDevices();
                    goToLogin('logged-out-everywhere');
                } catch (error) {
                    showAlert('Błąd', getErrorMessage(error, 'Nie udało się wylogować ze wszystkich urządzeń.'));
                }
            }
        });
    };

    const handleExport = async () => {
        setIsExporting(true);

        try {
            const data = await accountService.exportData();
            const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
            downloadFile(blob, `pzhgp-moje-dane-${new Date().toISOString().slice(0, 10)}.json`);
        } catch (error) {
            showAlert('Nie udało się pobrać danych', getErrorMessage(error, 'Wystąpił błąd podczas przygotowywania pliku z danymi.'));
        } finally {
            setIsExporting(false);
        }
    };

    return (
        <AuthGuard>
            <>
                <main className="grow max-w-3xl mx-auto w-full py-10 px-4 sm:px-6 lg:px-8">
                    <div className="border-b border-gray-200 pb-5 mb-8">
                        <h1 className="text-3xl font-bold text-gray-900">Ustawienia</h1>
                        <p className="mt-2 text-sm text-gray-600">
                            Twoje dane, bezpieczeństwo konta i kopia danych zapisanych w serwisie.
                        </p>
                    </div>

                    {isLoading ? (
                        <LoadingState />
                    ) : error || !account ? (
                        <ErrorState message={error || 'Nie udało się pobrać danych konta.'} onRetry={() => void loadAccount()} />
                    ) : (
                        <div className="space-y-6">
                            <SettingsSection
                                title="Dane osobowe"
                                description="Te dane zweryfikował zarząd przy zakładaniu konta. Aby je zmienić, skontaktuj się z administratorem."
                            >
                                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                    <Field label="Imię i nazwisko">{account.name} {account.surname}</Field>
                                    <Field label="Data urodzenia">{formatLocalDate(account.dateOfBirth)}</Field>
                                    <Field label="Adres e-mail"><span className="break-all">{account.email}</span></Field>
                                    <Field label="Sekcja">{account.sectionName}</Field>
                                </dl>
                            </SettingsSection>

                            <SettingsSection title="Dane kontaktowe" description="Numer telefonu i adres możesz zmienić samodzielnie.">
                                <ContactDetailsForm account={account} onSaved={setAccount} />
                            </SettingsSection>

                            <SettingsSection title="Konto">
                                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                    <Field label="Rola">{formatRole(account.role)}</Field>
                                    <Field label="Konto założone">{formatInstantDate(account.createdAt)}</Field>
                                    <Field label="Staż w serwisie">{formatAccountAge(account.createdAt)}</Field>
                                    <Field label="Funkcja w zarządzie">
                                        {account.boardFunctions.length === 0 ? (
                                            'Brak'
                                        ) : (
                                            <ul className="space-y-1">
                                                {account.boardFunctions.map(boardFunction => <li key={boardFunction}>{boardFunction}</li>)}
                                            </ul>
                                        )}
                                    </Field>
                                </dl>
                            </SettingsSection>

                            <SettingsSection
                                title="Zmiana hasła"
                                description="Po zmianie hasła zostaniesz wylogowany ze wszystkich urządzeń, a na Twój adres e-mail wyślemy potwierdzenie."
                            >
                                <PasswordChangeForm onPasswordChanged={() => goToLogin('password-changed')} />
                            </SettingsSection>

                            <SettingsSection
                                title="Sesje"
                                description="Jeśli logowałeś się na cudzym komputerze albo zgubiłeś telefon, zakończ wszystkie sesje swojego konta."
                            >
                                <button
                                    type="button"
                                    onClick={handleLogoutAllDevices}
                                    className="px-4 py-2 border border-red-200 bg-white text-red-700 rounded-md text-sm font-bold hover:bg-red-50 transition"
                                >
                                    Wyloguj ze wszystkich urządzeń
                                </button>
                            </SettingsSection>

                            <SettingsSection
                                title="Twoje dane"
                                description="Pobierz plik JSON z danymi konta oraz z wątkami, wpisami, ogłoszeniami i lotami, które dodałeś w serwisie."
                            >
                                <button
                                    type="button"
                                    onClick={() => void handleExport()}
                                    disabled={isExporting}
                                    className="px-4 py-2 border border-gray-300 bg-white text-gray-700 rounded-md text-sm font-bold hover:bg-gray-100 transition disabled:opacity-50 disabled:cursor-wait"
                                >
                                    {isExporting ? 'Przygotowywanie pliku...' : 'Pobierz moje dane'}
                                </button>
                            </SettingsSection>
                        </div>
                    )}
                </main>

                <ConfirmModal {...dialog} confirmLabel={dialog.isAlert ? undefined : 'Wyloguj'} />
            </>
        </AuthGuard>
    );
}