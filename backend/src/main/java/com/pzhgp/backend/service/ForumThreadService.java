package com.pzhgp.backend.service;

import com.pzhgp.backend.dto.ForumThreadDto;
import com.pzhgp.backend.dto.ForumThreadRequest;
import com.pzhgp.backend.dto.ThreadStatusRequest;
import com.pzhgp.backend.entity.*;
import com.pzhgp.backend.exception.ForbiddenOperationException;
import com.pzhgp.backend.repository.BreederRepository;
import com.pzhgp.backend.repository.ForumCategoryRepository;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumThreadService {

    /** Największa dopuszczalna liczba wątków na stronie kategorii. */
    private static final int MAX_PAGE_SIZE = 100;

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

        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        PaginationUtils.validate(page, size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("isPinned"),
                Sort.Order.desc("lastPostAt"),
                Sort.Order.desc("id")
        ));

        return threadRepository.findByCategoryId(categoryId, pageable).map(thread -> mapToDto(thread, requester));
    }

    @Transactional
    public void createThread(ForumThreadRequest request, String authorEmail) {
        ForumCategory category = categoryRepository.findById(request.categoryId()).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii."));
        Breeder author = breederRepository.requireByEmail(authorEmail);

        ForumThread thread = new ForumThread();
        thread.setCategory(category);
        thread.setAuthor(author);
        thread.setTitle(request.title());
        thread = threadRepository.save(thread);

        ForumPost firstPost = new ForumPost();
        firstPost.setThread(thread);
        firstPost.setAuthor(author);
        firstPost.setBody(RichTextSanitizer.sanitize(request.initialPostContent()));
        postRepository.save(firstPost);

        String notificationMessage = "Użytkownik " + author.getFullName() + " dodał nowy wątek na forum.";
        String notificationLink = "/forum/thread/" + thread.getId();

        List<Breeder> recipients = breederRepository.findByStatus(AccountStatus.ACTIVE).stream().filter(breeder -> !breeder.getId().equals(author.getId())).collect(Collectors.toList());

        if (!recipients.isEmpty()) {
            notificationService.createBulkNotifications(recipients, notificationMessage, notificationLink, NotificationType.NEW_THREAD);
        }
    }

    @Transactional
    public ForumThreadDto getThread(Long threadId, String requesterEmail, boolean countView) {
        ForumThread thread = threadRepository.findById(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));
        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        if (countView && !thread.getAuthor().getId().equals(requester.getId())) {
            threadRepository.incrementViews(threadId);
            thread = threadRepository.findById(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));
        }

        return mapToDto(thread, requester);
    }

    @Transactional
    public void updateThreadStatus(Long threadId, ThreadStatusRequest request, String requesterEmail) {
        ForumThread thread = threadRepository.findByIdForUpdate(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));
        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        if (!PermissionRules.canModerateForumContent(thread.getAuthor(), requester)) {
            throw new ForbiddenOperationException("Brak uprawnień do moderacji tego wątku.");
        }

        if (request.isLocked() != null) {
            thread.setLocked(request.isLocked());
        }

        if (request.isPinned() != null) {
            thread.setPinned(request.isPinned());
        }
    }

    @Transactional
    public void updateThreadTitle(Long threadId, String title, String requesterEmail) {
        ForumThread thread = threadRepository.findByIdForUpdate(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));
        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        if (!PermissionRules.canEditForumContent(thread.getAuthor(), requester)) {
            throw new ForbiddenOperationException("Brak uprawnień. Tylko autor może edytować tytuł wątku.");
        }

        // Długość tytułu sprawdza ThreadTitleRequest.
        thread.setTitle(title);
    }

    @Transactional
    public void deleteThread(Long threadId, String requesterEmail) {
        ForumThread thread = threadRepository.findByIdForUpdate(threadId).orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku o ID: " + threadId));
        Breeder requester = breederRepository.requireByEmail(requesterEmail);

        if (!PermissionRules.canDeleteForumContent(thread.getAuthor(), requester)) {
            throw new ForbiddenOperationException("Brak uprawnień do usunięcia tego wątku.");
        }

        postRepository.deleteAllByThreadId(threadId);
        threadRepository.delete(thread);
    }

    private ForumThreadDto mapToDto(ForumThread thread, Breeder requester) {
        int repliesCount = thread.getRepliesCount() != null ? Math.max(0, thread.getRepliesCount()) : 0;

        return new ForumThreadDto(
                thread.getId(),
                thread.getCategory().getId(),
                thread.getTitle(),
                thread.getAuthor().getFullName(),
                repliesCount,
                thread.isLocked(),
                thread.isPinned(),
                thread.getLastPostAt(),
                thread.getViews(),
                thread.getCreatedAt(),
                PermissionRules.canEditForumContent(thread.getAuthor(), requester),
                PermissionRules.canDeleteForumContent(thread.getAuthor(), requester),
                PermissionRules.canModerateForumContent(thread.getAuthor(), requester)
        );
    }
}