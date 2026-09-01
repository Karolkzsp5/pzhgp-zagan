package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumPostDto;
import com.pzhgp.backend.dto.ForumPostRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForumPostService Unit Tests")
class ForumPostServiceTest {

    @Mock
    private ForumPostRepository postRepository;
    @Mock
    private ForumTopicRepository topicRepository;
    @Mock
    private BreederRepository breederRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ForumPostService postService;

    private Breeder topicAuthor;
    private Breeder postAuthor;
    private Breeder moderator;
    private ForumTopic topic;
    private ForumPost post;

    @BeforeEach
    void setUp() {
        topicAuthor = new Breeder();
        topicAuthor.setId(1L);
        topicAuthor.setEmail("topic.author@test.pl");
        topicAuthor.setName("Jan");
        topicAuthor.setSurname("Kowalski");
        topicAuthor.setRole(Role.BREEDER);

        postAuthor = new Breeder();
        postAuthor.setId(2L);
        postAuthor.setEmail("post.author@test.pl");
        postAuthor.setName("Piotr");
        postAuthor.setSurname("Nowak");
        postAuthor.setRole(Role.BREEDER);

        moderator = new Breeder();
        moderator.setId(3L);
        moderator.setEmail("mod@test.pl");
        moderator.setRole(Role.MODERATOR);

        topic = new ForumTopic();
        topic.setId(100L);
        topic.setAuthor(topicAuthor);
        topic.setTitle("Tytuł Tematu");
        topic.setIsLocked(false);
        topic.setLastPostAt(LocalDateTime.now().minusDays(1));

        post = new ForumPost();
        post.setId(500L);
        post.setTopic(topic);
        post.setAuthor(postAuthor);
        post.setBody("Stara treść posta");
    }


    @Test
    @DisplayName("Should add post, explicitly update topic lastPostAt, and send NEW_REPLY notification")
    void addPost_ShouldSaveAndNotify() {
        ForumPostRequest request = new ForumPostRequest("Odpowiedź do tematu");
        LocalDateTime oldLastPostAt = topic.getLastPostAt();

        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("post.author@test.pl")).thenReturn(Optional.of(postAuthor));
        when(postRepository.save(any(ForumPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        postService.addPost(100L, request, "post.author@test.pl");

        ArgumentCaptor<ForumPost> postCaptor = ArgumentCaptor.forClass(ForumPost.class);
        verify(postRepository, times(1)).save(postCaptor.capture());
        assertEquals("Odpowiedź do tematu", postCaptor.getValue().getBody());
        assertEquals(postAuthor, postCaptor.getValue().getAuthor());
        assertEquals(topic, postCaptor.getValue().getTopic());

        verify(topicRepository, times(1)).save(topic);
        assertTrue(topic.getLastPostAt().isAfter(oldLastPostAt));

        verify(notificationService, times(1)).createNotification(
                eq(1L),
                contains("Piotr Nowak odpowiedział"),
                eq("/forum/topic/100"),
                eq(NotificationType.NEW_REPLY)
        );
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when topic does not exist on addPost")
    void addPost_WhenTopicNotFound_ShouldThrowException() {
        when(topicRepository.findById(999L)).thenReturn(Optional.empty());
        ForumPostRequest request = new ForumPostRequest("Odpowiedź");

        assertThrows(EntityNotFoundException.class, () -> {
            postService.addPost(999L, request, "post.author@test.pl");
        });

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when trying to add post to a locked topic")
    void addPost_WhenTopicIsLocked_ShouldThrowException() {
        topic.setIsLocked(true);
        ForumPostRequest request = new ForumPostRequest("Odpowiedź do zamkniętego");

        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            postService.addPost(100L, request, "post.author@test.pl");
        });

        assertEquals("Temat jest zamknięty. Nie można dodawać nowych odpowiedzi.", exception.getMessage());
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist on addPost")
    void addPost_WhenUserNotFound_ShouldThrowException() {
        when(topicRepository.findById(100L)).thenReturn(Optional.of(topic));
        when(breederRepository.findByEmail("unknown@test.pl")).thenReturn(Optional.empty());

        ForumPostRequest request = new ForumPostRequest("Odpowiedź widmo");

        assertThrows(EntityNotFoundException.class, () -> {
            postService.addPost(100L, request, "unknown@test.pl");
        });

        verify(postRepository, never()).save(any());
    }


    @Test
    @DisplayName("Author should be able to edit their own post")
    void updatePost_WhenRequesterIsAuthor_ShouldUpdateBody() {
        ForumPostRequest request = new ForumPostRequest("Nowa, zedytowana treść");

        when(postRepository.findById(500L)).thenReturn(Optional.of(post));
        when(breederRepository.findByEmail("post.author@test.pl")).thenReturn(Optional.of(postAuthor));

        postService.updatePost(500L, request, "post.author@test.pl");

        assertEquals("Nowa, zedytowana treść", post.getBody());
        assertNotNull(post.getEditedAt());
        verify(postRepository, times(1)).save(post);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when editing a non-existent post")
    void updatePost_WhenPostNotFound_ShouldThrowException() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());
        ForumPostRequest request = new ForumPostRequest("Treść");

        assertThrows(EntityNotFoundException.class, () -> {
            postService.updatePost(999L, request, "post.author@test.pl");
        });
    }

    @Test
    @DisplayName("Should throw IllegalStateException when someone else tries to edit a post")
    void updatePost_WhenRequesterIsNotAuthor_ShouldThrowException() {
        ForumPostRequest request = new ForumPostRequest("Próba włamania do treści");

        when(postRepository.findById(500L)).thenReturn(Optional.of(post));
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));

        assertThrows(IllegalStateException.class, () -> {
            postService.updatePost(500L, request, "mod@test.pl");
        });

        assertEquals("Stara treść posta", post.getBody());
        verify(postRepository, never()).save(any());
    }


    @Test
    @DisplayName("Moderator should be able to delete any post")
    void deletePost_WhenRequesterIsModerator_ShouldDelete() {
        when(postRepository.findById(500L)).thenReturn(Optional.of(post));
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));

        postService.deletePost(500L, "mod@test.pl");

        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("Author should be able to delete their own post")
    void deletePost_WhenRequesterIsAuthor_ShouldDelete() {
        when(postRepository.findById(500L)).thenReturn(Optional.of(post));
        when(breederRepository.findByEmail("post.author@test.pl")).thenReturn(Optional.of(postAuthor));

        postService.deletePost(500L, "post.author@test.pl");

        verify(postRepository, times(1)).delete(post);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a non-existent post")
    void deletePost_WhenPostNotFound_ShouldThrowException() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            postService.deletePost(999L, "mod@test.pl");
        });
    }

    @Test
    @DisplayName("Should throw IllegalStateException when random user tries to delete a post")
    void deletePost_WhenRequesterIsRandomUser_ShouldThrowException() {
        when(postRepository.findById(500L)).thenReturn(Optional.of(post));
        when(breederRepository.findByEmail("topic.author@test.pl")).thenReturn(Optional.of(topicAuthor));

        assertThrows(IllegalStateException.class, () -> {
            postService.deletePost(500L, "topic.author@test.pl");
        });

        verify(postRepository, never()).delete(any());
    }


    @Test
    @DisplayName("Should return mapped page of posts with edit=true and delete=true for Author")
    void getPostsByTopic_WhenRequesterIsAuthor_ShouldMapFlagsCorrectly() {
        Page<ForumPost> page = new PageImpl<>(List.of(post));

        when(topicRepository.existsById(100L)).thenReturn(true);
        when(breederRepository.findByEmail("post.author@test.pl")).thenReturn(Optional.of(postAuthor));
        when(postRepository.findByTopicId(eq(100L), any(Pageable.class))).thenReturn(page);

        Page<ForumPostDto> result = postService.getPostsByTopic(100L, 0, 10, "post.author@test.pl");

        assertNotNull(result);
        ForumPostDto dto = result.getContent().getFirst();
        assertTrue(dto.canEdit());
        assertTrue(dto.canDelete());
    }

    @Test
    @DisplayName("Should return mapped page of posts with edit=false and delete=true for Moderator")
    void getPostsByTopic_WhenRequesterIsModerator_ShouldMapFlagsCorrectly() {
        Page<ForumPost> page = new PageImpl<>(List.of(post));

        when(topicRepository.existsById(100L)).thenReturn(true);
        when(breederRepository.findByEmail("mod@test.pl")).thenReturn(Optional.of(moderator));
        when(postRepository.findByTopicId(eq(100L), any(Pageable.class))).thenReturn(page);

        Page<ForumPostDto> result = postService.getPostsByTopic(100L, 0, 10, "mod@test.pl");

        assertNotNull(result);
        ForumPostDto dto = result.getContent().getFirst();
        assertFalse(dto.canEdit());
        assertTrue(dto.canDelete());
    }

    @Test
    @DisplayName("Should return mapped page of posts with edit=false and delete=false for regular user")
    void getPostsByTopic_WhenRequesterIsRegularUser_ShouldMapFlagsCorrectly() {
        Page<ForumPost> page = new PageImpl<>(List.of(post));

        when(topicRepository.existsById(100L)).thenReturn(true);
        when(breederRepository.findByEmail("topic.author@test.pl")).thenReturn(Optional.of(topicAuthor));
        when(postRepository.findByTopicId(eq(100L), any(Pageable.class))).thenReturn(page);

        Page<ForumPostDto> result = postService.getPostsByTopic(100L, 0, 10, "topic.author@test.pl");

        assertNotNull(result);
        ForumPostDto dto = result.getContent().getFirst();

        assertFalse(dto.canEdit());
        assertFalse(dto.canDelete());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when fetching posts for non-existent topic")
    void getPostsByTopic_WhenTopicNotFound_ShouldThrowException() {
        when(topicRepository.existsById(999L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> {
            postService.getPostsByTopic(999L, 0, 10, "post.author@test.pl");
        });

        verify(postRepository, never()).findByTopicId(anyLong(), any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user does not exist on getPostsByTopic")
    void getPostsByTopic_WhenUserNotFound_ShouldThrowException() {
        when(topicRepository.existsById(100L)).thenReturn(true);
        when(breederRepository.findByEmail("unknown@test.pl")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            postService.getPostsByTopic(100L, 0, 10, "unknown@test.pl");
        });

        verify(postRepository, never()).findByTopicId(anyLong(), any());
    }
}