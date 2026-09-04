package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumThreadDto;
import com.pzhgp.backend.dto.ForumThreadRequest;
import com.pzhgp.backend.dto.ThreadAction;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumCategoryRepository;
import com.pzhgp.backend.repository.ForumPostRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForumThreadService Unit Tests")
class ForumThreadServiceTest {

    @Mock
    private ForumThreadRepository threadRepository;
    @Mock
    private ForumCategoryRepository categoryRepository;
    @Mock
    private BreederRepository breederRepository;
    @Mock
    private ForumPostRepository postRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ForumThreadService threadService;

    private Breeder author;
    private Breeder admin;
    private Breeder moderator;
    private Breeder randomUser;
    private ForumCategory category;
    private ForumThread thread;

    @BeforeEach
    void setUp() {
        author = new Breeder();
        author.setId(1L);
        author.setEmail("author@test.com");
        author.setName("Jan");
        author.setSurname("Kowalski");
        author.setRole(Role.BREEDER);
        author.setStatus(AccountStatus.ACTIVE);

        admin = new Breeder();
        admin.setId(2L);
        admin.setEmail("admin@test.com");
        admin.setRole(Role.ADMINISTRATOR);

        moderator = new Breeder();
        moderator.setId(3L);
        moderator.setEmail("mod@test.com");
        moderator.setRole(Role.MODERATOR);

        randomUser = new Breeder();
        randomUser.setId(4L);
        randomUser.setEmail("random@test.com");
        randomUser.setRole(Role.BREEDER);

        category = new ForumCategory();
        category.setId(10L);

        thread = new ForumThread();
        thread.setId(100L);
        thread.setAuthor(author);
        thread.setCategory(category);
        thread.setTitle("Testowy wątek");
        thread.setIsLocked(false);
        thread.setIsPinned(false);
        thread.setViews(0);
        thread.setRepliesCount(0);
    }

    @Test
    @DisplayName("Should create a new thread, save its initial post, and notify active breeders")
    void createThread_ShouldSaveThreadAndFirstPostAndNotify() {
        ForumThreadRequest request = new ForumThreadRequest(10L, "Nowy Wątek", "Treść pierwszego posta");
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));
        when(threadRepository.save(any(ForumThread.class))).thenReturn(thread);

        Breeder otherActiveBreeder = new Breeder();
        otherActiveBreeder.setId(5L);
        when(breederRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(List.of(author, otherActiveBreeder));

        threadService.createThread(request, "author@test.com");

        ArgumentCaptor<ForumThread> threadCaptor = ArgumentCaptor.forClass(ForumThread.class);
        verify(threadRepository, times(1)).save(threadCaptor.capture());
        assertEquals("Nowy Wątek", threadCaptor.getValue().getTitle());
        assertEquals(author, threadCaptor.getValue().getAuthor());

        ArgumentCaptor<ForumPost> postCaptor = ArgumentCaptor.forClass(ForumPost.class);
        verify(postRepository, times(1)).save(postCaptor.capture());
        assertEquals("Treść pierwszego posta", postCaptor.getValue().getBody());

        verify(notificationService, times(1)).createBulkNotifications(
                argThat(list -> list.size() == 1 && list.getFirst().getId().equals(5L)),
                eq("Jan Kowalski dodał/a nowy wątek na forum"),
                eq("/forum/thread/100"),
                eq(NotificationType.NEW_THREAD)
        );
    }

    @Test
    @DisplayName("Should return mapped page of threads when category exists")
    void getThreadsByCategory_Success() {
        thread.setRepliesCount(1); // Symulujemy działanie @Formula (np. 1 odpowiedź)
        Page<ForumThread> page = new PageImpl<>(List.of(thread));

        when(categoryRepository.existsById(10L)).thenReturn(true);
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));
        when(threadRepository.findByCategoryId(eq(10L), any(Pageable.class))).thenReturn(page);

        Page<ForumThreadDto> result = threadService.getThreadsByCategory(10L, 0, 10, "author@test.com");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        ForumThreadDto dto = result.getContent().getFirst();

        assertEquals(100L, dto.id());
        assertEquals("Testowy wątek", dto.title());
        assertEquals("Jan Kowalski", dto.authorName());
        assertEquals(10L, dto.categoryId());
        assertEquals(1, dto.repliesCount());
        assertTrue(dto.canDelete());
        assertFalse(dto.canModerate());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting threads for non-existent category")
    void getThreadsByCategory_CategoryNotFound() {
        when(categoryRepository.existsById(10L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> {
            threadService.getThreadsByCategory(10L, 0, 10, "author@test.com");
        });

        verify(threadRepository, never()).findByCategoryId(anyLong(), any());
    }

    @Test
    @DisplayName("Should increment views and return thread with Moderator permissions")
    void getThreadAndIncrementViews_WithModeratorPermissions() {
        thread.setRepliesCount(4);
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("mod@test.com")).thenReturn(Optional.of(moderator));

        assertEquals(0, thread.getViews());

        ForumThreadDto result = threadService.getThreadAndIncrementViews(100L, "mod@test.com");

        assertEquals(1, thread.getViews());
        assertNotNull(result);
        assertTrue(result.canDelete());
        assertTrue(result.canModerate());
        assertFalse(result.canEdit()); // Mod nie jest autorem, więc nie edytuje tytułu
    }

    @Test
    @DisplayName("Should increment views and return thread with regular user permissions")
    void getThreadAndIncrementViews_WithRandomUserPermissions() {
        thread.setRepliesCount(2);
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("random@test.com")).thenReturn(Optional.of(randomUser));

        assertEquals(0, thread.getViews());

        ForumThreadDto result = threadService.getThreadAndIncrementViews(100L, "random@test.com");

        assertEquals(1, thread.getViews());
        assertNotNull(result);
        assertFalse(result.canDelete());
        assertFalse(result.canModerate());
        assertFalse(result.canEdit());
    }

    @Test
    @DisplayName("Should NOT increment views when requester is the author of the thread")
    void getThreadAndIncrementViews_WhenRequesterIsAuthor_ShouldNotIncrementViews() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));

        assertEquals(0, thread.getViews());

        ForumThreadDto result = threadService.getThreadAndIncrementViews(100L, "author@test.com");

        assertEquals(0, thread.getViews()); // Licznik pozostaje bez zmian
        assertTrue(result.canEdit());
        assertTrue(result.canDelete());
        assertFalse(result.canModerate()); // Hodowca nie zamyka ani nie przypina
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when thread is not found")
    void getThreadAndIncrementViews_WhenThreadNotFound_ShouldThrowException() {
        when(threadRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            threadService.getThreadAndIncrementViews(999L, "author@test.com");
        });
    }

    @Test
    @DisplayName("Author should be able to delete their own thread along with all posts")
    void deleteThread_WhenRequesterIsAuthor_ShouldDeleteThreadAndPosts() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));

        threadService.deleteThread(100L, "author@test.com");

        verify(postRepository, times(1)).deleteAllByThreadId(100L);
        verify(threadRepository, times(1)).delete(thread);
    }

    @Test
    @DisplayName("Administrator should be able to delete any thread")
    void deleteThread_WhenRequesterIsAdmin_ShouldDeleteThreadAndPosts() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        threadService.deleteThread(100L, "admin@test.com");

        verify(postRepository, times(1)).deleteAllByThreadId(100L);
        verify(threadRepository, times(1)).delete(thread);
    }

    @Test
    @DisplayName("Moderator should be able to delete any thread created by a Breeder")
    void deleteThread_WhenRequesterIsModerator_ShouldDeleteThreadAndPosts() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("mod@test.com")).thenReturn(Optional.of(moderator));

        threadService.deleteThread(100L, "mod@test.com");

        verify(postRepository, times(1)).deleteAllByThreadId(100L);
        verify(threadRepository, times(1)).delete(thread);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when an unauthorized user tries to delete a thread")
    void deleteThread_WhenRequesterIsRandomUser_ShouldThrowException() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("random@test.com")).thenReturn(Optional.of(randomUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            threadService.deleteThread(100L, "random@test.com");
        });

        assertEquals("Brak uprawnień do usunięcia tego wątku.", exception.getMessage());
        verify(postRepository, never()).deleteAllByThreadId(anyLong());
        verify(threadRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when trying to delete a non-existent thread")
    void deleteThread_WhenThreadNotFound_ShouldThrowEntityNotFoundException() {
        when(threadRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            threadService.deleteThread(999L, "author@test.com");
        });
    }

    @Test
    @DisplayName("Admin should be able to toggle lock status both ways")
    void toggleThreadStatus_AdminShouldToggleLock() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        assertFalse(thread.getIsLocked());

        threadService.toggleThreadStatus(100L, "admin@test.com", ThreadAction.LOCK);
        assertTrue(thread.getIsLocked());

        threadService.toggleThreadStatus(100L, "admin@test.com", ThreadAction.LOCK);
        assertFalse(thread.getIsLocked());
    }

    @Test
    @DisplayName("Moderator should be able to toggle pin status both ways")
    void toggleThreadStatus_ModeratorShouldTogglePin() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("mod@test.com")).thenReturn(Optional.of(moderator));

        assertFalse(thread.getIsPinned());

        threadService.toggleThreadStatus(100L, "mod@test.com", ThreadAction.PIN);
        assertTrue(thread.getIsPinned());

        threadService.toggleThreadStatus(100L, "mod@test.com", ThreadAction.PIN);
        assertFalse(thread.getIsPinned());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when normal breeder tries to moderate (even if author)")
    void toggleThreadStatus_BreederShouldThrowException() {
        when(threadRepository.findById(100L)).thenReturn(Optional.of(thread));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));

        assertThrows(IllegalStateException.class, () -> {
            threadService.toggleThreadStatus(100L, "author@test.com", ThreadAction.LOCK);
        });
    }
}