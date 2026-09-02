package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumPostDto;
import com.pzhgp.backend.dto.ForumPostRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumPostRepository;
import com.pzhgp.backend.repository.ForumTopicRepository;
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
    private final ForumTopicRepository topicRepository;
    private final BreederRepository breederRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<ForumPostDto> getPostsByTopic(Long topicId, int page, int size, String requesterEmail) {
        if (!topicRepository.existsById(topicId)) {
            throw new EntityNotFoundException("Nie znaleziono tematu o ID: " + topicId);
        }

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));

        return postRepository.findByTopicId(topicId, pageable).map(post -> mapToDto(post, requester));
    }

    @Transactional
    public void addPost(Long topicId, ForumPostRequest request, String authorEmail) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tematu."));

        if (topic.getIsLocked()) {
            throw new IllegalStateException("Wątek jest zamknięty. Nie można dodawać nowych odpowiedzi.");
        }

        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        ForumPost post = new ForumPost();
        post.setTopic(topic);
        post.setAuthor(author);
        post.setBody(request.body());
        postRepository.save(post);

        topic.setLastPostAt(LocalDateTime.now());
        topicRepository.save(topic);

        if (!topic.getAuthor().getId().equals(author.getId())) {
            String authorFullName = author.getName() + " " + author.getSurname();
            notificationService.createNotification(
                    topic.getAuthor().getId(),
                    authorFullName + " dodał/a odpowiedź w twoim wątku: " + topic.getTitle(),
                    "/forum/topic/" + topic.getId(),
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