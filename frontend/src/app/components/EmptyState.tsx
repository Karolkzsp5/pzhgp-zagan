export default function EmptyState({ children }: { children: React.ReactNode }) {
    return <div className="bg-white rounded-lg border border-gray-200 p-10 text-center text-sm text-gray-600">{children}</div>;
}