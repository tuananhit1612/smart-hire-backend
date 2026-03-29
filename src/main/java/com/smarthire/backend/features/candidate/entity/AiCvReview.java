package com.smarthire.backend.features.candidate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_cv_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCvReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_file_id", nullable = false)
    private CvFile cvFile;

    @Column(columnDefinition = "JSON")
    private String issues;

    @Column(columnDefinition = "JSON")
    private String suggestions;

    @Column(columnDefinition = "JSON")
    private String strengths;

    @Column(columnDefinition = "JSON")
    private String weaknesses;
    
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "overall_rating", precision = 5, scale = 2)
    private BigDecimal overallRating;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
