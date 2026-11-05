/** Dane logowania wysyłane do /api/auth/login. */
export interface LoginRequest {
    email: string;
    password: string;
    rememberMe: boolean;
}

/** Dane konta wysyłane do /api/auth/register. */
export interface RegistrationRequest {
    name: string;
    surname: string;
    dateOfBirth: string;
    postalCode: string;
    city: string;
    street: string;
    houseNumber: string;
    sectionId: number;
    email: string;
    phoneNumber: string;
    password: string;
}
