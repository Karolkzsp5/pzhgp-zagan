package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumPostDto;
import com.pzhgp.backend.dto.ForumPostRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.exception.ResourceConflictException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumPostRepository;
import com.pzhgp.backend.repository.ForumThreadRepository;
import com.pzhgp.backend.utils.PaginationUtils;
import com.pzhgp.backend.utils.RichTextSanitizer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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

        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        PaginationUtils.validate(page, size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));

        return postRepository.findByThreadId(threadId, pageable).map(post -> mapToDto(post, requester));
    }

    @Transactional
    public void addPost(Long threadId, ForumPostRequest request, String authorEmail) {
        ForumThread thread = threadRepository.findByIdForUpdate(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));

        if (thread.isLocked()) {
            throw new ResourceConflictException("Wątek jest zamknięty. Nie można dodawać nowych odpowiedzi.");
        }

        Breeder author = breederRepository.requireByEmail(authorEmail);

        ForumPost post = new ForumPost();
        post.setThread(thread);
        post.setAuthor(author);
        post.setBody(RichTextSanitizer.sanitize(request.body()));
        postRepository.save(post);

        thread.setLastPostAt(Instant.now());
        threadRepository.save(thread);

        if (!thread.getAuthor().getId().equals(author.getId())) {
            notificationService.createNotification(
                    thread.getAuthor().getId(),
                    author.getFullName() + " dodał/a odpowiedź w twoim wątku na forum",
                    "/forum/thread/" + thread.getId(),
                    NotificationType.NEW_REPLY
            );
        }
    }

    @Transactional
    public void updatePost(Long postId, ForumPostRequest request, String requesterEmail) {
        ForumPost post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wpisu o ID: " + postId));
        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        if (!PermissionRules.canEditForumContent(post.getAuthor(), requester)) {
            throw new ForbiddenOperationException("Brak uprawnień. Nikt nie może edytować cudzych postów.");
        }

        post.setBody(RichTextSanitizer.sanitize(request.body()));
        post.setEditedAt(Instant.now());
        postRepository.save(post);
    }

    @Transactional
    public void deletePost(Long postId, String requesterEmail) {
        ForumPost post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wpisu o ID: " + postId));
        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        if (!PermissionRules.canDeleteForumContent(post.getAuthor(), requester)) {
            throw new ForbiddenOperationException("Brak uprawnień do usunięcia tego wpisu.");
        }

        Long threadId = post.getThread().getId();
        ForumThread thread = threadRepository.findByIdForUpdate(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));

        if (!postRepository.existsById(postId)) {
            throw new EntityNotFoundException("Nie znaleziono wpisu o ID: " + postId);
        }

        ForumPost firstPost = postRepository.findFirstByThreadIdOrderByCreatedAtAscIdAsc(threadId).orElse(post);

        if (firstPost.getId().equals(post.getId())) {
            throw new ResourceConflictException("Nie można usunąć pierwszego wpisu wątku. Usuń cały wątek zamiast jego pierwszego wpisu.");
        }

        ForumPost latestRemainingPost = postRepository.findFirstByThreadIdAndIdNotOrderByCreatedAtDescIdDesc(threadId, postId).orElseThrow(() ->
                new ResourceConflictException("Nie można usunąć wpisu, ponieważ wątek nie zawiera innego wpisu."));

        postRepository.delete(post);
        thread.setLastPostAt(latestRemainingPost.getCreatedAt());
        threadRepository.save(thread);
    }

    private ForumPostDto mapToDto(ForumPost post, Breeder requester) {
        return new ForumPostDto(
                post.getId(),
                post.getAuthor().getFullName(),
                post.getAuthor().getRole(),
                post.getBody(),
                post.getCreatedAt(),
                post.getEditedAt(),
                PermissionRules.canEditForumContent(post.getAuthor(), requester),
                PermissionRules.canDeleteForumContent(post.getAuthor(), requester)
        );
    }
}