package com.smarthire.backend.features.company.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ConflictException;
import com.smarthire.backend.core.exception.ForbiddenException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.company.dto.CompanyResponse;
import com.smarthire.backend.features.company.dto.CreateCompanyRequest;
import com.smarthire.backend.features.company.dto.UpdateCompanyRequest;
import com.smarthire.backend.features.company.dto.*;
import com.smarthire.backend.features.company.entity.Company;
import com.smarthire.backend.features.company.entity.embeddable.CompanyBenefit;
import com.smarthire.backend.features.company.entity.embeddable.CompanySocialLink;
import com.smarthire.backend.features.company.repository.CompanyRepository;
import com.smarthire.backend.infrastructure.storage.FileStorageService;
import com.smarthire.backend.shared.enums.CompanySize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();

        if (companyRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Company with this name already exists");
        }

        Company company = Company.builder()
                .name(request.getName())
                .website(request.getWebsite())
                .industry(request.getIndustry())
                .companySize(parseCompanySize(request.getCompanySize()))
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .coverUrl(request.getCoverUrl())
                .tagline(request.getTagline())
                .email(request.getEmail())
                .phone(request.getPhone())
                .founded(request.getFounded())
                .techStack(request.getTechStack() != null ? request.getTechStack() : List.of())
                .benefits(parseBenefits(request.getBenefits()))
                .socialLinks(parseSocialLinks(request.getSocialLinks()))
                .createdBy(currentUser)
                .isVerified(false)
                .build();

        company = companyRepository.save(company);
        log.info("Company created: {} by user {}", company.getName(), currentUser.getEmail());
        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = findCompanyOrThrow(id);
        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> getMyCompanies() {
        User currentUser = SecurityUtils.getCurrentUser();
        return companyRepository.findByCreatedById(currentUser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(Long id, UpdateCompanyRequest request) {
        Company company = findCompanyOrThrow(id);
        checkOwnership(company);

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!request.getName().equalsIgnoreCase(company.getName())
                    && companyRepository.existsByNameIgnoreCase(request.getName())) {
                throw new ConflictException("Company with this name already exists");
            }
            company.setName(request.getName());
        }
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());
        if (request.getIndustry() != null) company.setIndustry(request.getIndustry());
        if (request.getCompanySize() != null) company.setCompanySize(parseCompanySize(request.getCompanySize()));
        if (request.getDescription() != null) company.setDescription(request.getDescription());
        if (request.getAddress() != null) company.setAddress(request.getAddress());
        if (request.getCity() != null) company.setCity(request.getCity());
        if (request.getCoverUrl() != null) company.setCoverUrl(request.getCoverUrl());
        
        if (request.getTagline() != null) company.setTagline(request.getTagline());
        if (request.getEmail() != null) company.setEmail(request.getEmail());
        if (request.getPhone() != null) company.setPhone(request.getPhone());
        if (request.getFounded() != null) company.setFounded(request.getFounded());
        if (request.getTechStack() != null) {
            company.getTechStack().clear();
            company.getTechStack().addAll(request.getTechStack());
        }
        if (request.getBenefits() != null) {
            company.getBenefits().clear();
            company.getBenefits().addAll(parseBenefits(request.getBenefits()));
        }
        if (request.getSocialLinks() != null) {
            company.getSocialLinks().clear();
            company.getSocialLinks().addAll(parseSocialLinks(request.getSocialLinks()));
        }

        company = companyRepository.save(company);
        log.info("Company updated: {}", company.getName());
        return toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse uploadLogo(Long id, MultipartFile file) {
        Company company = findCompanyOrThrow(id);
        checkOwnership(company);

        // Xóa logo cũ nếu có
        if (company.getLogoUrl() != null && !company.getLogoUrl().isBlank()) {
            fileStorageService.deleteFile(company.getLogoUrl());
        }

        String logoPath = fileStorageService.storeImage(file, "logos");
        company.setLogoUrl(logoPath);
        company = companyRepository.save(company);

        log.info("Logo uploaded for company: {}", company.getName());
        return toResponse(company);
    }

    @Override
    @Transactional
    public CompanyResponse uploadCover(Long id, MultipartFile file) {
        Company company = findCompanyOrThrow(id);
        checkOwnership(company);

        // Xóa cover cũ nếu có
        if (company.getCoverUrl() != null && !company.getCoverUrl().isBlank() && !company.getCoverUrl().startsWith("http")) {
            fileStorageService.deleteFile(company.getCoverUrl());
        }

        String coverPath = fileStorageService.storeImage(file, "covers");
        company.setCoverUrl(coverPath);
        company = companyRepository.save(company);

        log.info("Cover uploaded for company: {}", company.getName());
        return toResponse(company);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        Company company = findCompanyOrThrow(id);
        checkOwnership(company);

        // Xóa logo nếu có
        if (company.getLogoUrl() != null && !company.getLogoUrl().isBlank() && !company.getLogoUrl().startsWith("http")) {
            fileStorageService.deleteFile(company.getLogoUrl());
        }
        
        // Xóa cover nếu có
        if (company.getCoverUrl() != null && !company.getCoverUrl().isBlank() && !company.getCoverUrl().startsWith("http")) {
            fileStorageService.deleteFile(company.getCoverUrl());
        }

        companyRepository.delete(company);
        log.info("Company deleted: {}", company.getName());
    }

    // ── Helpers ──

    private Company findCompanyOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id: " + id));
    }

    private void checkOwnership(Company company) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (!company.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to modify this company");
        }
    }

    private CompanySize parseCompanySize(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) return null;
        try {
            return CompanySize.valueOf(sizeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid company size. Must be: STARTUP, SMALL, MEDIUM, LARGE, ENTERPRISE");
        }
    }

    private List<CompanyBenefit> parseBenefits(List<CompanyBenefitDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> CompanyBenefit.builder()
                .id(dto.getId())
                .icon(dto.getIcon())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .build()).toList();
    }

    private List<CompanySocialLink> parseSocialLinks(List<CompanySocialLinkDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> CompanySocialLink.builder()
                .platform(dto.getPlatform())
                .url(dto.getUrl())
                .build()).toList();
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .logoUrl(company.getLogoUrl())
                .coverUrl(company.getCoverUrl())
                .website(company.getWebsite())
                .industry(company.getIndustry())
                .companySize(company.getCompanySize())
                .description(company.getDescription())
                .address(company.getAddress())
                .city(company.getCity())
                .tagline(company.getTagline())
                .email(company.getEmail())
                .phone(company.getPhone())
                .founded(company.getFounded())
                .techStack(company.getTechStack())
                .benefits(company.getBenefits() != null ? company.getBenefits().stream().map(b -> CompanyBenefitDto.builder()
                        .id(b.getId()).icon(b.getIcon()).title(b.getTitle()).description(b.getDescription()).build()).toList() : List.of())
                .socialLinks(company.getSocialLinks() != null ? company.getSocialLinks().stream().map(s -> CompanySocialLinkDto.builder()
                        .platform(s.getPlatform()).url(s.getUrl()).build()).toList() : List.of())
                .createdBy(company.getCreatedBy().getId())
                .isVerified(company.getIsVerified())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
