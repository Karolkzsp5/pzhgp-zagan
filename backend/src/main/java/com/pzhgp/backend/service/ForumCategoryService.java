package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumCategoryDto;
import com.pzhgp.backend.dto.ForumCategoryRequest;
import com.pzhgp.backend.entity.ForumCategory;
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

    @Transactional(readOnly = true)
    public List<ForumCategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ForumCategoryDto getCategoryById(Long id) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii o ID: " + id));
        return mapToDto(category);
    }

    @Transactional
    public void createCategory(ForumCategoryRequest request) {
        ForumCategory category = new ForumCategory();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder());

        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategory(Long id, ForumCategoryRequest request) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii o ID: " + id));

        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder());

        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        ForumCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii o ID: " + id));

        if (threadRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Nie można usunąć kategorii, która zawiera tematy.");
        }

        categoryRepository.delete(category);
    }

    private ForumCategoryDto mapToDto(ForumCategory category) {
        return new ForumCategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                category.getCreatedAt()
        );
    }
}