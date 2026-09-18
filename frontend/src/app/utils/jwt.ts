export const getAuthToken = () => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
};

export const decodeJwt = (token: string) => {
    try {
        const payloadBase64 = token.split('.')[1];
        const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
        const decodedJson = decodeURIComponent(
            atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join('')
        );
        return JSON.parse(decodedJson);
    } catch (error) {
        console.error("Błąd dekodowania tokenu JWT:", error);
        return null;
    }
};

export const isJwtValid = (token: string | null): boolean => {
    if (!token) return false;
    const payload = decodeJwt(token);
    if (!payload) return false;
    if (!payload.exp) return true;
    return payload.exp * 1000 > Date.now();
};

export const logout = () => {
    if (typeof window !== 'undefined') {
        localStorage.removeItem('jwt_token');
        sessionStorage.removeItem('jwt_token');
    }
};

export const getUserRole = (token: string | null): string | null => {
    if (!isJwtValid(token)) return null;
    const payload = decodeJwt(token!);
    return payload?.role || null;
};