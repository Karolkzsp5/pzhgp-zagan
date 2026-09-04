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
    private ForumThreadRepository threadRepository;

    @Mock
    private BreederRepository breederRepository;

    @InjectMocks
    private ForumCategoryService categoryService;

    private ForumCategory category;
    private ForumCategoryRequest request;
    private Breeder admin;
    private Breeder moderator;
    private Breeder breeder;

    @BeforeEach
    void setUp() {
        admin = new Breeder();
        admin.setId(1L);
        admin.setEmail("admin@test.pl");
        admin.setRole(Role.ADMINISTRATOR);

        moderator = new Breeder();
        moderator.setId(2L);
        moderator.setEmail("mod@test.pl");
        moderator.setRole(Role.MODERATOR);

        breeder = new Breeder();
        breeder.setId(3L);
        breeder.setEmail("breeder@test.pl");
        breeder.setRole(Role.BREEDER);

        category = new ForumCategory();
        category.setId(1L);
        category.setName("Choroby i leczenie");
        category.setDescription("Opis kategorii");
        category.setSortOrder(1);
        category.setAuthor(admin);

        request = new ForumCategoryRequest("Nowa nazwa", "Nowy opis", 2);
    }

    @Test
    @DisplayName("Should return all categories sorted by sortOrder with mapped flags for Admin")
    void getAllCategories_AsAdmin_ShouldReturnList() {
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));
        when(categoryRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(category));

        List<ForumCategoryDto> result = categoryService.getAllCategories("admin@test.pl");

        assertFalse(result.isEmpty());
        ForumCategoryDto dto = result.getFirst();
        assertEquals("Choroby i leczenie", dto.name());
        assertTrue(dto.canEdit());
        assertTrue(dto.canDelete());
        verify(categoryRepository, times(1)).findAllByOrderBySortOrderAscNameAsc();
    }

    @Test
    @DisplayName("Should return mapped flags canEdit=false, canDelete=false for Moderator viewing Admin's category")
    void getAllCategories_AsModerator_ShouldReturnCorrectFlags() {
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));
        when(categoryRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(category));

        List<ForumCategoryDto> result = categoryService.getAllCategories("mod@test.pl");

        assertFalse(result.isEmpty());
        ForumCategoryDto dto = result.getFirst();
        assertFalse(dto.canEdit());
        assertFalse(dto.canDelete());
    }

    @Test
    @DisplayName("Should return mapped flags canEdit=false, canDelete=false for Breeder viewing Admin's category")
    void getAllCategories_AsBreeder_ShouldReturnCorrectFlags() {
        when(breederRepository.findByEmail("breeder@test.pl")).thenReturn(Optional.of(breeder));
        when(categoryRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(category));

        List<ForumCategoryDto> result = categoryService.getAllCategories("breeder@test.pl");

        assertFalse(result.isEmpty());
        ForumCategoryDto dto = result.getFirst();
        assertFalse(dto.canEdit());
        assertFalse(dto.canDelete());
    }

    @Test
    @DisplayName("Should save new category when requester is Admin")
    void createCategory_AsAdmin_ShouldSaveToRepository() {
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));

        categoryService.createCategory(request, "admin@test.pl");

        ArgumentCaptor<ForumCategory> captor = ArgumentCaptor.forClass(ForumCategory.class);
        verify(categoryRepository, times(1)).save(captor.capture());

        ForumCategory saved = captor.getValue();
        assertEquals("Nowa nazwa", saved.getName());
        assertEquals("Nowy opis", saved.getDescription());
        assertEquals(2, saved.getSortOrder());
        assertEquals(admin, saved.getAuthor());
    }

    @Test
    @DisplayName("Should save new category when requester is Moderator")
    void createCategory_AsModerator_ShouldSaveToRepository() {
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));

        categoryService.createCategory(request, "mod@test.pl");

        ArgumentCaptor<ForumCategory> captor = ArgumentCaptor.forClass(ForumCategory.class);
        verify(categoryRepository).save(captor.capture());

        assertEquals("Nowa nazwa", captor.getValue().getName());
        assertEquals(moderator, captor.getValue().getAuthor());
    }

    @Test
    @DisplayName("Should throw exception when Breeder tries to create a category")
    void createCategory_AsBreeder_ShouldThrowException() {
        when(breederRepository.findByEmail("breeder@test.pl")).thenReturn(Optional.of(breeder));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            categoryService.createCategory(request, "breeder@test.pl");
        });

        assertEquals("Brak uprawnień. Hodowcy nie mogą tworzyć kategorii.", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update category successfully when requester is the Author")
    void updateCategory_WhenRequesterIsAuthor_ShouldUpdateAndSave() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));

        categoryService.updateCategory(1L, request, "admin@test.pl");

        verify(categoryRepository, times(1)).save(category);
        assertEquals("Nowa nazwa", category.getName());
    }

    @Test
    @DisplayName("Should update category successfully when Admin edits Moderator's category")
    void updateCategory_AdminOnModeratorCategory_ShouldUpdate() {
        category.setAuthor(moderator);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));

        categoryService.updateCategory(1L, request, "admin@test.pl");

        verify(categoryRepository).save(category);
        assertEquals("Nowa nazwa", category.getName());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Admin edits another Admin's category")
    void updateCategory_AdminOnAnotherAdminCategory_ShouldThrowException() {
        Breeder anotherAdmin = new Breeder();
        anotherAdmin.setId(5L);
        anotherAdmin.setRole(Role.ADMINISTRATOR);
        category.setAuthor(anotherAdmin);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class, () -> {
            categoryService.updateCategory(1L, request, "admin@test.pl");
        });

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Moderator tries to edit Admin's category")
    void updateCategory_AsModeratorOnAdminCategory_ShouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));

        assertThrows(IllegalStateException.class, () -> {
            categoryService.updateCategory(1L, request, "mod@test.pl");
        });

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating non-existent category")
    void updateCategory_WhenNotFound_ShouldThrowException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.updateCategory(99L, request, "admin@test.pl");
        });
    }

    @Test
    @DisplayName("Should delete category if it has no threads and requester is Author (Admin)")
    void deleteCategory_WhenNoThreadsExistAndRequesterIsAdmin_ShouldRemoveFromRepository() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));
        when(threadRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L, "admin@test.pl");

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Should delete category when Admin deletes Moderator's category")
    void deleteCategory_AdminOnModeratorCategory_ShouldDelete() {
        category.setAuthor(moderator);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));
        when(threadRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L, "admin@test.pl");

        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Admin deletes another Admin's category")
    void deleteCategory_AdminOnAnotherAdminCategory_ShouldThrowException() {
        Breeder anotherAdmin = new Breeder();
        anotherAdmin.setId(5L);
        anotherAdmin.setRole(Role.ADMINISTRATOR);
        category.setAuthor(anotherAdmin);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));

        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(1L, "admin@test.pl");
        });

        verify(threadRepository, never()).existsByCategoryId(anyLong());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw exception when Moderator tries to delete a category")
    void deleteCategory_AsModerator_ShouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));

        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(1L, "mod@test.pl");
        });

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when deleting category that contains threads")
    void deleteCategory_WhenThreadsExist_ShouldThrowException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("admin@test.pl")).thenReturn(Optional.of(admin));
        when(threadRepository.existsByCategoryId(1L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(1L, "admin@test.pl");
        });

        assertEquals("Nie można usunąć kategorii, która zawiera wątki.", exception.getMessage());
        verify(categoryRepository, never()).delete(any());
    }
}