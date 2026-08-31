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
    @Query("DELETE FROM ForumPost p WHERE p.topic.id = :topicId")
    void deleteAllByTopicId(@Param("topicId") Long topicId);

    @EntityGraph(attributePaths = {"author"})
    Page<ForumPost> findByTopicId(Long topicId, Pageable pageable);
}