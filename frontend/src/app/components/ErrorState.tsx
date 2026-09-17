interface ErrorStateProps {
    message: string;
    onRetry?: () => void;
}

export default function ErrorState({ message, onRetry }: ErrorStateProps) {
    return (
        <div className="bg-red-50 border border-red-200 rounded-lg p-8 text-center">
            <p className="text-sm text-red-700">{message}</p>
            {onRetry &&
                <button
                    type="button"
                    onClick={onRetry}
                    className="mt-4 px-4 py-2 border border-red-200 rounded-md text-sm font-medium text-red-700 hover:bg-red-100">
                    Spróbuj ponownie
                </button>
            }
        </div>
    );
}