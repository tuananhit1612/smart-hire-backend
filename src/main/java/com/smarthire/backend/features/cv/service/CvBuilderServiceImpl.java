package com.smarthire.backend.features.cv.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.entity.CvFile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.candidate.repository.CvFileRepository;
import com.smarthire.backend.features.cv.dto.CvBuilderRequest;
import com.smarthire.backend.features.cv.dto.CvBuilderResponse;
import com.smarthire.backend.features.cv.entity.CvBuilderData;
import com.smarthire.backend.features.cv.repository.CvBuilderDataRepository;
import com.smarthire.backend.shared.enums.CvFileType;
import com.smarthire.backend.shared.enums.CvSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvBuilderServiceImpl implements CvBuilderService {

    private final CvBuilderDataRepository cvBuilderDataRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CvFileRepository cvFileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CvBuilderResponse> getAllCvBuilderData(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidateProfile not found for user id: " + userId));

        return cvBuilderDataRepository.findAllByCandidateProfileIdOrderByUpdatedAtDesc(profile.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CvBuilderResponse getCvBuilderDataByCvFileId(Long cvFileId) {
        CvBuilderData data = cvBuilderDataRepository.findByCvFileId(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("No CV Builder Data found for cvFileId: " + cvFileId));

        return mapToResponse(data);
    }

    @Override
    @Transactional
    public CvBuilderResponse createCvBuilderData(Long userId, CvBuilderRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidateProfile not found for user id: " + userId));

        // Luôn tạo CvFile MỚI cho mỗi CV
        String cvTitle = request.getTitle() != null ? request.getTitle() : "CV Builder #" + System.currentTimeMillis();
        CvFile newCvFile = CvFile.builder()
                .candidateProfile(profile)
                .fileName(cvTitle)
                .filePath("/builder-cvs/" + profile.getId() + "/" + System.currentTimeMillis() + ".pdf")
                .fileType(CvFileType.PDF)
                .fileSize(0)
                .source(CvSource.BUILDER)
                .isPrimary(false)
                .build();
        newCvFile = cvFileRepository.save(newCvFile);

        // Tạo CvBuilderData MỚI gắn với CvFile mới
        CvBuilderData data = CvBuilderData.builder()
                .cvFile(newCvFile)
                .candidateProfile(profile)
                .templateId(request.getTemplateId())
                .sectionsData(request.getSectionsData())
                .build();
        data = cvBuilderDataRepository.save(data);

        log.info("✅ Created new CV builder data — id={}, cvFileId={}", data.getId(), newCvFile.getId());
        return mapToResponse(data);
    }

    @Override
    @Transactional
    public CvBuilderResponse updateCvBuilderDataByCvFileId(Long cvFileId, CvBuilderRequest request) {
        CvBuilderData data = cvBuilderDataRepository.findByCvFileId(cvFileId)
                .orElseThrow(() -> new ResourceNotFoundException("No CV Builder Data found for cvFileId: " + cvFileId));

        data.setTemplateId(request.getTemplateId());
        data.setSectionsData(request.getSectionsData());
        data = cvBuilderDataRepository.save(data);

        // Cập nhật tên file nếu có title
        if (request.getTitle() != null) {
            CvFile cvFile = data.getCvFile();
            cvFile.setFileName(request.getTitle());
            cvFileRepository.save(cvFile);
        }

        log.info("✅ Updated CV builder data — cvFileId={}", cvFileId);
        return mapToResponse(data);
    }

    private CvBuilderResponse mapToResponse(CvBuilderData entity) {
        return CvBuilderResponse.builder()
                .id(entity.getId())
                .cvFileId(entity.getCvFile().getId())
                .candidateProfileId(entity.getCandidateProfile().getId())
                .templateId(entity.getTemplateId())
                .sectionsData(entity.getSectionsData())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
