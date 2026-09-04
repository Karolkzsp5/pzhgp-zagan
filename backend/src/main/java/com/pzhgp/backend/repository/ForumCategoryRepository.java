package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumCategoryRepository extends JpaRepository<ForumCategory, Long> {
    List<ForumCategory> findAllByOrderBySortOrderAscNameAsc();
}