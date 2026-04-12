package com.smarthire.backend.features.onboarding.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.exception.UnauthorizedException;
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
    public OnboardingDocumentResponse uploadDocument(Long applicationId, Long userId, MultipartFile file, DocumentType type) {
        Application application = getApplicationAndVerifyAccess(applicationId, userId);

        if (application.getStage() != ApplicationStage.OFFER && application.getStage() != ApplicationStage.HIRED) {
            throw new BadRequestException("Chỉ có thể tải lên hồ sơ nhận việc khi trạng thái ứng tuyển là OFFER hoặc HIRED");
        }

        // 1. Lưu file xuống ổ cứng (private folder)
        String relativePath = fileStorageService.storeOnboardingFile(file, "onboarding_docs");
        Path absolutePath = fileStorageService.getFilePath(relativePath);

        VerificationStatus status = VerificationStatus.PENDING;
        String aiFeedback = null;

        // 2. Chạy AI Verify nếu là giấy tờ tùy thân
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
                String profileName = candidate.getUser() != null ? candidate.getUser().getFullName() : "Ứng viên";
                
                // OCR Name cross-check (Cơ bản)
                if (extractedName != null && !extractedName.isBlank() && profileName != null && !profileName.isBlank()) {
                    boolean isMatch = checkNameMatch(profileName, extractedName);
                    if (!isMatch) {
                        status = VerificationStatus.REJECTED;
                        aiFeedback = String.format("Tên trên CCCD (%s) không khớp với tên hồ sơ (%s). Vui lòng kiểm tra lại.", extractedName, profileName);
                        log.warn("Name mismatch! Profile: {}, OCR: {}", profileName, extractedName);
                    } else {
                        status = VerificationStatus.VERIFIED;
                        aiFeedback = "Hồ sơ CCCD hợp lệ và tên ứng viên trùng khớp.";
                    }
                } else {
                    status = VerificationStatus.VERIFIED;
                    aiFeedback = "Không xác nhận được tên chính sác qua OCR, nhưng hình ảnh CCCD hợp lệ.";
                }
            }
        }

        // 3. Cập nhật hoặc lưu mới vào DB
        List<OnboardingDocument> existingDocs = onboardingDocumentRepository.findByApplication_Id(applicationId)
                .stream()
                .filter(d -> d.getDocumentType() == type)
                .collect(Collectors.toList());

        OnboardingDocument document;
        if (!existingDocs.isEmpty()) {
            // Cập nhật bản ghi đã có để tránh sinh ra trùng lặp (duplication)
            document = existingDocs.get(0);
            document.setFilePath(relativePath);
            document.setStatus(status);
            document.setAiFeedback(aiFeedback);
            
            // Xóa các bản ghi thừa nếu bị trùng lặp do lỗi trước đây
            if (existingDocs.size() > 1) {
                for (int i = 1; i < existingDocs.size(); i++) {
                    onboardingDocumentRepository.delete(existingDocs.get(i));
                }
            }
        } else {
            // Tạo mới nếu chưa từng tải lên
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
    public List<OnboardingDocumentResponse> getDocumentsByApplication(Long applicationId, Long userId) {
        log.info("Fetching onboarding documents for application {} by user {}", applicationId, userId);
        getApplicationAndVerifyAccess(applicationId, userId);
        
        return onboardingDocumentRepository.findByApplication_Id(applicationId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadDocument(Long documentId, Long userId) {
        OnboardingDocument doc = onboardingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingDocument", documentId));
                
        // Xác thực quyền download
        getApplicationAndVerifyAccess(doc.getApplication().getId(), userId);
        
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
    public OnboardingDocumentResponse updateDocumentStatus(Long documentId, Long hrUserId, VerificationStatus status, String comment) {
        OnboardingDocument doc = onboardingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("OnboardingDocument", documentId));

        Application application = doc.getApplication();
        
        // Chỉ duyệt được nếu HRUserId là người tạo Company (owner)
        boolean isHrOwner = application.getJob().getCompany().getCreatedBy() != null
                          && application.getJob().getCompany().getCreatedBy().getId().equals(hrUserId);
                          
        if (!isHrOwner) {
            throw new UnauthorizedException("Chỉ chủ doanh nghiệp phòng ban này mới được duyệt hồ sơ");
        }
        
        doc.setStatus(status);
        if (comment != null && !comment.trim().isEmpty()) {
            doc.setAiFeedback(comment); // Dùng lại trường phản hồi để lưu feedback nhân sự
        }
        
        return mapToResponse(onboardingDocumentRepository.save(doc));
    }

    private Application getApplicationAndVerifyAccess(Long applicationId, Long userId) {
        if (applicationId == null || userId == null) {
            log.error("Null applicationId ({}) or userId ({}) in access verification", applicationId, userId);
            throw new BadRequestException("ID ứng tuyển hoặc ID người dùng không hợp lệ");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));
                
        // Kiểm tra quyền. Chỉ ứng viên của đơn đó, hoặc HR sở hữu job đó mới được xem/upload.
        CandidateProfile cp = application.getCandidateProfile();
        boolean isCandidate = cp != null && cp.getUser() != null && cp.getUser().getId().equals(userId);
        
        boolean isHrOwner = false;
        if (application.getJob() != null && application.getJob().getCompany() != null && application.getJob().getCompany().getCreatedBy() != null) {
            isHrOwner = application.getJob().getCompany().getCreatedBy().getId().equals(userId);
        }
                          
        if (!isCandidate && !isHrOwner) {
            log.warn("Access denied for user {} to application {}. Roles: candidate={}, hr={}", userId, applicationId, isCandidate, isHrOwner);
            throw new UnauthorizedException("Bạn không có quyền truy cập hồ sơ của đơn ứng tuyển này");
        }
        
        return application;
    }
    
    // Fuzzy match cơ bản kiểm tra sự trùng khớp của tên (cực kỳ hữu ích để chống giả mạo profile)
    private boolean checkNameMatch(String dbName, String ocrName) {
        String n1 = dbName.toLowerCase().trim().replaceAll("[^a-zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ\\s]", "");
        String n2 = ocrName.toLowerCase().trim().replaceAll("[^a-zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ\\s]", "");
        
        // Cắt khoảng trắng thừa
        n1 = n1.replaceAll("\\s+", " ");
        n2 = n2.replaceAll("\\s+", " ");
        
        // Log logic match
        log.info("Checking name match: DB='{}' vs OCR='{}'", n1, n2);
        
        // Sẽ trả về true nếu chứa toàn bộ các từ quan trọng (bỏ đệm cũng được nhưng phải đúng họ, tên)
        // Đây là bản rút gọn, trong thực tế có thể dùng Levenshtein distance
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
