package com.smarthire.backend.features.company.entity;

import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.company.entity.embeddable.CompanyBenefit;
import com.smarthire.backend.features.company.entity.embeddable.CompanySocialLink;
import com.smarthire.backend.shared.enums.CompanySize;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(length = 500)
    private String website;

    @Column(length = 150)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_size")
    private CompanySize companySize;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 255)
    private String tagline;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 10)
    private String founded;

    @ElementCollection
    @CollectionTable(name = "company_tech_stack", joinColumns = @JoinColumn(name = "company_id"))
    @Column(name = "technology")
    private List<String> techStack = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "company_benefits", joinColumns = @JoinColumn(name = "company_id"))
    private List<CompanyBenefit> benefits = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "company_social_links", joinColumns = @JoinColumn(name = "company_id"))
    private List<CompanySocialLink> socialLinks = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
