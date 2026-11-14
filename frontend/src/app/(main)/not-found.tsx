import NotFoundContent from '@/app/components/NotFoundContent';

/** Strona 404 dla podstron, które wywołały notFound(), np. przy nieprawidłowym identyfikatorze w adresie. Navbar i stopkę daje layout (main). */
export default function MainNotFound() {
    return <NotFoundContent />;
}
