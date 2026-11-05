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

export const formatDate = (dateString: string | null) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric' });
};

/** Data bez strefy czasowej (np. "2026-05-17" z backendu) zapisana jako "17.05.2026". */
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

const ROLE_LABELS: Record<string, string> = {
    ADMINISTRATOR: 'Administrator',
    MODERATOR: 'Moderator',
    BREEDER: 'Hodowca'
};

const STATUS_LABELS: Record<string, string> = {
    ACTIVE: 'Aktywny',
    BLOCKED: 'Zablokowany',
    PENDING: 'Oczekujący'
};

export const formatRole = (role: string) => ROLE_LABELS[role] ?? role;
export const formatAccountStatus = (status: string) => STATUS_LABELS[status] ?? status;