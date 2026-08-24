package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.Announcement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Announcement> findAllByOrderByIsPinnedDescCreatedAtDesc();
}