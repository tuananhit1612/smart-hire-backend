package com.smarthire.backend.features.cv.entity;

import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.entity.CvFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "cv_builder_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvBuilderData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_file_id", nullable = false, unique = true)
    private CvFile cvFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(name = "template_id", length = 50)
    private String templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_data", nullable = false, columnDefinition = "json")
    private Map<String, Object> sectionsData;

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
