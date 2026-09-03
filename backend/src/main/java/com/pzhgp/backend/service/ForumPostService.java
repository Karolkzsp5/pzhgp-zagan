package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumPostDto;
import com.pzhgp.backend.dto.ForumPostRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumPostRepository;
import com.pzhgp.backend.repository.ForumThreadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForumPostService {

    private final ForumPostRepository postRepository;
    private final ForumThreadRepository threadRepository;
    private final BreederRepository breederRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<ForumPostDto> getPostsByThread(Long threadId, int page, int size, String requesterEmail) {
        if (!threadRepository.existsById(threadId)) {
            throw new EntityNotFoundException("Nie znaleziono wątku o ID: " + threadId);
        }

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));

        return postRepository.findByThreadId(threadId, pageable).map(post -> mapToDto(post, requester));
    }

    @Transactional
    public void addPost(Long threadId, ForumPostRequest request, String authorEmail) {
        ForumThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));

        if (thread.getIsLocked()) {
            throw new IllegalStateException("Wątek jest zamknięty. Nie można dodawać nowych odpowiedzi.");
        }

        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        ForumPost post = new ForumPost();
        post.setThread(thread);
        post.setAuthor(author);
        post.setBody(request.body());
        postRepository.save(post);

        thread.setLastPostAt(LocalDateTime.now());
        threadRepository.save(thread);

        if (!thread.getAuthor().getId().equals(author.getId())) {
            String authorFullName = author.getName() + " " + author.getSurname();
            notificationService.createNotification(
                    thread.getAuthor().getId(),
                    authorFullName + " dodał/a odpowiedź w twoim wątku na forum",
                    "/forum/thread/" + thread.getId(),
                    NotificationType.NEW_REPLY
            );
        }
    }

    @Transactional
    public void updatePost(Long postId, ForumPostRequest request, String requesterEmail) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wpisu o ID: " + postId));

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (!post.getAuthor().getId().equals(requester.getId())) {
            throw new IllegalStateException("Brak uprawnień. Możesz edytować tylko własne wpisy.");
        }

        post.setBody(request.body());
        post.setEditedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId, String requesterEmail) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wpisu o ID: " + postId));

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        boolean isAuthor = post.getAuthor().getId().equals(requester.getId());
        boolean hasPrivileges = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;

        if (!isAuthor && !hasPrivileges) {
            throw new IllegalStateException("Brak uprawnień do usunięcia tego wpisu.");
        }

        if (postRepository.countByThreadId(post.getThread().getId()) <= 1) {
            throw new IllegalStateException("Nie można usunąć jedynego wpisu w wątku. Aby to zrobić, usuń cały wątek.");
        }

        postRepository.delete(post);
    }

    private ForumPostDto mapToDto(ForumPost post, Breeder requester) {
        String authorFullName = post.getAuthor().getName() + " " + post.getAuthor().getSurname();
        boolean isAuthor = post.getAuthor().getId().equals(requester.getId());
        boolean hasPrivileges = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;

        return new ForumPostDto(
                post.getId(),
                authorFullName,
                post.getBody(),
                post.getCreatedAt(),
                post.getEditedAt(),
                isAuthor,
                isAuthor || hasPrivileges
        );
    }
}