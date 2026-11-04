package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    boolean existsByName(String name);
    List<Section> findAllByOrderBySortOrderAsc();
}
