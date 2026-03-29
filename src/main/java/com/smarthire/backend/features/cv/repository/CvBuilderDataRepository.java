package com.smarthire.backend.features.cv.repository;

import com.smarthire.backend.features.cv.entity.CvBuilderData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvBuilderDataRepository extends JpaRepository<CvBuilderData, Long> {
    /** Trả về TẤT CẢ các CV builder data của 1 profile (hỗ trợ nhiều CV) */
    List<CvBuilderData> findAllByCandidateProfileIdOrderByUpdatedAtDesc(Long candidateProfileId);

    /** Tìm 1 CvBuilderData cụ thể theo cvFileId */
    Optional<CvBuilderData> findByCvFileId(Long cvFileId);
}
