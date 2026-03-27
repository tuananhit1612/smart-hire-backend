package com.smarthire.backend.features.application.repository;

import com.smarthire.backend.features.application.entity.ApplicationAiResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationAiResultRepository extends JpaRepository<ApplicationAiResult, Long> {
    Optional<ApplicationAiResult> findByApplicationId(Long applicationId);
}
