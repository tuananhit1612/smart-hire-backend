package com.smarthire.backend.features.company.service;

import com.smarthire.backend.features.company.dto.CompanyResponse;
import com.smarthire.backend.features.company.dto.CreateCompanyRequest;
import com.smarthire.backend.features.company.dto.UpdateCompanyRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CompanyService {

    CompanyResponse createCompany(CreateCompanyRequest request);

    CompanyResponse getCompanyById(Long id);

    List<CompanyResponse> getAllCompanies();

    List<CompanyResponse> getMyCompanies();

    CompanyResponse updateCompany(Long id, UpdateCompanyRequest request);

    CompanyResponse uploadLogo(Long id, MultipartFile file);

    CompanyResponse uploadCover(Long id, MultipartFile file);

    void deleteCompany(Long id);
}
