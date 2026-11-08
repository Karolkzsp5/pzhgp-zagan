interface PaginationProps {
    /** Numer bieżącej strony liczony od zera, tak jak w odpowiedziach API. */
    page: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    disabled?: boolean;
    className?: string;
}

const BUTTON_CLASS = 'px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition';

/** Przyciski "Poprzednia" i "Następna" z numerem strony. Przy jednej stronie nic nie wyświetla. */
export default function Pagination({ page, totalPages, onPageChange, disabled = false, className }: PaginationProps) {
    if (totalPages <= 1) return null;

    return (
        <div className={`flex justify-center items-center gap-4 ${className ?? ''}`.trim()}>
            <button
                type="button"
                onClick={() => onPageChange(page - 1)}
                disabled={disabled || page <= 0}
                className={BUTTON_CLASS}
            >
                Poprzednia
            </button>

            <span className="text-sm text-gray-600 font-medium">
                Strona {page + 1} z {totalPages}
            </span>

            <button
                type="button"
                onClick={() => onPageChange(page + 1)}
                disabled={disabled || page >= totalPages - 1}
                className={BUTTON_CLASS}
            >
                Następna
            </button>
        </div>
    );
}
