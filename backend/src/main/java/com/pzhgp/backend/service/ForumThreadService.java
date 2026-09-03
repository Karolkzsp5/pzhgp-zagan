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

@Service
@RequiredArgsConstructor
public class ForumThreadService {

    private final ForumThreadRepository threadRepository;
    private final ForumCategoryRepository categoryRepository;
    private final BreederRepository breederRepository;
    private final ForumPostRepository postRepository;

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
    }

    @Transactional
    public ForumThreadDto getThreadAndIncrementViews(Long threadId, String requesterEmail) {
        threadRepository.incrementViews(threadId);

        ForumThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        return mapToDto(thread, requester);
    }

    @Transactional
    public void toggleThreadStatus(Long threadId, String requesterEmail, ThreadAction action) {
        ForumThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono wątku."));
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (requester.getRole() != Role.ADMINISTRATOR && requester.getRole() != Role.MODERATOR) {
            throw new IllegalStateException("Brak uprawnień do moderacji.");
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

        boolean isAuthor = thread.getAuthor().getId().equals(requester.getId());
        boolean hasPrivileges = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;

        if (!isAuthor && !hasPrivileges) {
            throw new IllegalStateException("Brak uprawnień do usunięcia tego wątku.");
        }

        postRepository.deleteAllByThreadId(threadId);
        threadRepository.delete(thread);
    }

    private ForumThreadDto mapToDto(ForumThread thread, Breeder requester) {
        String authorFullName = thread.getAuthor().getName() + " " + thread.getAuthor().getSurname();
        boolean isAuthor = thread.getAuthor().getId().equals(requester.getId());
        boolean hasPrivileges = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;

        long totalPosts = postRepository.countByThreadId(thread.getId());
        int repliesCount = (int) Math.max(0, totalPosts - 1);

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
                isAuthor || hasPrivileges,
                hasPrivileges
        );
    }
}