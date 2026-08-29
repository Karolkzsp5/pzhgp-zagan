package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.ForumTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumTopicRepository extends JpaRepository<ForumTopic, Long> {

    @EntityGraph(attributePaths = {"author"})
    Page<ForumTopic> findByCategoryId(Long categoryId, Pageable pageable);
    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE ForumTopic t SET t.views = t.views + 1 WHERE t.id = :topicId")
    void incrementViews(@Param("topicId") Long topicId);
}