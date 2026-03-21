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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CvBuilderServiceImpl implements CvBuilderService {

    private final CvBuilderDataRepository cvBuilderDataRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CvFileRepository cvFileRepository;

    @Override
    @Transactional(readOnly = true)
    public CvBuilderResponse getCvBuilderData(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidateProfile not found for user id: " + userId));

        CvBuilderData data = cvBuilderDataRepository.findByCandidateProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No CV Builder Data found for this profile"));

        return mapToResponse(data);
    }

    @Override
    @Transactional
    public CvBuilderResponse saveCvBuilderData(Long userId, CvBuilderRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("CandidateProfile not found for user id: " + userId));

        CvBuilderData data = cvBuilderDataRepository.findByCandidateProfileId(profile.getId())
                .orElseGet(() -> {
                    // Create minimal CvFile for builder data if it doesn't exist
                    CvFile newCvFile = CvFile.builder()
                            .candidateProfile(profile)
                            .fileName("Generated Builder CV")
                            .filePath("/builder-cvs/" + profile.getId() + ".pdf")
                            .fileType(CvFileType.PDF)
                            .fileSize(0)
                            .source(CvSource.BUILDER)
                            .isPrimary(false)
                            .build();
                    newCvFile = cvFileRepository.save(newCvFile);

                    return CvBuilderData.builder()
                            .cvFile(newCvFile)
                            .candidateProfile(profile)
                            .build();
                });

        data.setTemplateId(request.getTemplateId());
        data.setSectionsData(request.getSectionsData());

        data = cvBuilderDataRepository.save(data);

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
