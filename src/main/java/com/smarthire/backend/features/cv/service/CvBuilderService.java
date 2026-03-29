package com.smarthire.backend.features.cv.service;

import com.smarthire.backend.features.cv.dto.CvBuilderRequest;
import com.smarthire.backend.features.cv.dto.CvBuilderResponse;

import java.util.List;

public interface CvBuilderService {
    /** Lấy TẤT CẢ CV của user (danh sách) */
    List<CvBuilderResponse> getAllCvBuilderData(Long userId);

    /** Lấy 1 CV cụ thể theo cvFileId */
    CvBuilderResponse getCvBuilderDataByCvFileId(Long cvFileId);

    /** Tạo MỚI 1 CV builder data (luôn tạo record mới, không ghi đè) */
    CvBuilderResponse createCvBuilderData(Long userId, CvBuilderRequest request);

    /** Cập nhật CV cụ thể theo cvFileId (dùng khi save từ CV Analysis hoặc edit) */
    CvBuilderResponse updateCvBuilderDataByCvFileId(Long cvFileId, CvBuilderRequest request);
}
