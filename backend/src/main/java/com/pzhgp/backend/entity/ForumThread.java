package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForumThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ForumCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeder_id", nullable = false)
    private Breeder author;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @Column(name = "last_post_at")
    private LocalDateTime lastPostAt;

    @Column(columnDefinition = "integer default 0")
    private Integer views = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Formula("(SELECT COUNT(p.id) FROM forum_posts p WHERE p.thread_id = id) - 1")
    private Integer repliesCount;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastPostAt = this.createdAt;
    }
}