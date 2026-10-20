package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.FlightPlanCreateRequest;
import com.pzhgp.backend.dto.FlightPlanDetailsDto;
import com.pzhgp.backend.dto.FlightPlanEntryDto;
import com.pzhgp.backend.dto.FlightPlanEntryRequest;
import com.pzhgp.backend.dto.FlightPlanSummaryDto;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.exception.ResourceConflictException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightPlanRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import com.pzhgp.backend.repository.projection.FlightResultSummaryProjection;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
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
    @DisplayName("Should return flight plans in the order provided by the repository")
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
    @DisplayName("Should separate adult and young flights and preserve their date order")
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
    @DisplayName("Should order flight results with branch results first and section results by sort order")
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
    @DisplayName("Should return an empty result list for a flight without uploaded results")
    void returnsEmptyResultListForFlightWithoutResults() {
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.findAllByFlightPlanIdOrderByScheduledDateAscIdAsc(plan.getId())).thenReturn(List.of(adultFlight));
        when(flightResultRepository.findSummariesByFlightPlanId(plan.getId())).thenReturn(List.of());

        FlightPlanDetailsDto result = flightPlanService.getPlanByYear(2026);

        assertTrue(result.adultFlights().getFirst().results().isEmpty());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when requesting a plan for a missing year")
    void rejectsMissingPlan() {
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> flightPlanService.getPlanByYear(2026));
        assertEquals("Nie znaleziono planu lotów dla roku 2026.", exception.getMessage());

        verifyNoInteractions(flightPlanEntryRepository);
        verifyNoInteractions(flightResultRepository);
    }

    @Test
    @DisplayName("Should allow an administrator to create a new flight plan")
    void createsPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.existsByYear(2026)).thenReturn(false);
        when(flightPlanRepository.saveAndFlush(any(FlightPlan.class))).thenAnswer(invocation -> {
            FlightPlan saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        Long id = flightPlanService.createPlan(new FlightPlanCreateRequest(2026), administrator.getEmail());
        assertEquals(50L, id);

        ArgumentCaptor<FlightPlan> captor = ArgumentCaptor.forClass(FlightPlan.class);
        verify(flightPlanRepository).saveAndFlush(captor.capture());

        assertEquals(2026, captor.getValue().getYear());
    }

    @Test
    @DisplayName("Should reject creating another flight plan for the same year")
    void rejectsDuplicatePlanYear() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.existsByYear(2026)).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> flightPlanService.createPlan(
                new FlightPlanCreateRequest(2026),
                administrator.getEmail()
        ));

        assertEquals("Plan lotów dla roku 2026 już istnieje.", exception.getMessage());
        verify(flightPlanRepository, never()).saveAndFlush(any());
    }


    @Test
    @DisplayName("Should throw ResourceConflictException when database detects duplicate plan year")
    void rejectsDuplicatePlanYearDetectedByDatabase() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));

        when(flightPlanRepository.existsByYear(2026)).thenReturn(false);

        SQLException sqlException = new SQLException("Duplicate key", "23505");

        ConstraintViolationException constraintException = new ConstraintViolationException(
                "Duplicate plan year",
                sqlException,
                "uk_flight_plans_year"
        );

        when(flightPlanRepository.saveAndFlush(any(FlightPlan.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate plan year", constraintException));

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () ->
                flightPlanService.createPlan(new FlightPlanCreateRequest(2026), administrator.getEmail())
        );

        assertEquals("Plan lotów dla roku 2026 już istnieje.", exception.getMessage());

        verify(flightPlanRepository).saveAndFlush(any(FlightPlan.class));
    }


    @Test
    @DisplayName("Should reject flight plan creation by a moderator")
    void rejectsPlanCreationByModerator() {
        when(breederRepository.findByEmail(moderator.getEmail())).thenReturn(Optional.of(moderator));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class, () -> flightPlanService.createPlan(
                new FlightPlanCreateRequest(2026),
                moderator.getEmail()
        ));

        assertEquals("Brak uprawnień. Planami lotów może zarządzać wyłącznie administrator.", exception.getMessage());
        verifyNoInteractions(flightPlanRepository);
    }

    @Test
    @DisplayName("Should reject plan modifications when the requesting user does not exist")
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
    @DisplayName("Should allow an administrator to add a flight to an existing plan")
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
    @DisplayName("Should store a blank flight category as null")
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
    @DisplayName("Should reject adding a flight scheduled outside the plan year")
    void rejectsFlightFromDifferentYearOnCreate() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));

        FlightPlanEntryRequest request = request(LocalDate.of(2025, 12, 31));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> flightPlanService.addEntry(2026, request, administrator.getEmail()));

        assertEquals("Data lotu musi należeć do roku planu: 2026.", exception.getMessage());
        verify(flightPlanEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when adding a flight to a missing plan")
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
    @DisplayName("Should allow an administrator to update an existing flight")
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
    @DisplayName("Should reject updating a flight with a date outside the plan year")
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
    @DisplayName("Should throw EntityNotFoundException when updating a missing flight")
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
    @DisplayName("Should allow an administrator to delete a flight without results")
    void deletesFlightWithoutResults() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(adultFlight.getId())).thenReturn(Optional.of(adultFlight));
        when(flightResultRepository.existsByFlightPlanEntryId(adultFlight.getId())).thenReturn(false);

        flightPlanService.deleteEntry(adultFlight.getId(), administrator.getEmail());

        verify(flightPlanEntryRepository).delete(adultFlight);
    }

    @Test
    @DisplayName("Should reject deleting a flight with uploaded results")
    void rejectsDeletingFlightWithResults() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(adultFlight.getId())).thenReturn(Optional.of(adultFlight));
        when(flightResultRepository.existsByFlightPlanEntryId(adultFlight.getId())).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> flightPlanService.deleteEntry(adultFlight.getId(), administrator.getEmail()));

        assertEquals("Nie można usunąć lotu z planu, ponieważ posiada przypisane wyniki. Najpierw usuń wyniki tego lotu.", exception.getMessage());
        verify(flightPlanEntryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a missing flight")
    void rejectsDeletingMissingFlight() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanEntryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> flightPlanService.deleteEntry(999L, administrator.getEmail()));
        verify(flightPlanEntryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow an administrator to delete an empty flight plan")
    void deletesEmptyPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.existsByFlightPlanId(plan.getId())).thenReturn(false);

        flightPlanService.deletePlan(2026, administrator.getEmail());

        verify(flightPlanRepository).delete(plan);
    }

    @Test
    @DisplayName("Should reject deleting a flight plan that contains flights")
    void rejectsDeletingNonEmptyPlan() {
        when(breederRepository.findByEmail(administrator.getEmail())).thenReturn(Optional.of(administrator));
        when(flightPlanRepository.findByYear(2026)).thenReturn(Optional.of(plan));
        when(flightPlanEntryRepository.existsByFlightPlanId(plan.getId())).thenReturn(true);

        ResourceConflictException exception = assertThrows(ResourceConflictException.class, () -> flightPlanService.deletePlan(2026, administrator.getEmail()));

        assertEquals("Nie można usunąć planu lotów, który zawiera loty. Najpierw usuń wszystkie pozycje planu.", exception.getMessage());
        verify(flightPlanRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a missing flight plan")
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