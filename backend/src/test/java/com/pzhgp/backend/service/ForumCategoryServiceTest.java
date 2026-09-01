package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumCategoryDto;
import com.pzhgp.backend.dto.ForumCategoryRequest;
import com.pzhgp.backend.entity.ForumCategory;
import com.pzhgp.backend.repository.ForumCategoryRepository;
import com.pzhgp.backend.repository.ForumTopicRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForumCategoryService Unit Tests")
class ForumCategoryServiceTest {

    @Mock
    private ForumCategoryRepository categoryRepository;

    @Mock
    private ForumTopicRepository topicRepository;

    @InjectMocks
    private ForumCategoryService categoryService;

    private ForumCategory category;
    private ForumCategoryRequest request;

    @BeforeEach
    void setUp() {
        category = new ForumCategory();
        category.setId(1L);
        category.setName("Choroby i leczenie");
        category.setDescription("Opis kategorii");
        category.setSortOrder(1);

        request = new ForumCategoryRequest("Nowa nazwa", "Nowy opis", 2);
    }

    @Test
    @DisplayName("Should return all categories sorted by sortOrder")
    void getAllCategories_ShouldReturnList() {
        when(categoryRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(category));

        List<ForumCategoryDto> result = categoryService.getAllCategories();

        assertFalse(result.isEmpty());
        assertEquals("Choroby i leczenie", result.getFirst().name());
        verify(categoryRepository, times(1)).findAllByOrderBySortOrderAsc();
    }

    @Test
    @DisplayName("Should save new category and map DTO correctly")
    void createCategory_ShouldSaveToRepository() {
        categoryService.createCategory(request);

        ArgumentCaptor<ForumCategory> captor = ArgumentCaptor.forClass(ForumCategory.class);
        verify(categoryRepository, times(1)).save(captor.capture());

        ForumCategory saved = captor.getValue();
        assertEquals("Nowa nazwa", saved.getName());
        assertEquals("Nowy opis", saved.getDescription());
        assertEquals(2, saved.getSortOrder());
    }

    @Test
    @DisplayName("Should update category successfully")
    void updateCategory_WhenFound_ShouldUpdateAndSave() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        categoryService.updateCategory(1L, request);

        verify(categoryRepository, times(1)).save(category);
        assertEquals("Nowa nazwa", category.getName());
        assertEquals("Nowy opis", category.getDescription());
        assertEquals(2, category.getSortOrder());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent category")
    void updateCategory_WhenNotFound_ShouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.updateCategory(99L, request);
        });

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete category if it has no topics")
    void deleteCategory_WhenNoTopicsExist_ShouldRemoveFromRepository() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(topicRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when deleting category that contains topics")
    void deleteCategory_WhenTopicsExist_ShouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(topicRepository.existsByCategoryId(1L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(1L);
        });

        assertEquals("Nie można usunąć kategorii, która zawiera tematy.", exception.getMessage());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting non-existent category")
    void deleteCategory_WhenNotFound_ShouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.deleteCategory(99L);
        });

        verify(categoryRepository, never()).delete(any());
        verify(topicRepository, never()).existsByCategoryId(anyLong());
    }
}