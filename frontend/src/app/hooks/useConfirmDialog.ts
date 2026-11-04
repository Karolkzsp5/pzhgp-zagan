import { useCallback, useState } from 'react';

interface ConfirmOptions {
    title: string;
    message: string;
    /** Akcja uruchamiana po zatwierdzeniu; okno jest wtedy już zamknięte. */
    onConfirm: () => void | Promise<void>;
}

interface DialogState {
    isOpen: boolean;
    title: string;
    message: string;
    isAlert: boolean;
    onConfirm: () => void;
}

const CLOSED_DIALOG: DialogState = {
    isOpen: false,
    title: '',
    message: '',
    isAlert: false,
    onConfirm: () => {}
};

/**
 * Stan okna ConfirmModal: komunikat z przyciskiem OK (showAlert) albo pytanie o potwierdzenie (showConfirm).
 * Obiekt `dialog` zawiera gotowe właściwości komponentu: `<ConfirmModal {...dialog} />`.
 */
export default function useConfirmDialog() {
    const [state, setState] = useState<DialogState>(CLOSED_DIALOG);

    const closeDialog = useCallback(() => {
        setState((previous) => ({ ...previous, isOpen: false }));
    }, []);

    const showAlert = useCallback(
        (title: string, message: string) => {
            setState({ isOpen: true, title, message, isAlert: true, onConfirm: closeDialog });
        },
        [closeDialog]
    );

    const showConfirm = useCallback(
        ({ title, message, onConfirm }: ConfirmOptions) => {
            setState({
                isOpen: true,
                title,
                message,
                isAlert: false,
                onConfirm: () => {
                    closeDialog();
                    void onConfirm();
                }
            });
        },
        [closeDialog]
    );

    return {
        dialog: { ...state, onCancel: closeDialog },
        showAlert,
        showConfirm
    };
}
