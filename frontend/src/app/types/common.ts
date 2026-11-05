/** Strona wyników zwracana przez endpointy ze stronicowaniem (rekord PageResponse w backendzie). */
export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}
