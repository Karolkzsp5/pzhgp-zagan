import { getPasswordStrength } from '@/app/utils/password';

/** Pasek siły hasła wyświetlany pod polem hasła. Dla pustego hasła nic nie pokazuje. */
export default function PasswordStrengthMeter({ password }: { password: string }) {
    if (password.length === 0) return null;

    const passwordStrength = getPasswordStrength(password);

    return (
        <div className="mt-2">
            <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                <div className={`h-1.5 rounded-full transition-all duration-300 ${passwordStrength.widthClass} ${passwordStrength.colorClass}`} />
            </div>
            <p className="text-xs text-gray-600 mt-1 flex justify-between">
                <span>Siła hasła: <strong className="text-gray-800">{passwordStrength.label}</strong></span>
                <span className="text-gray-400">{passwordStrength.score}/5</span>
            </p>
        </div>
    );
}