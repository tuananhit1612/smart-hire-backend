package com.smarthire.backend.features.cv.service;

import com.smarthire.backend.features.cv.dto.CvBuilderRequest;
import com.smarthire.backend.features.cv.dto.CvBuilderResponse;

public interface CvBuilderService {
    CvBuilderResponse getCvBuilderData(Long userId);
    CvBuilderResponse saveCvBuilderData(Long userId, CvBuilderRequest request);
}
