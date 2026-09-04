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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumThreadService {

    private final ForumThreadRepository threadRepository;
    private final ForumCategoryRepository categoryRepository;
    private final BreederRepository breederRepository;
    private final ForumPostRepository postRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<ForumThreadDto> getThreadsByCategory(Long categoryId, int page, int size, String requesterEmail) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Nie znaleziono kategorii o ID: " + categoryId);
        }

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastPostAt"));

        return threadRepository.findByCategoryId(categoryId, pageable)
                .map(thread -> mapToDto(thread, requester));
    }

    @Transactional
    public void createThread(ForumThreadRequest request, String authorEmail) {
        ForumCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii."));

        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        ForumThread thread = new ForumThread();
        thread.setCategory(category);
        thread.setAuthor(author);
        thread.setTitle(request.title());
        thread = threadRepository.save(thread);

        ForumPost firstPost = new ForumPost();
        firstPost.setThread(thread);
        firstPost.setAuthor(author);
        firstPost.setBody(request.initialPostContent());
        postRepository.save(firstPost);

        String authorFullName = author.getName() + " " + author.getSurname();
        String notificationMessage = authorFullName + " dodał/a nowy wątek na forum";
        String notificationLink = "/forum/thread/" + thread.getId();

        List<Breeder> recipients = breederRepository.findByStatus(AccountStatus.ACTIVE).stream()
                .filter(breeder -> !breeder.getId().equals(author.getId()))
                .collect(Collectors.toList());

        if (!recipients.isEmpty()) {
            notificationService.createBulkNotifications(
                    recipients,
                    notificationMessage,
                    notificationLink,
                    NotificationType.NEW_THREAD
            );
        }
    }

    @Transactional
    public ForumThreadDto getThreadAndIncrementViews(Long threadId, String requesterEmail) {
        ForumThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (!thread.getAuthor().getId().equals(requester.getId())) {
            thread.setViews(thread.getViews() + 1);
        }

        return mapToDto(thread, requester);
    }

    @Transactional
    public void toggleThreadStatus(Long threadId, String requesterEmail, ThreadAction action) {
        ForumThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (!canDeleteContent(thread.getAuthor(), requester)) {
            throw new IllegalStateException("Brak uprawnień do moderacji tego wątku.");
        }

        switch (action) {
            case LOCK -> thread.setIsLocked(!thread.getIsLocked());
            case PIN -> thread.setIsPinned(!thread.getIsPinned());
            default -> throw new IllegalArgumentException("Nieobsługiwana akcja moderacyjna.");
        }
    }

    @Transactional
    public void deleteThread(Long threadId, String requesterEmail) {
        ForumThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku o ID: " + threadId));

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (!canDeleteContent(thread.getAuthor(), requester)) {
            throw new IllegalStateException("Brak uprawnień do usunięcia tego wątku.");
        }

        postRepository.deleteAllByThreadId(threadId);
        threadRepository.delete(thread);
    }

    // Permission logic

    private boolean canEditContent(Breeder author, Breeder requester) {
        return author.getId().equals(requester.getId());
    }

    private boolean canDeleteContent(Breeder author, Breeder requester) {
        if (author.getId().equals(requester.getId())) {
            return true;
        }
        if (requester.getRole() == Role.ADMINISTRATOR) {
            return author.getRole() != Role.ADMINISTRATOR;
        }
        if (requester.getRole() == Role.MODERATOR) {
            return author.getRole() == Role.BREEDER;
        }
        return false;
    }

    private ForumThreadDto mapToDto(ForumThread thread, Breeder requester) {
        String authorFullName = thread.getAuthor().getName() + " " + thread.getAuthor().getSurname();

        int repliesCount = thread.getRepliesCount() != null ? Math.max(0, thread.getRepliesCount()) : 0;

        return new ForumThreadDto(
                thread.getId(),
                thread.getCategory().getId(),
                thread.getTitle(),
                authorFullName,
                repliesCount,
                thread.getIsLocked(),
                thread.getIsPinned(),
                thread.getLastPostAt(),
                thread.getViews(),
                thread.getCreatedAt(),
                canEditContent(thread.getAuthor(), requester),
                canDeleteContent(thread.getAuthor(), requester),
                canDeleteContent(thread.getAuthor(), requester)
        );
    }
}