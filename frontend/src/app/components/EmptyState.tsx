import { ReactNode } from 'react';

export default function EmptyState({ children }: { children: ReactNode }) {
    return (
        <div className="bg-white rounded-lg border border-gray-200 p-10 text-center text-sm text-gray-600">
            {children}
        </div>
    );
}
