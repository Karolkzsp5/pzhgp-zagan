package com.pzhgp.backend.service;

import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.exception.ForbiddenOperationException;

/**
 * Reguły uprawnień wspólne dla kilku serwisów.
 * <p>
 * Na forum obowiązuje hierarchia ról: administrator może usuwać treści moderatorów i hodowców,
 * moderator — wyłącznie treści hodowców, a każdy autor może usunąć to, co sam napisał.
 */
final class PermissionRules {

    private PermissionRules() {}

    /**
     * Zwraca użytkownika, jeżeli jest administratorem.
     *
     * @throws ForbiddenOperationException z podanym komunikatem, gdy użytkownik nie jest administratorem
     */
    static Breeder requireAdministrator(Breeder user, String deniedMessage) {
        if (user.getRole() != Role.ADMINISTRATOR) {
            throw new ForbiddenOperationException(deniedMessage);
        }

        return user;
    }

    /** Wątek lub wpis na forum może edytować wyłącznie jego autor. */
    static boolean canEditForumContent(Breeder author, Breeder requester) {
        return author.getId().equals(requester.getId());
    }

    static boolean canDeleteForumContent(Breeder author, Breeder requester) {
        if (author.getId().equals(requester.getId())) {
            return true;
        }

        if (requester.getRole() == Role.ADMINISTRATOR) {
            return author.getRole() != Role.ADMINISTRATOR;
        }

        if (requester.getRole() == Role.MODERATOR) {
            return author.getRole() == Role.BREEDER;
        }

        return false;
    }

    /** Zamykanie i przypinanie wątków. */
    static boolean canModerateForumContent(Breeder author, Breeder requester) {
        if (requester.getRole() == Role.ADMINISTRATOR) {
            return true;
        }

        if (requester.getRole() == Role.MODERATOR) {
            return author.getId().equals(requester.getId()) || author.getRole() == Role.BREEDER;
        }

        return false;
    }
}
