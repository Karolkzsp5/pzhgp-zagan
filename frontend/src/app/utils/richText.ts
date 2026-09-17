export const isHtmlEmpty = (html: string): boolean => {
    if (!html) return true;

    const document = new DOMParser().parseFromString(html, 'text/html');

    return !(document.body.textContent || '')
        .replace(/\u00a0/g, ' ')
        .trim();
};