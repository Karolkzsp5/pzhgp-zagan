package com.pzhgp.backend.repository;

import com.pzhgp.backend.entity.BoardMember;
import com.pzhgp.backend.entity.BoardRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    @Query("SELECT b FROM BoardMember b " +
            "LEFT JOIN FETCH b.managedSection " +
            "LEFT JOIN FETCH b.breeder " +
            "ORDER BY b.managedSection.id NULLS FIRST, b.sortOrder ASC")
    List<BoardMember> findAllWithDetails();

    @Query("SELECT COUNT(b) > 0 FROM BoardMember b WHERE b.role = :role AND b.managedSection.id = :sectionId AND b.id != :excludeId")
    boolean existsConflictForSection(@Param("role") BoardRole role, @Param("sectionId") Long sectionId, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(b) > 0 FROM BoardMember b WHERE b.role = :role AND b.managedSection IS NULL AND b.id != :excludeId")
    boolean existsConflictForOddzial(@Param("role") BoardRole role, @Param("excludeId") Long excludeId);
}