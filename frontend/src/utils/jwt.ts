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