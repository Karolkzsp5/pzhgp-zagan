package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BoardMemberDto;
import com.pzhgp.backend.dto.BoardMemberRequest;
import com.pzhgp.backend.entity.BoardMember;
import com.pzhgp.backend.entity.BoardRole;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.Section;
import com.pzhgp.backend.repository.BoardMemberRepository;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardMemberService {

    private final BoardMemberRepository boardMemberRepository;
    private final BreederRepository breederRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<BoardMemberDto> getAllBoardMembers() {
        return boardMemberRepository.findAllWithDetails().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void createBoardMember(BoardMemberRequest request) {
        BoardMember member = new BoardMember();
        updateEntityFromRequest(member, request);
        boardMemberRepository.save(member);
    }

    @Transactional
    public void updateBoardMember(Long id, BoardMemberRequest request) {
        BoardMember member = boardMemberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono członka zarządu o ID: " + id));
        updateEntityFromRequest(member, request);
        boardMemberRepository.save(member);
    }

    @Transactional
    public void deleteBoardMember(Long id) {
        BoardMember member = boardMemberRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono członka zarządu o ID: " + id));
        boardMemberRepository.delete(member);
    }

    private void updateEntityFromRequest(BoardMember member, BoardMemberRequest request) {
        member.setRole(request.role());

        Section managedSection = null;
        if (request.managedSectionId() != null) {
            managedSection = sectionRepository.findById(request.managedSectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono sekcji o ID: " + request.managedSectionId()));
        }
        member.setManagedSection(managedSection);

        if (managedSection != null) {
            if (request.role() != BoardRole.PREZES &&
                    request.role() != BoardRole.SKARBNIK &&
                    request.role() != BoardRole.SEKRETARZ) {
                throw new IllegalStateException("Dla zarządu sekcji dozwolone są wyłącznie stanowiska: Prezes, Skarbnik lub Sekretarz.");
            }
        } else {
            if (request.role() == BoardRole.SKARBNIK) {
                throw new IllegalStateException("Zarząd Oddziału nie posiada stanowiska Skarbnika.");
            }
        }

        validateUniqueRole(request.role(), managedSection, member.getId());

        if (request.breederId() != null) {
            Breeder breeder = breederRepository.findById(request.breederId())
                    .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono hodowcy o ID: " + request.breederId()));

            if (managedSection != null && !breeder.getSection().getId().equals(managedSection.getId())) {
                throw new IllegalStateException("Hodowca nie należy do sekcji, w której ma pełnić funkcję.");
            }

            Long safeExcludeId = (member.getId() != null) ? member.getId() : -1L;
            boolean alreadyHasThisRole = managedSection != null
                    ? boardMemberRepository.existsByBreederAndRoleForSection(request.breederId(), request.role(), managedSection.getId(), safeExcludeId)
                    : boardMemberRepository.existsByBreederAndRoleForBranch(request.breederId(), request.role(), safeExcludeId);

            if (alreadyHasThisRole) {
                throw new IllegalStateException("Ten hodowca pełni już to stanowisko w tym zarządzie.");
            }

            member.setBreeder(breeder);
            member.setCustomName(null);
            member.setCustomSurname(null);
        } else {
            member.setBreeder(null);
            member.setCustomName(request.customName() != null ? request.customName().trim() : null);
            member.setCustomSurname(request.customSurname() != null ? request.customSurname().trim() : null);
        }

        member.setContactPhone(request.contactPhone() != null && !request.contactPhone().isBlank() ? request.contactPhone().trim() : null);
    }

    private void validateUniqueRole(BoardRole role, Section section, Long excludeId) {
        if (!role.isUniquePerBoard()) return;

        Long safeExcludeId = (excludeId != null) ? excludeId : -1L;
        boolean existsConflict;

        if (section != null) {
            existsConflict = boardMemberRepository.existsConflictForSection(role, section.getId(), safeExcludeId);
        } else {
            existsConflict = boardMemberRepository.existsConflictForBranch(role, safeExcludeId);
        }

        if (existsConflict) {
            throw new IllegalStateException("To stanowisko (" + role.name() + ") jest już zajęte w tym zarządzie.");
        }
    }

    private BoardMemberDto mapToDto(BoardMember member) {
        String firstName = member.getBreeder() != null ? member.getBreeder().getName() : member.getCustomName();
        String lastName = member.getBreeder() != null ? member.getBreeder().getSurname() : member.getCustomSurname();

        Long sectionId = member.getManagedSection() != null ? member.getManagedSection().getId() : null;
        String sectionName = member.getManagedSection() != null ? member.getManagedSection().getName() : null;
        Long breederId = member.getBreeder() != null ? member.getBreeder().getId() : null;

        return new BoardMemberDto(
                member.getId(),
                member.getRole(),
                sectionId,
                sectionName,
                firstName,
                lastName,
                member.getContactPhone(),
                breederId
        );
    }
}