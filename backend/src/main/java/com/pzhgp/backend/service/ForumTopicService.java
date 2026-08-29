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

        // Pobieramy użytkownika, który wysyła zapytanie
        Breeder requester = breederRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        // Sortujemy od najnowszych postów
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastPostAt"));

        // Przekazujemy obiekt requestera do metody mapującej
        return topicRepository.findByCategoryId(categoryId, pageable)
                .map(topic -> mapToDto(topic, requester));
    }

    @Transactional
    public void createTopic(ForumTopicRequest request, String authorEmail) {
        ForumCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono kategorii."));

        Breeder author = breederRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono użytkownika."));

        // 1. Zapisujemy nowy temat
        ForumTopic topic = new ForumTopic();
        topic.setCategory(category);
        topic.setAuthor(author);
        topic.setTitle(request.title());
        topic = topicRepository.save(topic);

        // 2. Zapisujemy od razu pierwszy wpis w temacie
        ForumPost firstPost = new ForumPost();
        firstPost.setTopic(topic);
        firstPost.setAuthor(author);
        firstPost.setBody(request.initialPostContent());
        postRepository.save(firstPost);
    }

    @Transactional
    public ForumTopicDto getTopicAndIncrementViews(Long topicId, String requesterEmail) {
        // 1. Atomowe podbicie licznika w bazie (bez wczytywania encji do pamięci)
        topicRepository.incrementViews(topicId);

        // 2. Pobranie zaktualizowanego tematu
        ForumTopic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tematu."));

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
            default -> throw new IllegalArgumentException("Nieobsługiwana akcja moderacyjna."); // Choć przy Enumie to teoretycznie niemożliwe
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

        // 1. Optymalne usunięcie wszystkich komentarzy powiązanych z tematem jednym zapytaniem SQL
        postRepository.deleteAllByTopicId(topicId);

        // 2. Usunięcie samego tematu
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
                isAuthor || hasPrivileges, // canDelete
                hasPrivileges              // canModerate
        );
    }
}