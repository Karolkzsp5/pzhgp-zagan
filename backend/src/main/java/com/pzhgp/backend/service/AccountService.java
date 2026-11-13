package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.AccountDataExportDto;
import com.pzhgp.backend.dto.AccountDto;
import com.pzhgp.backend.dto.ContactUpdateRequest;
import com.pzhgp.backend.dto.PasswordChangeRequest;
import com.pzhgp.backend.entity.BoardMember;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.exception.ResourceConflictException;
import com.pzhgp.backend.repository.AnnouncementRepository;
import com.pzhgp.backend.repository.BoardMemberRepository;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumPostRepository;
import com.pzhgp.backend.repository.ForumThreadRepository;
import com.pzhgp.backend.repository.PigeonFlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Konto zalogowanego użytkownika: dane na stronie ustawień, zmiana danych kontaktowych i hasła,
 * wylogowanie ze wszystkich urządzeń oraz eksport danych.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final BreederRepository breederRepository;
    private final BoardMemberRepository boardMemberRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final ForumPostRepository forumPostRepository;
    private final AnnouncementRepository announcementRepository;
    private final PigeonFlightRepository pigeonFlightRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AccountDto getAccount(String email) {
        return mapToDto(breederRepository.requireByEmail(email));
    }

    @Transactional
    public AccountDto updateContact(String email, ContactUpdateRequest request) {
        Breeder breeder = breederRepository.requireByEmail(email);

        if (breederRepository.existsByPhoneNumberAndIdNot(request.phoneNumber(), breeder.getId())) {
            throw new ResourceConflictException("Użytkownik z podanym numerem telefonu już istnieje.");
        }

        breeder.setPhoneNumber(request.phoneNumber());
        breeder.setPostalCode(request.postalCode());
        breeder.setCity(request.city());
        breeder.setStreet(request.street());
        breeder.setHouseNumber(request.houseNumber());

        try {
            breederRepository.saveAndFlush(breeder);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceConflictException("Użytkownik z podanym numerem telefonu już istnieje.", ex);
        }

        return mapToDto(breeder);
    }

    /**
     * Zmienia hasło po sprawdzeniu obecnego i kończy wszystkie sesje konta, także bieżącą.
     *
     * @throws IllegalArgumentException gdy obecne hasło jest nieprawidłowe albo nowe hasło jest takie samo
     */
    @Transactional
    public void changePassword(String email, PasswordChangeRequest request) {
        Breeder breeder = breederRepository.requireByEmail(email);

        if (!passwordEncoder.matches(request.currentPassword(), breeder.getPasswordHash())) {
            throw new IllegalArgumentException("Obecne hasło jest nieprawidłowe.");
        }

        if (passwordEncoder.matches(request.newPassword(), breeder.getPasswordHash())) {
            throw new IllegalArgumentException("Nowe hasło musi się różnić od obecnego.");
        }

        breeder.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // Niewykorzystany link z wiadomości „Nie pamiętasz hasła?” pozwoliłby nadpisać nowe hasło.
        breeder.setPasswordResetTokenHash(null);
        breeder.setPasswordResetTokenExpiresAt(null);
        breeder.endAllSessions();
        breederRepository.save(breeder);

        mailService.sendPasswordChangedEmail(breeder.getEmail(), breeder.getName());

        log.info("Zmieniono hasło konta o ID {} w ustawieniach.", breeder.getId());
    }

    /** Wylogowuje konto ze wszystkich urządzeń, także z bieżącego. */
    @Transactional
    public void endAllSessions(String email) {
        Breeder breeder = breederRepository.requireByEmail(email);
        breeder.endAllSessions();
        breederRepository.save(breeder);

        log.info("Zakończono wszystkie sesje konta o ID {}.", breeder.getId());
    }

    @Transactional(readOnly = true)
    public AccountDataExportDto exportData(String email) {
        Breeder breeder = breederRepository.requireByEmail(email);
        Long id = breeder.getId();

        return new AccountDataExportDto(
                clock.instant(),
                mapToDto(breeder),
                forumThreadRepository.findByAuthorIdOrderByCreatedAtAscIdAsc(id).stream()
                        .map(thread -> new AccountDataExportDto.ForumThreadData(thread.getTitle(), thread.getCategory().getName(), thread.getCreatedAt()))
                        .toList(),
                forumPostRepository.findByAuthorIdOrderByCreatedAtAscIdAsc(id).stream()
                        .map(post -> new AccountDataExportDto.ForumPostData(post.getThread().getTitle(), post.getBody(), post.getCreatedAt(), post.getEditedAt()))
                        .toList(),
                announcementRepository.findByAuthorIdOrderByCreatedAtAscIdAsc(id).stream()
                        .map(announcement -> new AccountDataExportDto.AnnouncementData(announcement.getTitle(), announcement.getContent(), announcement.getCreatedAt(), announcement.getUpdatedAt()))
                        .toList(),
                pigeonFlightRepository.findByOwnerIdOrderByUploadedAtAscIdAsc(id).stream()
                        .map(flight -> new AccountDataExportDto.FlightData(
                                flight.getName(),
                                flight.getRingNumber(),
                                flight.getReleaseSite(),
                                flight.getOriginalFileName(),
                                flight.getStartTime(),
                                flight.getEndTime(),
                                flight.getTrackDistanceMeters(),
                                flight.getDurationSeconds(),
                                flight.getUploadedAt()
                        ))
                        .toList()
        );
    }

    private AccountDto mapToDto(Breeder breeder) {
        return new AccountDto(
                breeder.getName(),
                breeder.getSurname(),
                breeder.getDateOfBirth(),
                breeder.getEmail(),
                breeder.getPhoneNumber(),
                breeder.getPostalCode(),
                breeder.getCity(),
                breeder.getStreet(),
                breeder.getHouseNumber(),
                breeder.getSection().getName(),
                breeder.getRole(),
                breeder.getCreatedAt(),
                boardMemberRepository.findByBreederId(breeder.getId()).stream().map(AccountService::describeBoardFunction).toList()
        );
    }

    // Te same nazwy co na stronie zarządu, np. „Prezes – Zarząd Oddziału” albo „Skarbnik – Zarząd Sekcji Żagań”.
    private static String describeBoardFunction(BoardMember member) {
        String board = member.getManagedSection() == null ? "Zarząd Oddziału" : "Zarząd Sekcji " + member.getManagedSection().getName();
        return member.getRole().getLabel() + " – " + board;
    }
}