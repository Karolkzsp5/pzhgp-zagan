package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumCategoryDto;
import com.pzhgp.backend.dto.ForumCategoryRequest;
import com.pzhgp.backend.entity.Breeder;
import com.pzhgp.backend.entity.ForumCategory;
import com.pzhgp.backend.entity.Role;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumCategoryRepository;
import com.pzhgp.backend.repository.ForumThreadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumCategoryService {

    private final ForumCategoryRepository categoryRepository;
    private final ForumThreadRepository threadRepository;
    private final BreederRepository breederRepository;

    @Transactional(readOnly = true)
    public List<ForumCategoryDto> getAllCategories(String requesterEmail) {
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(category -> mapToDto(category, requester))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ForumCategoryDto getCategoryById(Long id, String requesterEmail) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii o ID: " + id));
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        return mapToDto(category, requester);
    }

    @Transactional
    public void createCategory(ForumCategoryRequest request, String authorEmail) {
        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        // Hodowca nie może dodawać kategorii
        if (author.getRole() == Role.BREEDER) {
            throw new IllegalStateException("Brak uprawnień. Hodowcy nie mogą tworzyć kategorii.");
        }

        ForumCategory category = new ForumCategory();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder());
        category.setAuthor(author);

        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategory(Long id, ForumCategoryRequest request, String requesterEmail) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii o ID: " + id));
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (!canEditCategory(category.getAuthor(), requester)) {
            throw new IllegalStateException("Brak uprawnień. Administratorzy edytują kategorie poniżej swojej rangi, a moderatorzy tylko własne.");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder());

        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id, String requesterEmail) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii o ID: " + id));
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (!canDeleteCategory(category.getAuthor(), requester)) {
            throw new IllegalStateException("Brak uprawnień. Tylko administrator może usuwać kategorie (z wyjątkiem tych utworzonych przez innych administratorów).");
        }

        if (threadRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Nie można usunąć kategorii, która zawiera wątki.");
        }

        categoryRepository.delete(category);
    }

    // --- LOGIKA UPRAWNIEŃ ---

    private boolean canEditCategory(Breeder author, Breeder requester) {
        // Moderator i Administrator mogą edytować własne
        if (author.getId().equals(requester.getId())) {
            return true;
        }
        // Administrator może edytować kategorie, ale nie te od innego administratora
        if (requester.getRole() == Role.ADMINISTRATOR) {
            return author.getRole() != Role.ADMINISTRATOR;
        }
        return false;
    }

    private boolean canDeleteCategory(Breeder author, Breeder requester) {
        // Tylko Administrator może usuwać (z wyjątkiem kategorii innego administratora)
        if (requester.getRole() == Role.ADMINISTRATOR) {
            return author.getId().equals(requester.getId()) || author.getRole() != Role.ADMINISTRATOR;
        }
        return false;
    }

    private ForumCategoryDto mapToDto(ForumCategory category, Breeder requester) {
        return new ForumCategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                category.getCreatedAt(),
                canEditCategory(category.getAuthor(), requester),
                canDeleteCategory(category.getAuthor(), requester)
        );
    }
}