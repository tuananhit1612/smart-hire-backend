package com.smarthire.backend.features.onboarding.service;

import com.smarthire.backend.features.onboarding.dto.OnboardingDocumentResponse;
import com.smarthire.backend.features.onboarding.enums.DocumentType;
import com.smarthire.backend.features.onboarding.enums.VerificationStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;

public interface OnboardingDocumentService {

    OnboardingDocumentResponse uploadDocument(Long applicationId, Long userId, MultipartFile file, DocumentType type);

    List<OnboardingDocumentResponse> getDocumentsByApplication(Long applicationId, Long userId);

    Resource downloadDocument(Long documentId, Long userId);

    OnboardingDocumentResponse updateDocumentStatus(Long documentId, Long hrUserId, VerificationStatus status, String comment);
}
