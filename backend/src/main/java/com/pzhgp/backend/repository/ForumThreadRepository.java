package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.ForumThread;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumThreadRepository extends JpaRepository<ForumThread, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<ForumThread> findByCategoryId(Long categoryId, Pageable pageable);
    boolean existsByCategoryId(Long categoryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ForumThread t SET t.views = COALESCE(t.views, 0) + 1 WHERE t.id = :threadId")
    int incrementViews(@Param("threadId") Long threadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ForumThread t WHERE t.id = :threadId")
    Optional<ForumThread> findByIdForUpdate(@Param("threadId") Long threadId);
}