package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.features.candidate.dto.CvFileResponse;
import com.smarthire.backend.shared.enums.CvSource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CvFileService {

    CvFileResponse uploadCv(MultipartFile file, CvSource source);

    List<CvFileResponse> getMyCvFiles();

    CvFileResponse getCvFileById(Long id);

    CvFileResponse setPrimary(Long id);

    void deleteCvFile(Long id);

    Resource downloadCvFile(Long id);
}
