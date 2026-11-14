/** Zapisuje plik na dysku użytkownika przez tymczasowy link do obiektu Blob. */
export const downloadFile = (blob: Blob, fileName: string) => {
    const objectUrl = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();

    window.setTimeout(() => {
        URL.revokeObjectURL(objectUrl);
    }, 60_000);
};
