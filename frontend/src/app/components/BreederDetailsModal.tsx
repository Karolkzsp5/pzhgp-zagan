"use client";

export interface BreederDto {
    id: number;
    name: string;
    surname: string;
    email: string;
    phoneNumber: string;
    dateOfBirth: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
    sectionId: number;
    sectionName: string;
    status: 'PENDING' | 'ACTIVE' | 'BLOCKED';
    createdAt: string;
    role: string;
}

interface BreederDetailsModalProps {
    breeder: BreederDto;
    onClose: () => void;

}

export default function BreederDetailsModal({ breeder, onClose }: BreederDetailsModalProps) {
    return (
        <div data-cy="details-modal" className="fixed inset-0 bg-gray-50/50 backdrop-blur-sm flex items-center justify-center z-50 p-4 transition-opacity">
            <div className="bg-white rounded-lg shadow-2xl max-w-md w-full p-6 relative">

                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition"
                >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>

                <h3 className="text-2xl font-bold text-gray-900 mb-6 border-b pb-2">Dane hodowcy</h3>

                <div className="space-y-4">
                    <div>
                        <p className="text-xs text-gray-500 uppercase tracking-wider">Imię i nazwisko</p>
                        <p className="text-lg font-medium text-gray-900">{breeder.name} {breeder.surname}</p>
                    </div>

                    <div className="grid grid-cols-1 gap-4">
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Email</p>
                            <p className="text-sm font-medium text-gray-800 break-all">{breeder.email}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Telefon</p>
                            <p className="text-sm font-medium text-gray-800">{breeder.phoneNumber}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Data urodzenia</p>
                            <p className="text-sm font-medium text-gray-800">
                                {breeder.dateOfBirth ? breeder.dateOfBirth.split('-').reverse().join('.') : ''}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider mb-1">Adres zamieszkania</p>
                            <p className="text-sm font-medium text-gray-800">
                                ul. {breeder.street} {breeder.houseNumber}
                                <br />
                                {breeder.postalCode} {breeder.city}
                            </p>
                        </div>

                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Sekcja</p>
                            <p className="text-sm font-medium text-gray-800">{breeder.sectionName}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Data rejestracji</p>
                            <p className="text-sm font-medium text-gray-800">
                                {new Date(breeder.createdAt).toLocaleDateString('pl-PL', { day: '2-digit', month: '2-digit', year: 'numeric' })}
                            </p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Rola w systemie</p>
                            <p className="text-sm font-bold text-gray-700">{breeder.role}</p>
                        </div>
                        <div>
                            <p className="text-xs text-gray-500 uppercase tracking-wider">Obecny status</p>
                            <p className={`text-sm font-bold ${breeder.status === 'ACTIVE' ? 'text-green-600' : breeder.status === 'BLOCKED' ? 'text-red-600' : 'text-yellow-600'}`}>
                                {breeder.status}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="mt-8 flex justify-end">
                    <button
                        onClick={onClose}
                        className="bg-blue-600 hover:bg-blue-700 text-white font-medium px-6 py-2 rounded-md transition shadow-sm"
                    >
                        Zamknij
                    </button>
                </div>
            </div>
        </div>
    );
}