export default function LoadingState() {
    return (
        <div className="flex justify-center items-center py-16" role="status" aria-label="Ładowanie">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-700"></div>
        </div>
    );
}