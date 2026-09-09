package com.pzhgp.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "board_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private BoardRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managed_section_id")
    private Section managedSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breeder_id")
    private Breeder breeder;

    @Column(name = "custom_name", length = 32)
    private String customName;

    @Column(name = "custom_surname", length = 64)
    private String customSurname;

    @Column(name = "contact_phone", length = 9)
    private String contactPhone;
}