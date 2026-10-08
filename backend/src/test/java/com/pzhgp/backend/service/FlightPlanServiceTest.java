package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FlightPlanCreateRequest;
import com.pzhgp.backend.dto.FlightPlanDetailsDto;
import com.pzhgp.backend.dto.FlightPlanEntryDto;
import com.pzhgp.backend.dto.FlightPlanEntryRequest;
import com.pzhgp.backend.dto.FlightPlanSummaryDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ResourceConflictException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightPlanRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import com.pzhgp.backend.repository.projection.FlightResultSummaryProjection;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightPlanServiceTest {

    @Mock
    private FlightPlanRepository flightPlanRepository;

    @Mock
    private FlightPlanEntryRepository flightPlanEntryRepository;

    @Mock
    private FlightResultRepository flightResultRepository;

    @Mock
    private BreederRepository breederRepository;

    @InjectMocks
    private FlightPlanService flightPlanService;

    private Breeder administrator;
    private Breeder moderator;
    private FlightPlan plan;
    private FlightPlanEntry adultFlight;
    private FlightPlanEntry youngFlight;

    @BeforeEach
    void setUp() {
        administrator = breeder(1L, "admin@test.pl", Role.ADMINISTRATOR);
        moderator = breeder(2L, "moderator@test.pl", Role.MODERATOR);

        plan = new FlightPlan();
        plan.setId(10L);
        plan.setYear(2026);

        adultFlight = flight(
                100L,
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 4, 26),
                "Dahme",
                130,
                "A",
                "Oddziałowa"
        );

        youngFlight = flight(
                200L,
                PigeonAgeGroup.YOUNG,
                LocalDate.of(2026, 8, 2),
                "Jessen",
                170,
                null,
                "Oddziałowa"
        );
    }

    @Test
    @DisplayName("Plans are returned in the order provided by the repository")
    void returnsAllPlans() {
        FlightPlan olderPlan = new FlightPlan();
        olderPlan.setId(11L);
        olderPlan.setYear(2025);

        when(flightPlanRepository.findAllByOrderByYearDesc()).thenReturn(List.of(plan, olderPlan));
        List<FlightPlanSummaryDto> result = flightPlanService.getAllPlans();

        assertEquals(2, result.size());
        assertEquals(2026, result.getFirst().year());
        assertEquals(2025, result.get(1).year());
        assertEquals(10L, result.getFirst().id());
        assertEquals(11L, result.get(1).id());

        verify(flightPlanRepository).findAllByOrderByYearDesc();
    }

    @Test
    @DisplayName("Plan details separate adult and young flights and preserve date order")
    void returnsPlanDetailsSeparatedByAgeGroup() {
        FlightPlanEntry secondAdultFlight = flight(
                101L,
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 5, 3),
                "Dessau",
                220,
                "A",
                "Oddziałowa"
        );

        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId())).thenReturn(List.of(adultFlight, secondAdultFlight, youngFlight));
        when(flightResultRepository.findSummariesByFlightPlanId(plan.getId())).thenReturn(List.of());

        FlightPlanDetailsDto result = flightPlanService.getPlanByYear(2026);

        assertEquals(plan.getId(), result.id());
        assertEquals(2026, result.year());

        assertEquals(2, result.adultFlights().size());
        assertEquals(1, result.youngFlights().size());

        assertEquals("Dahme", result.adultFlights().getFirst().location());
        assertEquals(LocalDate.of(2026, 4, 26), result.adultFlights().getFirst().scheduledDate());

        assertEquals("Dessau", result.adultFlights().get(1).location());
        assertEquals(LocalDate.of(2026, 5, 3), result.adultFlights().get(1).scheduledDate());

        assertEquals("Jessen", result.youngFlights().getFirst().location());

        verify(flightPlanEntryRepository).findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId());
    }

    @Test
    @DisplayName("Flight results are ordered with Branch first and Sections by their sort order")
    void sortsFlightResults() {
        FlightResultSummaryProjection sectionThree = resultProjection(
                3L,
                adultFlight.getId(),
                FlightResultScope.SECTION,
                3L,
                "Chotków",
                3,
                "sekcja3.txt"
        );

        FlightResultSummaryProjection branch = resultProjection(
                1L,
                adultFlight.getId(),
                FlightResultScope.BRANCH,
                null,
                null,
                null,
                "oddzial.txt"
        );

        FlightResultSummaryProjection sectionOne = resultProjection(
                2L,
                adultFlight.getId(),
                FlightResultScope.SECTION,
                1L,
                "Żagań",
                1,
                "sekcja1.txt"
        );

        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId())).thenReturn(List.of(adultFlight));
        when(flightResultRepository.findSummariesByFlightPlanId(plan.getId())).thenReturn(List.of(sectionThree, branch, sectionOne));

        FlightPlanDetailsDto result = flightPlanService.getPlanByYear(2026);
        List<com.pzhgp.backend.dto.FlightResultSummaryDto> results = result.adultFlights().getFirst().results();

        assertEquals(3, results.size());
        assertEquals(FlightResultScope.BRANCH, results.getFirst().scope());
        assertEquals("oddzial.txt", results.getFirst().originalFileName());

        assertEquals(FlightResultScope.SECTION, results.get(1).scope());
        assertEquals(1, results.get(1).sectionSortOrder());

        assertEquals(FlightResultScope.SECTION, results.get(2).scope());
        assertEquals(3, results.get(2).sectionSortOrder());
    }

    @Test
    @DisplayName("A flight without uploaded results contains an empty result list")
    void returnsEmptyResultListForFlightWithoutResults() {
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId())).thenReturn(List.of(adultFlight));
        when(flightResultRepository.findSummariesByFlightPlanId(plan.getId())).thenReturn(List.of());

        FlightPlanDetailsDto result = flightPlanService.getPlanByYear(2026);

        assertTrue(result.adultFlights().getFirst().results().isEmpty());
    }

    @Test
    @DisplayName("Requesting a plan for a missing year throws EntityNotFoundException")
    void rejectsMissingPlan() {
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> flightPlanService.getPlanByYear(2026));
        assertEquals("Nie znaleziono planu lotów dla roku 2026.", exception.getMessage());

        verifyNoInteractions(flightPlanEntryRepository);
        verifyNoInteractions(flightResultRepository);
    }

    @Test
    @DisplayName("Administrator can create a new flight plan")
    void createsPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.existsByYear(2026)).thenReturn(false);
        when(flightPlanRepository.save(any(FlightPlan.class))).thenAnswer(invocation -> {
            FlightPlan saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        Long id = flightPlanService.createPlan(new FlightPlanCreateRequest(2026), administrator.getEmail());
        assertEquals(50L, id);

        ArgumentCaptor<FlightPlan> captor = ArgumentCaptor.forClass(FlightPlan.class);
        verify(flightPlanRepository).save(captor.capture());

        assertEquals(2026, captor.getValue().getYear());
    }

    @Test
    @DisplayName("Creating another plan for the same year is rejected")
    void rejectsDuplicatePlanYear() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.existsByYear(2026)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> flightPlanService.createPlan(
                new FlightPlanCreateRequest(2026),
                administrator.getEmail()
        ));

        assertEquals("Plan lotów dla roku 2026 już istnieje.", exception.getMessage());
        verify(flightPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("Moderator cannot create a flight plan")
    void rejectsPlanCreationByModerator() {
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> flightPlanService.createPlan(
                new FlightPlanCreateRequest(2026),
                moderator.getEmail()
        ));

        assertEquals("Brak uprawnień. Planami lotów może zarządzać wyłącznie administrator.", exception.getMessage());
        verifyNoInteractions(flightPlanRepository);
    }

    @Test
    @DisplayName("Plan modification fails when the requesting user does not exist")
    void rejectsMissingAdministrator() {
        when(breederRepository.findByEmail("ghost@test.pl")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> flightPlanService.createPlan(
                new FlightPlanCreateRequest(2026),
                "ghost@test.pl"
        ));

        assertEquals("Nie znaleziono użytkownika.", exception.getMessage());
        verifyNoInteractions(flightPlanRepository);
    }

    @Test
    @DisplayName("Administrator can add a flight to an existing plan")
    void addsFlight() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.save(any(FlightPlanEntry.class))).thenAnswer(invocation -> {
            FlightPlanEntry saved = invocation.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        FlightPlanEntryRequest request = new FlightPlanEntryRequest(
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 5, 10),
                "  Dessau  ",
                220,
                "  A/B  ",
                "  Oddziałowa  "
        );

        Long id = flightPlanService.addEntry(2026, request, administrator.getEmail());

        assertEquals(300L, id);

        ArgumentCaptor<FlightPlanEntry> captor = ArgumentCaptor.forClass(FlightPlanEntry.class);
        verify(flightPlanEntryRepository).save(captor.capture());

        FlightPlanEntry saved = captor.getValue();

        assertSame(plan, saved.getFlightPlan());
        assertEquals(PigeonAgeGroup.ADULT, saved.getPigeonAgeGroup());
        assertEquals(LocalDate.of(2026, 5, 10), saved.getScheduledDate());
        assertEquals("Dessau", saved.getLocation());
        assertEquals(220, saved.getDistanceKm());
        assertEquals("A/B", saved.getCategory());
        assertEquals("Oddziałowa", saved.getListType());
    }

    @Test
    @DisplayName("Blank flight category is stored as null")
    void convertsBlankCategoryToNull() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.save(any(FlightPlanEntry.class))).thenAnswer(invocation -> {
            FlightPlanEntry saved = invocation.getArgument(0);
            saved.setId(300L);
            return saved;
        });

        FlightPlanEntryRequest request = new FlightPlanEntryRequest(
                PigeonAgeGroup.ADULT,
                LocalDate.of(2026, 5, 10),
                "Dessau",
                220,
                "   ",
                "Oddziałowa"
        );

        flightPlanService.addEntry(2026, request, administrator.getEmail());

        ArgumentCaptor<FlightPlanEntry> captor = ArgumentCaptor.forClass(FlightPlanEntry.class);
        verify(flightPlanEntryRepository).save(captor.capture());

        assertNull(captor.getValue().getCategory());
    }

    @Test
    @DisplayName("Flight date outside the plan year is rejected when adding a flight")
    void rejectsFlightFromDifferentYearOnCreate() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));

        FlightPlanEntryRequest request = request(LocalDate.of(2025, 12, 31));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> flightPlanService.addEntry(2026, request, administrator.getEmail()));

        assertEquals("Data lotu musi należeć do roku planu: 2026.", exception.getMessage());
        verify(flightPlanEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Adding a flight to a missing plan throws EntityNotFoundException")
    void rejectsFlightForMissingPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> flightPlanService.addEntry(
                2026,
                request(LocalDate.of(2026, 4, 26)),
                administrator.getEmail()
        ));

        verify(flightPlanEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Administrator can update an existing flight")
    void updatesFlight() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(adultFlight.getId())).thenReturn(Optional.of(adultFlight));

        FlightPlanEntryRequest request = new FlightPlanEntryRequest(
                PigeonAgeGroup.YOUNG,
                LocalDate.of(2026, 8, 16),
                "  Helmstedt  ",
                310,
                "   ",
                "  Oddział/Dubel  "
        );

        flightPlanService.updateEntry(adultFlight.getId(), request, administrator.getEmail());

        assertEquals(PigeonAgeGroup.YOUNG, adultFlight.getPigeonAgeGroup());
        assertEquals(LocalDate.of(2026, 8, 16), adultFlight.getScheduledDate());
        assertEquals("Helmstedt", adultFlight.getLocation());
        assertEquals(310, adultFlight.getDistanceKm());
        assertNull(adultFlight.getCategory());
        assertEquals("Oddział/Dubel", adultFlight.getListType());

        verify(flightPlanEntryRepository).save(adultFlight);
    }

    @Test
    @DisplayName("Flight date outside the plan year is rejected when updating a flight")
    void rejectsFlightFromDifferentYearOnUpdate() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(adultFlight.getId())).thenReturn(Optional.of(adultFlight));

        FlightPlanEntryRequest request = request(LocalDate.of(2027, 1, 1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> flightPlanService.updateEntry(
                adultFlight.getId(),
                request,
                administrator.getEmail()
        ));

        assertEquals("Data lotu musi należeć do roku planu: 2026.", exception.getMessage());
        verify(flightPlanEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating a missing flight throws EntityNotFoundException")
    void rejectsUpdateOfMissingFlight() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> flightPlanService.updateEntry(
                999L,
                request(LocalDate.of(2026, 5, 3)),
                administrator.getEmail()
        ));

        assertEquals("Nie znaleziono lotu w planie o ID: 999", exception.getMessage());
        verify(flightPlanEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Administrator can delete a flight without results")
    void deletesFlightWithoutResults() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(adultFlight.getId())).thenReturn(Optional.of(adultFlight));
        when(flightResultRepository.existsByFlightPlanEntryId(adultFlight.getId())).thenReturn(false);

        flightPlanService.deleteEntry(adultFlight.getId(), administrator.getEmail());

        verify(flightPlanEntryRepository).delete(adultFlight);
    }

    @Test
    @DisplayName("Flight with uploaded results cannot be deleted")
    void rejectsDeletingFlightWithResults() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(adultFlight.getId())).thenReturn(Optional.of(adultFlight));
        when(flightResultRepository.existsByFlightPlanEntryId(adultFlight.getId())).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> flightPlanService.deleteEntry(adultFlight.getId(), administrator.getEmail()));

        assertEquals("Nie można usunąć lotu z planu, ponieważ posiada przypisane wyniki. Najpierw usuń wyniki tego lotu.", exception.getMessage());
        verify(flightPlanEntryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deleting a missing flight throws EntityNotFoundException")
    void rejectsDeletingMissingFlight() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> flightPlanService.deleteEntry(999L, administrator.getEmail()));
        verify(flightPlanEntryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Administrator can delete an empty flight plan")
    void deletesEmptyPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.existsByFlightPlanId(plan.getId())).thenReturn(false);

        flightPlanService.deletePlan(2026, administrator.getEmail());

        verify(flightPlanRepository).delete(plan);
    }

    @Test
    @DisplayName("Flight plan containing flights cannot be deleted")
    void rejectsDeletingNonEmptyPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.existsByFlightPlanId(plan.getId())).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> flightPlanService.deletePlan(2026, administrator.getEmail()));

        assertEquals("Nie można usunąć planu lotów, który zawiera loty. Najpierw usuń wszystkie pozycje planu.", exception.getMessage());
        verify(flightPlanRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deleting a missing flight plan throws EntityNotFoundException")
    void rejectsDeletingMissingPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> flightPlanService.deletePlan(2026, administrator.getEmail()));
        verify(flightPlanRepository, never()).delete(any());
    }

    private Breeder breeder(Long id, String email, Role role) {
        Breeder breeder = new Breeder();
        breeder.setId(id);
        breeder.setEmail(email);
        breeder.setRole(role);
        return breeder;
    }

    private FlightPlanEntry flight(
            Long id,
            PigeonAgeGroup ageGroup,
            LocalDate date,
            String location,
            int distanceKm,
            String category,
            String listType
    ) {
        FlightPlanEntry entry = new FlightPlanEntry();
        entry.setId(id);
        entry.setFlightPlan(plan);
        entry.setPigeonAgeGroup(ageGroup);
        entry.setScheduledDate(date);
        entry.setLocation(location);
        entry.setDistanceKm(distanceKm);
        entry.setCategory(category);
        entry.setListType(listType);
        return entry;
    }

    private FlightPlanEntryRequest request(LocalDate date) {
        return new FlightPlanEntryRequest(
                PigeonAgeGroup.ADULT,
                date,
                "Dahme",
                130,
                "A",
                "Oddziałowa"
        );
    }

    private FlightResultSummaryProjection resultProjection(
            Long id,
            Long flightPlanEntryId,
            FlightResultScope scope,
            Long sectionId,
            String sectionName,
            Integer sectionSortOrder,
            String originalFileName
    ) {
        FlightResultSummaryProjection projection = mock(FlightResultSummaryProjection.class);

        when(projection.getId()).thenReturn(id);
        when(projection.getFlightPlanEntryId()).thenReturn(flightPlanEntryId);
        when(projection.getScope()).thenReturn(scope);
        when(projection.getSectionId()).thenReturn(sectionId);
        when(projection.getSectionName()).thenReturn(sectionName);
        when(projection.getSectionSortOrder()).thenReturn(sectionSortOrder);
        when(projection.getOriginalFileName()).thenReturn(originalFileName);

        return projection;
    }
}