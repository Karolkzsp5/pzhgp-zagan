package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** Ogłoszenia opublikowane przez użytkownika, od najstarszego — do eksportu jego danych. */
    List<Announcement> findByAuthorIdOrderByCreatedAtAscIdAsc(Long authorId);

    @EntityGraph(attributePaths = {"author"})
    Page<Announcement> findAllByOrderByIsPinnedDescCreatedAtDescIdDesc(Pageable pageable);
}