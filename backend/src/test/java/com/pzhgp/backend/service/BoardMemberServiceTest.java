package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.BoardMemberDto;
import com.pzhgp.backend.dto.BoardMemberRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BoardMemberRepository;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.SectionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardMemberServiceTest {

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private BreederRepository breederRepository;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private BoardMemberService boardMemberService;

    private Section section;
    private Breeder breeder;

    @BeforeEach
    void setUp() {
        section = new Section();
        section.setId(1L);
        section.setName("Żagań");

        breeder = new Breeder();
        breeder.setId(1L);
        breeder.setName("Jan");
        breeder.setSurname("Kowalski");
        breeder.setSection(section);
        breeder.setStatus(AccountStatus.ACTIVE);
    }


    @Test
    @DisplayName("Should successfully map and return a list of all board members")
    void shouldReturnAllBoardMembersAsDtoList() {
        BoardMember member = new BoardMember();
        member.setId(10L);
        member.setRole(BoardRole.PREZES);
        member.setBreeder(breeder);
        member.setManagedSection(section);

        when(boardMemberRepository.findAllWithDetails()).thenReturn(List.of(member));

        List<BoardMemberDto> result = boardMemberService.getAllBoardMembers();

        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().id());
        assertEquals(BoardRole.PREZES, result.getFirst().role());
        assertEquals(1L, result.getFirst().breederId());
        assertEquals("Jan", result.getFirst().firstName());
        assertEquals(1L, result.getFirst().managedSectionId());
    }

    @Test
    @DisplayName("Should delete board member when they exist in the database")
    void shouldDeleteBoardMemberWhenExists() {
        BoardMember member = new BoardMember();
        member.setId(1L);

        when(boardMemberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertDoesNotThrow(() -> boardMemberService.deleteBoardMember(1L));
        verify(boardMemberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when attempting to delete a non-existent member")
    void shouldThrowExceptionWhenDeletingNonExistentMember() {
        when(boardMemberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> boardMemberService.deleteBoardMember(99L));
        verify(boardMemberRepository, never()).delete(any());
    }


    @Test
    @DisplayName("Should throw EntityNotFoundException when assigned Section does not exist")
    void shouldThrowExceptionWhenSectionNotFound() {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, 99L, 1L, null, null, null);

        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> boardMemberService.createBoardMember(request));
        verify(boardMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when assigned Breeder does not exist")
    void shouldThrowExceptionWhenBreederNotFound() {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, 99L, null, null, null);

        when(breederRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> boardMemberService.createBoardMember(request));
        verify(boardMemberRepository, never()).save(any());
    }


    @Test
    @DisplayName("Should successfully create a board member for a Section with valid data")
    void shouldCreateSectionMemberSuccessfully() {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, 1L, 1L, null, null, null);

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(breederRepository.findById(1L)).thenReturn(Optional.of(breeder));
        when(boardMemberRepository.existsByBreederAndRoleForSection(any(), any(), any(), any())).thenReturn(false);
        when(boardMemberRepository.existsConflictForSection(any(), any(), any())).thenReturn(false);

        assertDoesNotThrow(() -> boardMemberService.createBoardMember(request));
        verify(boardMemberRepository, times(1)).save(any(BoardMember.class));
    }

    @Test
    @DisplayName("Should prevent assigning an invalid role (e.g., Vice President) in a Section board")
    void shouldThrowExceptionWhenInvalidRoleAssignedToSection() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.WICEPREZES_DS_LOTOWYCH, 1L, 1L, null, null, null
        );

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                boardMemberService.createBoardMember(request)
        );
        assertTrue(exception.getMessage().contains("wyłącznie stanowiska: Prezes, Skarbnik lub Sekretarz"));
    }

    @Test
    @DisplayName("Should prevent assigning a Treasurer in a Branch board")
    void shouldThrowExceptionWhenSkarbnikAssignedToBranch() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.SKARBNIK, null, 1L, null, null, null
        );

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                boardMemberService.createBoardMember(request)
        );
        assertTrue(exception.getMessage().contains("Zarząd Oddziału nie posiada stanowiska Skarbnika"));
    }


    @Test
    @DisplayName("Should throw EntityNotFoundException when updating a non-existent board member")
    void shouldThrowExceptionWhenUpdatingNonExistentMember() {
        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, 1L, null, null, null);
        when(boardMemberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> boardMemberService.updateBoardMember(99L, request));
    }

    @Test
    @DisplayName("Should successfully update existing board member without triggering false positive unique conflicts")
    void shouldUpdateExistingBoardMemberWithoutFalseConflict() {
        BoardMember existingMember = new BoardMember();
        existingMember.setId(10L);
        existingMember.setRole(BoardRole.PREZES);
        existingMember.setBreeder(breeder);

        BoardMemberRequest request = new BoardMemberRequest(BoardRole.PREZES, null, 1L, null, null, null);

        when(boardMemberRepository.findById(10L)).thenReturn(Optional.of(existingMember));
        when(breederRepository.findById(1L)).thenReturn(Optional.of(breeder));

        when(boardMemberRepository.existsByBreederAndRoleForBranch(1L, BoardRole.PREZES, 10L)).thenReturn(false);
        when(boardMemberRepository.existsConflictForBranch(BoardRole.PREZES, 10L)).thenReturn(false);

        assertDoesNotThrow(() -> boardMemberService.updateBoardMember(10L, request));
        verify(boardMemberRepository, times(1)).save(existingMember);
    }


    @Test
    @DisplayName("Should throw an exception when a unique role in a Branch is already taken")
    void shouldThrowExceptionWhenUniqueRoleConflictInBranch() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.PREZES, null, 1L, null, null, null
        );

        when(boardMemberRepository.existsConflictForBranch(eq(BoardRole.PREZES), any())).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                boardMemberService.createBoardMember(request)
        );
        assertTrue(exception.getMessage().contains("jest już zajęte w tym zarządzie"));
    }

    @Test
    @DisplayName("Should throw an exception when a unique role in a Section is already taken")
    void shouldThrowExceptionWhenUniqueRoleConflictInSection() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.SEKRETARZ, 1L, 1L, null, null, null
        );

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(boardMemberRepository.existsConflictForSection(eq(BoardRole.SEKRETARZ), eq(1L), any())).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                boardMemberService.createBoardMember(request)
        );
        assertTrue(exception.getMessage().contains("jest już zajęte w tym zarządzie"));
    }

    @Test
    @DisplayName("Should ignore uniqueness check for regular Board Members (non-unique role)")
    void shouldAllowMultipleMembersWithNonUniqueRole() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.CZLONEK_ZARZADU, null, 1L, null, null, null
        );

        when(breederRepository.findById(1L)).thenReturn(Optional.of(breeder));
        when(boardMemberRepository.existsByBreederAndRoleForBranch(anyLong(), any(), any())).thenReturn(false);

        assertDoesNotThrow(() -> boardMemberService.createBoardMember(request));

        verify(boardMemberRepository, times(1)).existsByBreederAndRoleForBranch(anyLong(), any(), any());
        verify(boardMemberRepository, never()).existsConflictForBranch(any(), any());
    }


    @Test
    @DisplayName("Should throw an exception when attempting to assign a breeder to a different Section")
    void shouldThrowExceptionWhenBreederSectionMismatch() {
        Section otherSection = new Section();
        otherSection.setId(2L);
        otherSection.setName("Szprotawa");
        breeder.setSection(otherSection);

        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.PREZES, 1L, 1L, null, null, null
        );

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(breederRepository.findById(1L)).thenReturn(Optional.of(breeder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                boardMemberService.createBoardMember(request)
        );
        assertEquals("Hodowca nie należy do sekcji, w której ma pełnić funkcję.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw an exception when the same breeder is assigned the exact same role multiple times in a Section")
    void shouldThrowExceptionWhenBreederAlreadyHasSameRoleInSection() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.PREZES, 1L, 1L, null, null, null
        );

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(section));
        when(breederRepository.findById(1L)).thenReturn(Optional.of(breeder));
        when(boardMemberRepository.existsByBreederAndRoleForSection(eq(1L), eq(BoardRole.PREZES), eq(1L), any())).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                boardMemberService.createBoardMember(request)
        );
        assertEquals("Ten hodowca pełni już to stanowisko w tym zarządzie.", exception.getMessage());
    }


    @Test
    @DisplayName("Should successfully save a custom person with full details including phone number")
    void shouldSaveCustomPersonWithPhoneNumber() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.CZLONEK_ZARZADU, null, null, "Anna", "Nowak", "987654321"
        );

        assertDoesNotThrow(() -> boardMemberService.createBoardMember(request));

        ArgumentCaptor<BoardMember> captor = ArgumentCaptor.forClass(BoardMember.class);
        verify(boardMemberRepository).save(captor.capture());

        BoardMember savedMember = captor.getValue();
        assertNull(savedMember.getBreeder());
        assertEquals("Anna", savedMember.getCustomName());
        assertEquals("Nowak", savedMember.getCustomSurname());
        assertEquals("987654321", savedMember.getContactPhone());
    }

    @Test
    @DisplayName("Should successfully save a custom board member, trimming whitespace")
    void shouldSaveSuccessfullyWithCustomPersonAndTrim() {
        BoardMemberRequest request = new BoardMemberRequest(
                BoardRole.WICEPREZES_DS_GOSPODARCZYCH, null, null, "  Adam  ", " Nowak ", null
        );

        when(boardMemberRepository.existsConflictForBranch(any(), any())).thenReturn(false);

        boardMemberService.createBoardMember(request);

        ArgumentCaptor<BoardMember> captor = ArgumentCaptor.forClass(BoardMember.class);
        verify(boardMemberRepository).save(captor.capture());

        BoardMember savedMember = captor.getValue();
        assertNull(savedMember.getBreeder());
        assertEquals("Adam", savedMember.getCustomName());
        assertEquals("Nowak", savedMember.getCustomSurname());
        assertNull(savedMember.getContactPhone());
    }
}