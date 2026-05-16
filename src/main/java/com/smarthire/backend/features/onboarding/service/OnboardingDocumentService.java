package com.smarthire.backend.features.onboarding.service;

import com.smarthire.backend.features.onboarding.dto.OnboardingDocumentResponse;
import com.smarthire.backend.features.onboarding.enums.DocumentType;
import com.smarthire.backend.features.onboarding.enums.VerificationStatus;
import com.smarthire.backend.shared.enums.Role;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.util.List;

public interface OnboardingDocumentService {

    OnboardingDocumentResponse uploadDocument(Long applicationId, Long userId, Role role, MultipartFile file, DocumentType type);

    List<OnboardingDocumentResponse> getDocumentsByApplication(Long applicationId, Long userId, Role role);

    Resource downloadDocument(Long documentId, Long userId, Role role);

    OnboardingDocumentResponse updateDocumentStatus(Long documentId, Long userId, Role role, VerificationStatus status, String comment);
}
