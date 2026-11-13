import { AccountStatus } from '@/app/types/breeder';
import { UserRole } from '@/app/utils/authSession';

/** Data i godzina (bez sekund) w czasie lokalnym, np. "04.11.2026, 14:05". */
export const formatDateTime = (dateString: string | null) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit' });
};

/**
 * Data i godzina z sekundami w czasie lokalnym.
 * Pliki z obrączek zapisują czas w UTC, a hodowca oczekuje godziny "z zegarka".
 */
export const formatDateTimeWithSeconds = (isoTime: string | null): string => {
    if (!isoTime) return '—';
    return new Date(isoTime).toLocaleString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
};

/** Sama data z momentu w czasie (Instant z backendu) w strefie przeglądarki, np. "04.11.2026". */
export const formatInstantDate = (dateString: string | null) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric' });
};

/** Data kalendarzowa bez strefy czasowej (LocalDate z backendu, np. "2026-05-17") zapisana jako "17.05.2026". */
export const formatLocalDate = (dateString: string | null) => {
    if (!dateString) return '—';
    const [year, month, day] = dateString.split('-');
    return year && month && day ? `${day}.${month}.${year}` : dateString;
};

/** Sama godzina z sekundami w czasie lokalnym. */
export const formatTime = (isoTime: string | null): string => {
    if (!isoTime) return '—';
    return new Date(isoTime).toLocaleTimeString('pl-PL', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
};

/** Formatuje czas trwania w sekundach jako "2 godz. 13 min" lub "13 min 38 s". */
export const formatDuration = (seconds: number): string => {
    if (!seconds || seconds <= 0) return '—';

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const rest = Math.floor(seconds % 60);

    if (hours > 0) return `${hours} godz. ${minutes} min`;
    if (minutes > 0) return `${minutes} min ${rest} s`;
    return `${rest} s`;
};

/** Formatuje prędkość w metrach na minutę, np. "1 378 m/min". */
export const formatSpeed = (metersPerMinute: number | null): string => {
    if (metersPerMinute === null || !Number.isFinite(metersPerMinute) || metersPerMinute <= 0) return '—';
    return `${Math.round(metersPerMinute).toLocaleString('pl-PL')} m/min`;
};

export const formatPhoneNumber = (phone: string | null) => {
    if (!phone) return '—';
    const digits = phone.replace(/\D/g, '');
    return digits.length === 9 ? `${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6)}` : phone;
};

export const formatPhoneInput = (value: string) => {
    const digits = value.replace(/\D/g, '').slice(0, 9);
    return digits.replace(/(\d{3})(?=\d)/g, '$1 ').trim();
};

// Pola adresu wspólne dla rejestracji i ustawień konta. Te same reguły sprawdza backend.

/** Numer domu lub lokalu, np. 12, 12A, 12/4. */
export const HOUSE_NUMBER_PATTERN = /^[1-9]\d*\s?[a-zA-Z]?(\s?[\/-]\s?[1-9]\d*\s?[a-zA-Z]?)?$/;

/** Kod pocztowy w formacie XX-XXX; myślnik dopisuje się sam, a przy kasowaniu znika razem z cyfrą. */
export const formatPostalCodeInput = (val: string, prevVal: string) => {
    if (val.length < prevVal.length) {
        if (val.endsWith('-')) {
            return val.slice(0, -1);
        }
        return val;
    }

    const digits = val.replace(/\D/g, '').slice(0, 5);

    if (digits.length >= 2) {
        return `${digits.slice(0, 2)}-${digits.slice(2)}`;
    }
    return digits;
};

/** Imię, nazwisko lub miejscowość: litery z dowolnego alfabetu (np. Müller), spacje, myślniki i apostrofy (np. O'Connor). */
export const formatNameInput = (value: string) => {
    return value.replace(/[^\p{L}\p{M}\s'’-]/gu, '');
};

export const formatStreetInput = (value: string) => {
    return value.replace(/[^0-9a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\s./-]/g, '');
};

export const formatHouseNumberInput = (value: string) => {
    return value.replace(/[^0-9a-zA-Z\s\/-]/g, '');
};

/** Odmiana liczebnika, np. 1 rok, 2 lata, 5 lat. */
const pluralize = (count: number, one: string, few: string, many: string) => {
    const lastTwoDigits = count % 100;
    const lastDigit = count % 10;

    if (count === 1) return one;
    if (lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14)) return few;
    return many;
};

/**
 * Staż konta liczony w dniach kalendarzowych, np. "1 rok, 8 miesięcy i 3 dni".
 * Konto założone dzisiaj daje "mniej niż dzień".
 */
export const formatAccountAge = (createdAt: string, now: Date = new Date()): string => {
    const created = new Date(createdAt);

    let years = now.getFullYear() - created.getFullYear();
    let months = now.getMonth() - created.getMonth();

    if (now.getDate() < created.getDate()) {
        months -= 1;
    }

    if (months < 0) {
        years -= 1;
        months += 12;
    }

    // Data założenia przesunięta o pełne lata i miesiące; dzień miesiąca nie wychodzi poza jego koniec
    // (31 stycznia + 1 miesiąc = 28 lutego). Pozostała różnica to pełne dni kalendarzowe.
    const targetMonth = created.getMonth() + months;
    const targetYear = created.getFullYear() + years + Math.floor(targetMonth / 12);
    const daysInTargetMonth = new Date(targetYear, (targetMonth % 12) + 1, 0).getDate();
    const anniversary = new Date(targetYear, targetMonth % 12, Math.min(created.getDate(), daysInTargetMonth));
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const days = Math.round((today.getTime() - anniversary.getTime()) / 86_400_000);

    const parts = [
        years > 0 ? `${years} ${pluralize(years, 'rok', 'lata', 'lat')}` : null,
        months > 0 ? `${months} ${pluralize(months, 'miesiąc', 'miesiące', 'miesięcy')}` : null,
        days > 0 ? `${days} ${days === 1 ? 'dzień' : 'dni'}` : null
    ].filter((part): part is string => part !== null);

    if (parts.length === 0) return 'mniej niż dzień';
    if (parts.length === 1) return parts[0];
    return `${parts.slice(0, -1).join(', ')} i ${parts[parts.length - 1]}`;
};

const ROLE_LABELS: Record<UserRole, string> = {
    ADMINISTRATOR: 'Administrator',
    MODERATOR: 'Moderator',
    BREEDER: 'Hodowca'
};

const STATUS_LABELS: Record<AccountStatus, string> = {
    ACTIVE: 'Aktywny',
    BLOCKED: 'Zablokowany',
    PENDING: 'Oczekujący'
};

export const formatRole = (role: UserRole) => ROLE_LABELS[role] ?? role;
export const formatAccountStatus = (status: AccountStatus) => STATUS_LABELS[status] ?? status;