package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.ForumThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<ForumThread> findByCategoryId(Long categoryId, Pageable pageable);
    boolean existsByCategoryId(Long categoryId);
}