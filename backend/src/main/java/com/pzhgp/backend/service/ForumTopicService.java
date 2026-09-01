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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForumTopicService {

    private final ForumTopicRepository topicRepository;
    private final ForumCategoryRepository categoryRepository;
    private final BreederRepository breederRepository;
    private final ForumPostRepository postRepository;

    @Transactional(readOnly = true)
    public Page<ForumTopicDto> getTopicsByCategory(Long categoryId, int page, int size, String requesterEmail) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Nie znaleziono kategorii o ID: " + categoryId);
        }

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastPostAt"));

        return topicRepository.findByCategoryId(categoryId, pageable)
                .map(topic -> mapToDto(topic, requester));
    }

    @Transactional
    public void createTopic(ForumTopicRequest request, String authorEmail) {
        ForumCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii."));

        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        ForumTopic topic = new ForumTopic();
        topic.setCategory(category);
        topic.setAuthor(author);
        topic.setTitle(request.title());
        topic = topicRepository.save(topic);

        ForumPost firstPost = new ForumPost();
        firstPost.setTopic(topic);
        firstPost.setAuthor(author);
        firstPost.setBody(request.initialPostContent());
        postRepository.save(firstPost);
    }

    @Transactional
    public ForumTopicDto getTopicAndIncrementViews(Long topicId, String requesterEmail) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tematu."));

        topicRepository.incrementViews(topicId);
        topic.setViews(topic.getViews() + 1);

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        return mapToDto(topic, requester);
    }

    @Transactional
    public void toggleTopicStatus(Long topicId, String requesterEmail, TopicAction action) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tematu."));
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        if (requester.getRole() != Role.ADMINISTRATOR && requester.getRole() != Role.MODERATOR) {
            throw new IllegalStateException("Brak uprawnień do moderacji.");
        }

        switch (action) {
            case LOCK -> topic.setIsLocked(!topic.getIsLocked());
            case PIN -> topic.setIsPinned(!topic.getIsPinned());
            default -> throw new IllegalArgumentException("Nieobsługiwana akcja moderacyjna.");
        }
    }

    @Transactional
    public void deleteTopic(Long topicId, String requesterEmail) {
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tematu o ID: " + topicId));

        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        boolean isAuthor = topic.getAuthor().getId().equals(requester.getId());
        boolean hasPrivileges = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;

        if (!isAuthor && !hasPrivileges) {
            throw new IllegalStateException("Brak uprawnień do usunięcia tego tematu.");
        }

        postRepository.deleteAllByTopicId(topicId);
        topicRepository.delete(topic);
    }

    private ForumTopicDto mapToDto(ForumTopic topic, Breeder requester) {
        String authorFullName = topic.getAuthor().getName() + " " + topic.getAuthor().getSurname();

        boolean isAuthor = topic.getAuthor().getId().equals(requester.getId());
        boolean hasPrivileges = requester.getRole() == Role.ADMINISTRATOR || requester.getRole() == Role.MODERATOR;

        return new ForumTopicDto(
                topic.getId(),
                topic.getCategory().getId(),
                topic.getTitle(),
                authorFullName,
                topic.getIsLocked(),
                topic.getIsPinned(),
                topic.getLastPostAt(),
                topic.getViews(),
                topic.getCreatedAt(),
                isAuthor || hasPrivileges,
                hasPrivileges
        );
    }
}