package com.smarthire.backend.features.onboarding.repository;

import com.smarthire.backend.features.onboarding.entity.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, Long> {
    
    List<OnboardingDocument> findByApplication_Id(Long applicationId);
    
    // Optional: Find specific document type for an application
    List<OnboardingDocument> findByApplication_IdAndDocumentType(Long applicationId, com.smarthire.backend.features.onboarding.enums.DocumentType documentType);
}
