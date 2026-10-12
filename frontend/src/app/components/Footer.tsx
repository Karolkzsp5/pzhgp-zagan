export default function Footer() {
    const currentYear = new Date().getFullYear();

    return (
        <footer className="bg-white border-t border-gray-200 mt-auto" data-cy="footer">
            <div className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
                <p className="text-center text-sm text-gray-500" data-cy="footer-copyright">
                    &copy; {currentYear} Karol Kondracki. Wszelkie prawa zastrzeżone.
                </p>
            </div>
        </footer>
    );
}