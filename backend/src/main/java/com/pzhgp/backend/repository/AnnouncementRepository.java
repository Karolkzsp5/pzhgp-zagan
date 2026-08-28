package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<Announcement> findAllByOrderByIsPinnedDescCreatedAtDesc(Pageable pageable);
}