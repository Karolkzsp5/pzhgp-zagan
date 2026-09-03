package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    @Modifying
    @Query("DELETE FROM ForumPost p WHERE p.thread.id = :threadId")
    void deleteAllByThreadId(@Param("threadId") Long threadId);

    @EntityGraph(attributePaths = {"author"})
    Page<ForumPost> findByThreadId(Long threadId, Pageable pageable);

    // NOWOŚĆ: Liczenie postów do statystyk
    long countByThreadId(Long threadId);
}