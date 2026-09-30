package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.*;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.FlightPlanEntryRepository;
import com.pzhgp.backend.repository.FlightPlanRepository;
import com.pzhgp.backend.repository.FlightResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightPlanService {

    private final FlightPlanRepository flightPlanRepository;
    private final FlightPlanEntryRepository flightPlanEntryRepository;
    private final FlightResultRepository flightResultRepository;
    private final BreederRepository breederRepository;

    @Transactional(readOnly = true)
    public List<FlightPlanSummaryDto> getAllPlans() {
        return flightPlanRepository.findAllByOrderByYearDesc()
                .stream()
                .map(plan -> new FlightPlanSummaryDto(
                        plan.getId(),
                        plan.getYear()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public FlightPlanDetailsDto getPlanByYear(Integer year) {
        FlightPlan plan = flightPlanRepository.findByYear(year)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono planu lotów dla roku " + year + "."
                ));

        List<FlightPlanEntry> entries =
                flightPlanEntryRepository.findAllByFlightPlanIdOrderBySortOrderAsc(plan.getId());

        List<FlightPlanEntryDto> adultFlights = entries.stream()
                .filter(entry -> entry.getPigeonAgeGroup() == PigeonAgeGroup.ADULT)
                .map(this::mapEntryToDto)
                .toList();

        List<FlightPlanEntryDto> youngFlights = entries.stream()
                .filter(entry -> entry.getPigeonAgeGroup() == PigeonAgeGroup.YOUNG)
                .map(this::mapEntryToDto)
                .toList();

        return new FlightPlanDetailsDto(
                plan.getId(),
                plan.getYear(),
                plan.getAdultNotes(),
                plan.getYoungNotes(),
                adultFlights,
                youngFlights
        );
    }

    @Transactional
    public Long createPlan(FlightPlanCreateRequest request, String userEmail) {
        requirePlanManager(userEmail);

        if (flightPlanRepository.existsByYear(request.year())) {
            throw new IllegalArgumentException(
                    "Plan lotów dla roku " + request.year() + " już istnieje."
            );
        }

        FlightPlan plan = new FlightPlan();
        plan.setYear(request.year());

        return flightPlanRepository.save(plan).getId();
    }

    @Transactional
    public void updateNotes(Integer year, FlightPlanNotesRequest request, String userEmail) {
        requirePlanManager(userEmail);

        FlightPlan plan = requirePlanByYear(year);

        plan.setAdultNotes(trimToNull(request.adultNotes()));
        plan.setYoungNotes(trimToNull(request.youngNotes()));

        flightPlanRepository.save(plan);
    }

    @Transactional
    public Long addEntry(Integer year, FlightPlanEntryRequest request, String userEmail) {
        requirePlanManager(userEmail);

        FlightPlan plan = requirePlanByYear(year);

        validateEntryRequest(plan, request, null);

        FlightPlanEntry entry = new FlightPlanEntry();
        entry.setFlightPlan(plan);
        applyEntryRequest(entry, request);

        return flightPlanEntryRepository.save(entry).getId();
    }

    @Transactional
    public void updateEntry(Long entryId, FlightPlanEntryRequest request, String userEmail) {
        requirePlanManager(userEmail);

        FlightPlanEntry entry = flightPlanEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono lotu w planie o ID: " + entryId
                ));

        validateEntryRequest(entry.getFlightPlan(), request, entryId);
        applyEntryRequest(entry, request);

        flightPlanEntryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long entryId, String userEmail) {
        requirePlanManager(userEmail);

        FlightPlanEntry entry = flightPlanEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono lotu w planie o ID: " + entryId
                ));

        if (flightResultRepository.existsByFlightPlanEntryId(entryId)) {
            throw new IllegalStateException(
                    "Nie można usunąć lotu z planu, ponieważ posiada przypisane wyniki. Najpierw usuń wyniki tego lotu."
            );
        }

        flightPlanEntryRepository.delete(entry);
    }

    @Transactional
    public void deletePlan(Integer year, String userEmail) {
        requirePlanManager(userEmail);

        FlightPlan plan = requirePlanByYear(year);

        if (flightPlanEntryRepository.existsByFlightPlanId(plan.getId())) {
            throw new IllegalStateException(
                    "Nie można usunąć planu lotów, który zawiera loty. Najpierw usuń wszystkie pozycje planu."
            );
        }

        flightPlanRepository.delete(plan);
    }

    private FlightPlan requirePlanByYear(Integer year) {
        return flightPlanRepository.findByYear(year)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono planu lotów dla roku " + year + "."
                ));
    }

    private Breeder requirePlanManager(String userEmail) {
        Breeder user = breederRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (user.getRole() != Role.ADMINISTRATOR && user.getRole() != Role.MODERATOR) {
            throw new IllegalStateException(
                    "Brak uprawnień. Planami lotów mogą zarządzać wyłącznie administratorzy i moderatorzy."
            );
        }

        return user;
    }

    private void validateEntryRequest(
            FlightPlan plan,
            FlightPlanEntryRequest request,
            Long editedEntryId
    ) {
        if (request.scheduledDate().getYear() != plan.getYear()) {
            throw new IllegalArgumentException(
                    "Data lotu musi należeć do roku planu: " + plan.getYear() + "."
            );
        }

        boolean duplicateSortOrder;

        if (editedEntryId == null) {
            duplicateSortOrder =
                    flightPlanEntryRepository.existsByFlightPlanIdAndPigeonAgeGroupAndSortOrder(
                            plan.getId(),
                            request.pigeonAgeGroup(),
                            request.sortOrder()
                    );
        } else {
            duplicateSortOrder =
                    flightPlanEntryRepository.existsByFlightPlanIdAndPigeonAgeGroupAndSortOrderAndIdNot(
                            plan.getId(),
                            request.pigeonAgeGroup(),
                            request.sortOrder(),
                            editedEntryId
                    );
        }

        if (duplicateSortOrder) {
            throw new IllegalArgumentException(
                    "Pozycja o numerze " + request.sortOrder()
                            + " już istnieje w tym planie dla wybranej grupy gołębi."
            );
        }
    }

    private void applyEntryRequest(FlightPlanEntry entry, FlightPlanEntryRequest request) {
        entry.setPigeonAgeGroup(request.pigeonAgeGroup());
        entry.setScheduledDate(request.scheduledDate());
        entry.setLocation(request.location().trim());
        entry.setDistanceKm(request.distanceKm());
        entry.setCategory(trimToNull(request.category()));
        entry.setListType(request.listType().trim());
        entry.setSortOrder(request.sortOrder());
    }

    private FlightPlanEntryDto mapEntryToDto(FlightPlanEntry entry) {
        List<FlightResultSummaryDto> results = entry.getResults()
                .stream()
                .sorted(
                        Comparator
                                .comparingInt((FlightResult result) ->
                                        result.getScope() == FlightResultScope.BRANCH ? 0 : 1
                                )
                                .thenComparingInt(result ->
                                        result.getSection() != null
                                                ? result.getSection().getSortOrder()
                                                : 0
                                )
                )
                .map(this::mapResultToDto)
                .toList();

        return new FlightPlanEntryDto(
                entry.getId(),
                entry.getPigeonAgeGroup(),
                entry.getScheduledDate(),
                entry.getLocation(),
                entry.getDistanceKm(),
                entry.getCategory(),
                entry.getListType(),
                entry.getSortOrder(),
                results
        );
    }

    private FlightResultSummaryDto mapResultToDto(FlightResult result) {
        Section section = result.getSection();

        return new FlightResultSummaryDto(
                result.getId(),
                result.getScope(),
                section != null ? section.getId() : null,
                section != null ? section.getName() : null,
                section != null ? section.getSortOrder() : null,
                result.getOriginalFileName(),
                result.getActualFlightDate()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}