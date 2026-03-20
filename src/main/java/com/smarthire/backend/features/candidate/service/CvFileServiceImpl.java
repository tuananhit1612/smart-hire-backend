package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.CustomUserDetails;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.dto.CvFileResponse;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.infrastructure.storage.FileStorageService;
import com.smarthire.backend.shared.enums.CvFileType;
import com.smarthire.backend.shared.enums.CvSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvFileServiceImpl implements CvFileService {

    private final CvFileRepository cvFileRepository;
    private final CandidateProfileRepository profileRepository;
    private final FileStorageService fileStorageService;

    private static final String CV_UPLOAD_DIR = "cv";

    // ─── Helper ────────────────────────────────────────────────

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private CandidateProfile getMyProfile() {
        User currentUser = getCurrentUser();
        return profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile", currentUser.getId()));
    }

    private CvFile findOwnedCvFile(Long id) {
        CandidateProfile profile = getMyProfile();
        CvFile cvFile = cvFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CV File", id));
        if (!cvFile.getCandidateProfile().getId().equals(profile.getId())) {
            throw new ResourceNotFoundException("CV File", id);
        }
        return cvFile;
    }

    private CvFileType resolveFileType(String contentType) {
        if (contentType == null) {
            throw new BadRequestException("File content type is required");
        }
        return switch (contentType) {
            case "application/pdf" -> CvFileType.PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> CvFileType.DOCX;
            default -> throw new BadRequestException("Invalid CV file type. Only PDF and DOCX are allowed.");
        };
    }

    private CvFileResponse mapToResponse(CvFile cvFile) {
        return CvFileResponse.builder()
                .id(cvFile.getId())
                .fileName(cvFile.getFileName())
                .fileType(cvFile.getFileType())
                .fileSize(cvFile.getFileSize())
                .source(cvFile.getSource())
                .isPrimary(cvFile.getIsPrimary())
                .createdAt(cvFile.getCreatedAt())
                .downloadUrl("/api/candidate/profile/cv-files/" + cvFile.getId() + "/download")
                .build();
    }

    // ─── Upload ────────────────────────────────────────────────

    @Override
    @Transactional
    public CvFileResponse uploadCv(MultipartFile file) {
        CandidateProfile profile = getMyProfile();

        // Resolve file type from content type
        CvFileType fileType = resolveFileType(file.getContentType());

        // Store file on disk via FileStorageService (validates size & type)
        String relativePath = fileStorageService.storeDocument(file, CV_UPLOAD_DIR);

        // Original filename
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "cv." + fileType.name().toLowerCase());

        // If first CV → auto primary
        boolean isFirst = cvFileRepository.countByCandidateProfileId(profile.getId()) == 0;

        CvFile cvFile = CvFile.builder()
                .candidateProfile(profile)
                .fileName(originalFilename)
                .filePath(relativePath)
                .fileType(fileType)
                .fileSize((int) file.getSize())
                .source(CvSource.UPLOAD)
                .isPrimary(isFirst)
                .build();

        cvFile = cvFileRepository.save(cvFile);
        log.info("Uploaded CV {} for user {} (primary={})", cvFile.getId(), getCurrentUser().getEmail(), isFirst);
        return mapToResponse(cvFile);
    }

    // ─── List ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CvFileResponse> getMyCvFiles() {
        CandidateProfile profile = getMyProfile();
        return cvFileRepository.findByCandidateProfileIdOrderByCreatedAtDesc(profile.getId())
                .stream().map(this::mapToResponse).toList();
    }

    // ─── Get by ID ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CvFileResponse getCvFileById(Long id) {
        CvFile cvFile = findOwnedCvFile(id);
        return mapToResponse(cvFile);
    }

    // ─── Set Primary ───────────────────────────────────────────

    @Override
    @Transactional
    public CvFileResponse setPrimary(Long id) {
        CvFile cvFile = findOwnedCvFile(id);

        // Reset all → set this one
        cvFileRepository.resetPrimaryByProfileId(cvFile.getCandidateProfile().getId());
        cvFile.setIsPrimary(true);
        cvFile = cvFileRepository.save(cvFile);

        log.info("Set CV {} as primary", cvFile.getId());
        return mapToResponse(cvFile);
    }

    // ─── Delete ────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteCvFile(Long id) {
        CvFile cvFile = findOwnedCvFile(id);

        if (Boolean.TRUE.equals(cvFile.getIsPrimary())) {
            throw new BadRequestException("Cannot delete the primary CV. Please set another CV as primary first.");
        }

        // Delete file from disk
        fileStorageService.deleteFile(cvFile.getFilePath());

        // Delete DB record
        cvFileRepository.delete(cvFile);
        log.info("Deleted CV {} (file: {})", id, cvFile.getFilePath());
    }

    // ─── Download ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Resource downloadCvFile(Long id) {
        CvFile cvFile = findOwnedCvFile(id);

        Path filePath = fileStorageService.getFilePath(cvFile.getFilePath());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("CV File on disk", id);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read CV file", e);
        }
    }
}
