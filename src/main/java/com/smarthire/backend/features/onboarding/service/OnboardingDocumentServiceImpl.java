package com.smarthire.backend.features.onboarding.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ForbiddenException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.onboarding.dto.OnboardingAiVerificationResult;
import com.smarthire.backend.features.onboarding.dto.OnboardingDocumentResponse;
import com.smarthire.backend.features.onboarding.entity.OnboardingDocument;
import com.smarthire.backend.features.onboarding.enums.DocumentType;
import com.smarthire.backend.features.onboarding.enums.VerificationStatus;
import com.smarthire.backend.features.onboarding.repository.OnboardingDocumentRepository;
import com.smarthire.backend.infrastructure.ai.service.AiService;
import com.smarthire.backend.infrastructure.storage.FileStorageService;
import com.smarthire.backend.shared.enums.ApplicationStage;
import com.smarthire.backend.shared.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingDocumentServiceImpl implements OnboardingDocumentService {

    private final OnboardingDocumentRepository onboardingDocumentRepository;
    private final ApplicationRepository applicationRepository;
    private final FileStorageService fileStorageService;
    private final AiService aiService;

    @Override
    @Transactional
    public OnboardingDocumentResponse uploadDocument(Long applicationId, Long userId, Role role, MultipartFile file, DocumentType type) {
        Application application = getApplicationAndVerifyUploadAccess(applicationId, userId, role);

        if (application.getStage() != ApplicationStage.OFFER && application.getStage() != ApplicationStage.HIRED) {
            throw new BadRequestException("Chá»‰ cÃ³ thá»ƒ táº£i lÃªn há»“ sÆ¡ nháº­n viá»‡c khi tráº¡ng thÃ¡i á»©ng tuyá»ƒn lÃ  OFFER hoáº·c HIRED");
        }

        // 1. LÆ°u file xuá»‘ng á»• cá»©ng (private folder)
        String relativePath = fileStorageService.storeOnboardingFile(file, "onboarding_docs");
        Path absolutePath = fileStorageService.getFilePath(relativePath);

        VerificationStatus status = VerificationStatus.PENDING;
        String aiFeedback = null;

        // 2. Cháº¡y AI Verify náº¿u lÃ  giáº¥y tá» tÃ¹y thÃ¢n
        if (type == DocumentType.ID_FRONT || type == DocumentType.ID_BACK) {
            String mimeType = file.getContentType();
            OnboardingAiVerificationResult aiResult = aiService.verifyIdCardImage(absolutePath, mimeType);

            if (!aiResult.isValid()) {
                status = VerificationStatus.REJECTED;
                aiFeedback = aiResult.getFeedbackReason();
                log.warn("AI Rejected ID Document for application {}: {}", applicationId, aiFeedback);
            } else {
                CandidateProfile candidate = application.getCandidateProfile();
                String extractedName = aiResult.getExtractedName();
                String profileName = candidate.getUser() != null ? candidate.getUser().getFullName() : "á»¨ng viÃªn";
                
                // OCR Name cross-check (CÆ¡ báº£n)
                if (extractedName != null && !extractedName.isBlank() && profileName != null && !profileName.isBlank()) {
                    boolean isMatch = checkNameMatch(profileName, extractedName);
                    if (!isMatch) {
                        status = VerificationStatus.REJECTED;
                        aiFeedback = String.format("TÃªn trÃªn CCCD (%s) khÃ´ng khá»›p vá»›i tÃªn há»“ sÆ¡ (%s). Vui lÃ²ng kiá»ƒm tra láº¡i.", extractedName, profileName);
                        log.warn("Name mismatch! Profile: {}, OCR: {}", profileName, extractedName);
                    } else {
                        status = VerificationStatus.VERIFIED;
                        aiFeedback = "Há»“ sÆ¡ CCCD há»£p lá»‡ vÃ  tÃªn á»©ng viÃªn trÃ¹ng khá»›p.";
                    }
                } else {
                    status = VerificationStatus.VERIFIED;
                    aiFeedback = "KhÃ´ng xÃ¡c nháº­n Ä‘Æ°á»£c tÃªn chÃ­nh sÃ¡c qua OCR, nhÆ°ng hÃ¬nh áº£nh CCCD há»£p lá»‡.";
                }
            }
        }

        // 3. Cáº­p nháº­t hoáº·c lÆ°u má»›i vÃ o DB
        List<OnboardingDocument> existingDocs = onboardingDocumentRepository.findByApplication_Id(applicationId)
                .stream()
                .filter(d -> d.getDocumentType() == type)
                .collect(Collectors.toList());

        OnboardingDocument document;
        if (!existingDocs.isEmpty()) {
            // Cáº­p nháº­t báº£n ghi Ä‘Ã£ cÃ³ Ä‘á»ƒ trÃ¡nh sinh ra trÃ¹ng láº·p (duplication)
            document = existingDocs.get(0);
            fileStorageService.deleteFile(document.getFilePath());
            document.setFilePath(relativePath);
            document.setStatus(status);
            document.setAiFeedback(aiFeedback);
            
            // XÃ³a cÃ¡c báº£n ghi thá»«a náº¿u bá»‹ trÃ¹ng láº·p do lá»—i trÆ°á»›c Ä‘Ã¢y
            if (existingDocs.size() > 1) {
                for (int i = 1; i < existingDocs.size(); i++) {
                    onboardingDocumentRepository.delete(existingDocs.get(i));
                }
            }
        } else {
            // Táº¡o má»›i náº¿u chÆ°a tá»«ng táº£i lÃªn
            document = OnboardingDocument.builder()
                    .application(application)
                    .documentType(type)
                    .filePath(relativePath)
                    .status(status)
                    .aiFeedback(aiFeedback)
                    .build();
        }

        document = onboardingDocumentRepository.save(document);

        return mapToResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingDocumentResponse> getDocumentsByApplication(Long applicationId, Long userId, Role role) {
        log.info("Fetching onboarding documents for application {} by user {}", applicationId, userId);
        getApplicationAndVerifyReadAccess(applicationId, userId, role);
        
        return onboardingDocumentRepository.findByApplication_Id(applicationId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId, Long userId, Role role) {
        OnboardingDocument doc = onboardingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingDocument", documentId));
                
        // XÃ¡c thá»±c quyá»n download
        getApplicationAndVerifyReadAccess(doc.getApplication().getId(), userId, role);
        
        Path filePath = fileStorageService.getFilePath(doc.getFilePath());
        try {
            Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                log.error("File not found on disk: {}", filePath);
                throw new ResourceNotFoundException("File not found: " + doc.getFilePath());
            }
        } catch (java.net.MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + doc.getFilePath());
        }
    }

    @Override
    @Transactional
    public OnboardingDocumentResponse updateDocumentStatus(Long documentId, Long userId, Role role, VerificationStatus status, String comment) {
        OnboardingDocument doc = onboardingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingDocument", documentId));

        Application application = doc.getApplication();
        
        // Chá»‰ duyá»‡t Ä‘Æ°á»£c náº¿u HRUserId lÃ  ngÆ°á»i táº¡o Company (owner)
        boolean isHrOwner = isHrOwner(application, userId);
                          
        if (role != Role.ADMIN && !isHrOwner) {
            throw new ForbiddenException("Chá»‰ chá»§ doanh nghiá»‡p phÃ²ng ban nÃ y má»›i Ä‘Æ°á»£c duyá»‡t há»“ sÆ¡");
        }
        
        doc.setStatus(status);
        if (comment != null && !comment.trim().isEmpty()) {
            doc.setAiFeedback(comment); // DÃ¹ng láº¡i trÆ°á»ng pháº£n há»“i Ä‘á»ƒ lÆ°u feedback nhÃ¢n sá»±
        }
        
        return mapToResponse(onboardingDocumentRepository.save(doc));
    }

    private Application getApplicationAndVerifyUploadAccess(Long applicationId, Long userId, Role role) {
        Application application = getApplication(applicationId, userId);
        if (role != Role.CANDIDATE || !isCandidateOwner(application, userId)) {
            log.warn("Upload denied for user {} with role {} to application {}", userId, role, applicationId);
            throw new ForbiddenException("Only the candidate who owns this application can upload onboarding documents");
        }
        return application;
    }

    private Application getApplicationAndVerifyReadAccess(Long applicationId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return getApplication(applicationId, userId);
        }
        return getApplicationAndVerifyAccess(applicationId, userId);
    }

    private Application getApplication(Long applicationId, Long userId) {
        if (applicationId == null || userId == null) {
            log.error("Null applicationId ({}) or userId ({}) in access verification", applicationId, userId);
            throw new BadRequestException("Application ID or user ID is invalid");
        }

        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
    }

    private boolean isCandidateOwner(Application application, Long userId) {
        CandidateProfile cp = application.getCandidateProfile();
        return cp != null && cp.getUser() != null && cp.getUser().getId().equals(userId);
    }

    private boolean isHrOwner(Application application, Long userId) {
        return application.getJob() != null
                && application.getJob().getCompany() != null
                && application.getJob().getCompany().getCreatedBy() != null
                && application.getJob().getCompany().getCreatedBy().getId().equals(userId);
    }

    private Application getApplicationAndVerifyAccess(Long applicationId, Long userId) {
        if (applicationId == null || userId == null) {
            log.error("Null applicationId ({}) or userId ({}) in access verification", applicationId, userId);
            throw new BadRequestException("ID á»©ng tuyá»ƒn hoáº·c ID ngÆ°á»i dÃ¹ng khÃ´ng há»£p lá»‡");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
                
        // Kiá»ƒm tra quyá»n. Chá»‰ á»©ng viÃªn cá»§a Ä‘Æ¡n Ä‘Ã³, hoáº·c HR sá»Ÿ há»¯u job Ä‘Ã³ má»›i Ä‘Æ°á»£c xem/upload.
        CandidateProfile cp = application.getCandidateProfile();
        boolean isCandidate = cp != null && cp.getUser() != null && cp.getUser().getId().equals(userId);
        
        boolean isHrOwner = false;
        if (application.getJob() != null && application.getJob().getCompany() != null && application.getJob().getCompany().getCreatedBy() != null) {
            isHrOwner = application.getJob().getCompany().getCreatedBy().getId().equals(userId);
        }
                          
        if (!isCandidate && !isHrOwner) {
            log.warn("Access denied for user {} to application {}. Roles: candidate={}, hr={}", userId, applicationId, isCandidate, isHrOwner);
            throw new ForbiddenException("Báº¡n khÃ´ng cÃ³ quyá»n truy cáº­p há»“ sÆ¡ cá»§a Ä‘Æ¡n á»©ng tuyá»ƒn nÃ y");
        }
        
        return application;
    }
    
    // Fuzzy match cÆ¡ báº£n kiá»ƒm tra sá»± trÃ¹ng khá»›p cá»§a tÃªn (cá»±c ká»³ há»¯u Ã­ch Ä‘á»ƒ chá»‘ng giáº£ máº¡o profile)
    private boolean checkNameMatch(String dbName, String ocrName) {
        String n1 = dbName.toLowerCase().trim().replaceAll("[^a-zÃ Ã¡áº¡áº£Ã£Ã¢áº§áº¥áº­áº©áº«Äƒáº±áº¯áº·áº³áºµÃ¨Ã©áº¹áº»áº½Ãªá»áº¿á»‡á»ƒá»…Ã¬Ã­á»‹á»‰Ä©Ã²Ã³á»á»ÃµÃ´á»“á»‘á»™á»•á»—Æ¡á»á»›á»£á»Ÿá»¡Ã¹Ãºá»¥á»§Å©Æ°á»«á»©á»±á»­á»¯á»³Ã½á»µá»·á»¹Ä‘\\s]", "");
        String n2 = ocrName.toLowerCase().trim().replaceAll("[^a-zÃ Ã¡áº¡áº£Ã£Ã¢áº§áº¥áº­áº©áº«Äƒáº±áº¯áº·áº³áºµÃ¨Ã©áº¹áº»áº½Ãªá»áº¿á»‡á»ƒá»…Ã¬Ã­á»‹á»‰Ä©Ã²Ã³á»á»ÃµÃ´á»“á»‘á»™á»•á»—Æ¡á»á»›á»£á»Ÿá»¡Ã¹Ãºá»¥á»§Å©Æ°á»«á»©á»±á»­á»¯á»³Ã½á»µá»·á»¹Ä‘\\s]", "");
        
        // Cáº¯t khoáº£ng tráº¯ng thá»«a
        n1 = n1.replaceAll("\\s+", " ");
        n2 = n2.replaceAll("\\s+", " ");
        
        // Log logic match
        log.info("Checking name match: DB='{}' vs OCR='{}'", n1, n2);
        
        // Sáº½ tráº£ vá» true náº¿u chá»©a toÃ n bá»™ cÃ¡c tá»« quan trá»ng (bá» Ä‘á»‡m cÅ©ng Ä‘Æ°á»£c nhÆ°ng pháº£i Ä‘Ãºng há», tÃªn)
        // ÄÃ¢y lÃ  báº£n rÃºt gá»n, trong thá»±c táº¿ cÃ³ thá»ƒ dÃ¹ng Levenshtein distance
        return n1.equals(n2) || n2.contains(n1) || n1.contains(n2);
    }

    private OnboardingDocumentResponse mapToResponse(OnboardingDocument entity) {
        return OnboardingDocumentResponse.builder()
                .id(entity.getId())
                .applicationId(entity.getApplication().getId())
                .documentType(entity.getDocumentType())
                .status(entity.getStatus())
                .aiFeedback(entity.getAiFeedback())
                .uploadedAt(entity.getUploadedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
