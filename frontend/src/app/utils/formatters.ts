export const formatGlobalDate = (dateString: string | null) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit' });
};

export const formatDate = (dateString: string | null) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('pl-PL', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric' });
};

export const formatLocalDate = (dateString: string | null) => {
    if (!dateString) return '—';
    const [year, month, day] = dateString.split('-');
    return year && month && day ? `${day}.${month}.${year}` : dateString;
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