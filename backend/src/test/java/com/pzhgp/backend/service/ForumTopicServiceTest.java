package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumTopicDto;
import com.pzhgp.backend.dto.ForumTopicRequest;
import com.pzhgp.backend.dto.TopicAction;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumCategoryRepository;
import com.pzhgp.backend.repository.ForumPostRepository;
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
@DisplayName("ForumTopicService Unit Tests")
class ForumTopicServiceTest {

    @Mock
    private ForumTopicRepository topicRepository;
    @Mock
    private ForumCategoryRepository categoryRepository;
    @Mock
    private BreederRepository breederRepository;
    @Mock
    private ForumPostRepository postRepository;

    @InjectMocks
    private ForumTopicService topicService;

    private Breeder author;
    private Breeder admin;
    private Breeder moderator;
    private Breeder randomUser;
    private ForumCategory category;
    private ForumTopic topic;

    @BeforeEach
    void setUp() {
        author = new Breeder();
        author.setId(1L);
        author.setEmail("author@test.com");
        author.setName("Jan");
        author.setSurname("Kowalski");
        author.setRole(Role.BREEDER);

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

        topic = new ForumTopic();
        topic.setId(100L);
        topic.setAuthor(author);
        topic.setCategory(category);
        topic.setTitle("Testowy temat");
        topic.setIsLocked(false);
        topic.setIsPinned(false);
        topic.setViews(0);
    }


    @Test
    @DisplayName("Should create a new topic and save its initial post with correct relations")
    void createTopic_ShouldSaveTopicAndFirstPost() {
        ForumTopicRequest request = new ForumTopicRequest(10L, "Nowy Temat", "Treść pierwszego posta");
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));
        when(topicRepository.save(any(ForumTopic.class))).thenReturn(topic);

        topicService.createTopic(request, "author@test.com");

        ArgumentCaptor<ForumTopic> topicCaptor = ArgumentCaptor.forClass(ForumTopic.class);
        verify(topicRepository, times(1)).save(topicCaptor.capture());
        assertEquals("Nowy Temat", topicCaptor.getValue().getTitle());
        assertEquals(author, topicCaptor.getValue().getAuthor());

        ArgumentCaptor<ForumPost> postCaptor = ArgumentCaptor.forClass(ForumPost.class);
        verify(postRepository, times(1)).save(postCaptor.capture());
        assertEquals("Treść pierwszego posta", postCaptor.getValue().getBody());
        assertEquals(author, postCaptor.getValue().getAuthor());
        assertEquals(topic, postCaptor.getValue().getTopic());
    }

    @Test
    @DisplayName("Should return mapped page of topics when category exists")
    void getTopicsByCategory_Success() {
        Page<ForumTopic> page = new PageImpl<>(List.of(topic));
        when(categoryRepository.existsById(10L)).thenReturn(true);
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));
        when(topicRepository.findByCategoryId(eq(10L), any(Pageable.class))).thenReturn(page);

        Page<ForumTopicDto> result = topicService.getTopicsByCategory(10L, 0, 10, "author@test.com");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        ForumTopicDto dto = result.getContent().getFirst();

        assertEquals(100L, dto.id());
        assertEquals("Testowy temat", dto.title());
        assertEquals("Jan Kowalski", dto.authorName());
        assertEquals(10L, dto.categoryId());
        assertTrue(dto.canDelete());
        assertFalse(dto.canModerate());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting topics for non-existent category")
    void getTopicsByCategory_CategoryNotFound() {
        when(categoryRepository.existsById(10L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> {
            topicService.getTopicsByCategory(10L, 0, 10, "author@test.com");
        });

        verify(topicRepository, never()).findByCategoryId(anyLong(), any());
    }


    @Test
    @DisplayName("Should increment views and return topic with Moderator permissions")
    void getTopicAndIncrementViews_WithModeratorPermissions() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("mod@test.com")).thenReturn(Optional.of(moderator));

        ForumTopicDto result = topicService.getTopicAndIncrementViews(100L, "mod@test.com");

        verify(topicRepository, times(1)).incrementViews(100L);
        assertNotNull(result);
        assertTrue(result.canDelete());
        assertTrue(result.canModerate());
    }

    @Test
    @DisplayName("Should increment views and return topic with regular user permissions")
    void getTopicAndIncrementViews_WithRandomUserPermissions() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("random@test.com")).thenReturn(Optional.of(randomUser));

        ForumTopicDto result = topicService.getTopicAndIncrementViews(100L, "random@test.com");

        verify(topicRepository, times(1)).incrementViews(100L);
        assertNotNull(result);
        assertFalse(result.canDelete());
        assertFalse(result.canModerate());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException and NOT increment views when topic is not found")
    void getTopicAndIncrementViews_WhenTopicNotFound_ShouldThrowException() {
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            topicService.getTopicAndIncrementViews(999L, "author@test.com");
        });

        verify(topicRepository, never()).incrementViews(anyLong());
    }


    @Test
    @DisplayName("Author should be able to delete their own topic along with all posts")
    void deleteTopic_WhenRequesterIsAuthor_ShouldDeleteTopicAndPosts() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));

        topicService.deleteTopic(100L, "author@test.com");

        verify(postRepository, times(1)).deleteAllByTopicId(100L);
        verify(topicRepository, times(1)).delete(topic);
    }

    @Test
    @DisplayName("Administrator should be able to delete any topic")
    void deleteTopic_WhenRequesterIsAdmin_ShouldDeleteTopicAndPosts() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        topicService.deleteTopic(100L, "admin@test.com");

        verify(postRepository, times(1)).deleteAllByTopicId(100L);
        verify(topicRepository, times(1)).delete(topic);
    }

    @Test
    @DisplayName("Moderator should be able to delete any topic")
    void deleteTopic_WhenRequesterIsModerator_ShouldDeleteTopicAndPosts() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("mod@test.com")).thenReturn(Optional.of(moderator));

        topicService.deleteTopic(100L, "mod@test.com");

        verify(postRepository, times(1)).deleteAllByTopicId(100L);
        verify(topicRepository, times(1)).delete(topic);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when an unauthorized user tries to delete a topic")
    void deleteTopic_WhenRequesterIsRandomUser_ShouldThrowException() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("random@test.com")).thenReturn(Optional.of(randomUser));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            topicService.deleteTopic(100L, "random@test.com");
        });

        assertEquals("Brak uprawnień do usunięcia tego tematu.", exception.getMessage());
        verify(postRepository, never()).deleteAllByTopicId(anyLong());
        verify(topicRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when trying to delete a non-existent topic")
    void deleteTopic_WhenTopicNotFound_ShouldThrowEntityNotFoundException() {
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            topicService.deleteTopic(999L, "author@test.com");
        });
    }


    @Test
    @DisplayName("Admin should be able to toggle lock status both ways")
    void toggleTopicStatus_AdminShouldToggleLock() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        assertFalse(topic.getIsLocked());

        topicService.toggleTopicStatus(100L, "admin@test.com", TopicAction.LOCK);
        assertTrue(topic.getIsLocked());

        topicService.toggleTopicStatus(100L, "admin@test.com", TopicAction.LOCK);
        assertFalse(topic.getIsLocked());
    }

    @Test
    @DisplayName("Moderator should be able to toggle pin status both ways")
    void toggleTopicStatus_ModeratorShouldTogglePin() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("mod@test.com")).thenReturn(Optional.of(moderator));

        assertFalse(topic.getIsPinned());

        topicService.toggleTopicStatus(100L, "mod@test.com", TopicAction.PIN);
        assertTrue(topic.getIsPinned());

        topicService.toggleTopicStatus(100L, "mod@test.com", TopicAction.PIN);
        assertFalse(topic.getIsPinned());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when normal breeder tries to moderate")
    void toggleTopicStatus_BreederShouldThrowException() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("author@test.com")).thenReturn(Optional.of(author));

        assertThrows(IllegalStateException.class, () -> {
            topicService.toggleTopicStatus(100L, "author@test.com", TopicAction.LOCK);
        });
    }
}