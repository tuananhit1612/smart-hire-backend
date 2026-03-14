package com.smarthire.backend.features.company.repository;

import com.smarthire.backend.features.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByCreatedById(Long userId);

    boolean existsByNameIgnoreCase(String name);
}
