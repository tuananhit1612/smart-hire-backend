package com.smarthire.backend.features.application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "application_ai_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAiResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @Column(name = "skill_match", nullable = false)
    @Builder.Default
    private Integer skillMatch = 0;

    @Column(name = "experience_match", nullable = false)
    @Builder.Default
    private Integer experienceMatch = 0;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_ai_strengths", joinColumns = @JoinColumn(name = "ai_result_id"))
    @Column(name = "strength")
    private List<String> strengths;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_ai_gaps", joinColumns = @JoinColumn(name = "ai_result_id"))
    @Column(name = "gap")
    private List<String> gaps;

    @Column(name = "analyzed_at", nullable = false)
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();
}
