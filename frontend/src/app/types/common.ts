/** Strona wyników zwracana przez endpointy ze stronicowaniem (Spring Data Page). */
export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}
