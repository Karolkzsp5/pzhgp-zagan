// Reguły hasła wspólne dla rejestracji i zmiany hasła. Te same reguły sprawdza backend.

// Znak specjalny to każdy znak, który nie jest literą ani cyfrą (np. ! ; ' [ ] ~ lub spacja), tak samo jak w backendzie.
const SPECIAL_CHARACTER = /[^\p{L}\p{N}]/u;

/** Ocena siły hasła: po jednym punkcie za długość, dużą literę, małą literę, cyfrę i znak specjalny. */
export const getPasswordStrength = (pass: string) => {
    let score = 0;
    if (!pass) return { score: 0, label: '', widthClass: 'w-0', colorClass: 'bg-transparent' };

    if (pass.length >= 8) score += 1;
    if (/[A-ZĄĆĘŁŃÓŚŹŻ]/.test(pass)) score += 1;
    if (/[a-ząćęłńóśźż]/.test(pass)) score += 1;
    if (/\d/.test(pass)) score += 1;
    if (SPECIAL_CHARACTER.test(pass)) score += 1;

    switch (score) {
        case 1:
            return { score, label: 'Bardzo słabe', widthClass: 'w-1/5', colorClass: 'bg-red-500' };
        case 2:
            return { score, label: 'Słabe', widthClass: 'w-2/5', colorClass: 'bg-red-500' };
        case 3:
            return { score, label: 'Średnie', widthClass: 'w-3/5', colorClass: 'bg-yellow-500' };
        case 4:
            return { score, label: 'Dobre', widthClass: 'w-4/5', colorClass: 'bg-blue-500' };
        case 5:
            return { score, label: 'Bardzo silne', widthClass: 'w-full', colorClass: 'bg-green-500' };
        default:
            return { score: 0, label: '', widthClass: 'w-0', colorClass: 'bg-transparent' };
    }
};

/** Komunikat o pierwszej niespełnionej regule hasła albo null, gdy hasło jest poprawne. */
export const validateStrongPassword = (pass: string): string | null => {
    if (pass.length < 8) return 'Hasło musi zawierać co najmniej 8 znaków.';
    if (!/[A-ZĄĆĘŁŃÓŚŹŻ]/.test(pass)) return 'Hasło musi zawierać co najmniej jedną dużą literę.';
    if (!/[a-ząćęłńóśźż]/.test(pass)) return 'Hasło musi zawierać co najmniej jedną małą literę.';
    if (!/\d/.test(pass)) return 'Hasło musi zawierać co najmniej jedną cyfrę.';
    if (!SPECIAL_CHARACTER.test(pass)) return 'Hasło musi zawierać co najmniej jeden znak specjalny.';
    if (pass.length > 72 || new TextEncoder().encode(pass).length > 72) return 'Hasło może mieć maksymalnie 72 bajty (UTF-8).';
    return null;
};