package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.CustomUserDetails;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.dto.*;
import com.smarthire.backend.features.candidate.entity.*;
import com.smarthire.backend.features.candidate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateResumeServiceImpl implements CandidateResumeService {

    private final CandidateProfileRepository profileRepository;
    private final CandidateEducationRepository educationRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateSkillRepository skillRepository;
    private final CandidateProjectRepository projectRepository;

    // ─── Helper ────────────────────────────────────────────────

    private User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser();
    }

    private CandidateProfile getMyProfile() {
        User currentUser = getCurrentUser();
        return profileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate Profile", currentUser.getId()));
    }

    // ─── EDUCATION ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<EducationResponse> getEducations() {
        CandidateProfile profile = getMyProfile();
        return educationRepository.findByCandidateProfileIdOrderByStartDateDesc(profile.getId())
                .stream().map(this::mapEducation).toList();
    }

    @Override
    @Transactional
    public EducationResponse createEducation(EducationRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateEducation edu = CandidateEducation.builder()
                .candidateProfile(profile)
                .institution(request.getInstitution())
                .degree(request.getDegree())
                .fieldOfStudy(request.getFieldOfStudy())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .gpa(request.getGpa())
                .description(request.getDescription())
                .build();
        edu = educationRepository.save(edu);
        log.info("Created education {} for user {}", edu.getId(), getCurrentUser().getEmail());
        return mapEducation(edu);
    }

    @Override
    @Transactional
    public EducationResponse updateEducation(Long id, EducationRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateEducation edu = educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", id));
        verifyOwnership(edu.getCandidateProfile().getId(), profile.getId());

        edu.setInstitution(request.getInstitution());
        edu.setDegree(request.getDegree());
        edu.setFieldOfStudy(request.getFieldOfStudy());
        edu.setStartDate(request.getStartDate());
        edu.setEndDate(request.getEndDate());
        edu.setGpa(request.getGpa());
        edu.setDescription(request.getDescription());
        edu = educationRepository.save(edu);
        log.info("Updated education {}", edu.getId());
        return mapEducation(edu);
    }

    @Override
    @Transactional
    public void deleteEducation(Long id) {
        CandidateProfile profile = getMyProfile();
        CandidateEducation edu = educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", id));
        verifyOwnership(edu.getCandidateProfile().getId(), profile.getId());
        educationRepository.delete(edu);
        log.info("Deleted education {}", id);
    }

    // ─── EXPERIENCE ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ExperienceResponse> getExperiences() {
        CandidateProfile profile = getMyProfile();
        return experienceRepository.findByCandidateProfileIdOrderByStartDateDesc(profile.getId())
                .stream().map(this::mapExperience).toList();
    }

    @Override
    @Transactional
    public ExperienceResponse createExperience(ExperienceRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateExperience exp = CandidateExperience.builder()
                .candidateProfile(profile)
                .companyName(request.getCompanyName())
                .title(request.getTitle())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : false)
                .description(request.getDescription())
                .build();
        exp = experienceRepository.save(exp);
        log.info("Created experience {} for user {}", exp.getId(), getCurrentUser().getEmail());
        return mapExperience(exp);
    }

    @Override
    @Transactional
    public ExperienceResponse updateExperience(Long id, ExperienceRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateExperience exp = experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id));
        verifyOwnership(exp.getCandidateProfile().getId(), profile.getId());

        exp.setCompanyName(request.getCompanyName());
        exp.setTitle(request.getTitle());
        exp.setStartDate(request.getStartDate());
        exp.setEndDate(request.getEndDate());
        exp.setIsCurrent(request.getIsCurrent() != null ? request.getIsCurrent() : false);
        exp.setDescription(request.getDescription());
        exp = experienceRepository.save(exp);
        log.info("Updated experience {}", exp.getId());
        return mapExperience(exp);
    }

    @Override
    @Transactional
    public void deleteExperience(Long id) {
        CandidateProfile profile = getMyProfile();
        CandidateExperience exp = experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id));
        verifyOwnership(exp.getCandidateProfile().getId(), profile.getId());
        experienceRepository.delete(exp);
        log.info("Deleted experience {}", id);
    }

    // ─── SKILL ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills() {
        CandidateProfile profile = getMyProfile();
        return skillRepository.findByCandidateProfileId(profile.getId())
                .stream().map(this::mapSkill).toList();
    }

    @Override
    @Transactional
    public SkillResponse createSkill(SkillRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateSkill skill = CandidateSkill.builder()
                .candidateProfile(profile)
                .skillName(request.getSkillName())
                .proficiencyLevel(request.getProficiencyLevel())
                .build();
        skill = skillRepository.save(skill);
        log.info("Created skill {} for user {}", skill.getId(), getCurrentUser().getEmail());
        return mapSkill(skill);
    }

    @Override
    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateSkill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));
        verifyOwnership(skill.getCandidateProfile().getId(), profile.getId());

        skill.setSkillName(request.getSkillName());
        skill.setProficiencyLevel(request.getProficiencyLevel());
        skill = skillRepository.save(skill);
        log.info("Updated skill {}", skill.getId());
        return mapSkill(skill);
    }

    @Override
    @Transactional
    public void deleteSkill(Long id) {
        CandidateProfile profile = getMyProfile();
        CandidateSkill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));
        verifyOwnership(skill.getCandidateProfile().getId(), profile.getId());
        skillRepository.delete(skill);
        log.info("Deleted skill {}", id);
    }

    // ─── PROJECT ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        CandidateProfile profile = getMyProfile();
        return projectRepository.findByCandidateProfileId(profile.getId())
                .stream().map(this::mapProject).toList();
    }

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateProject project = CandidateProject.builder()
                .candidateProfile(profile)
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .technologies(request.getTechnologies())
                .build();
        project = projectRepository.save(project);
        log.info("Created project {} for user {}", project.getId(), getCurrentUser().getEmail());
        return mapProject(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        CandidateProfile profile = getMyProfile();
        CandidateProject project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        verifyOwnership(project.getCandidateProfile().getId(), profile.getId());

        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setTechnologies(request.getTechnologies());
        project = projectRepository.save(project);
        log.info("Updated project {}", project.getId());
        return mapProject(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        CandidateProfile profile = getMyProfile();
        CandidateProject project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        verifyOwnership(project.getCandidateProfile().getId(), profile.getId());
        projectRepository.delete(project);
        log.info("Deleted project {}", id);
    }

    // ─── MAPPERS ───────────────────────────────────────────────

    private void verifyOwnership(Long itemProfileId, Long currentProfileId) {
        if (!itemProfileId.equals(currentProfileId)) {
            throw new ResourceNotFoundException("Resource", itemProfileId);
        }
    }

    private EducationResponse mapEducation(CandidateEducation edu) {
        return EducationResponse.builder()
                .id(edu.getId())
                .institution(edu.getInstitution())
                .degree(edu.getDegree())
                .fieldOfStudy(edu.getFieldOfStudy())
                .startDate(edu.getStartDate())
                .endDate(edu.getEndDate())
                .gpa(edu.getGpa())
                .description(edu.getDescription())
                .build();
    }

    private ExperienceResponse mapExperience(CandidateExperience exp) {
        return ExperienceResponse.builder()
                .id(exp.getId())
                .companyName(exp.getCompanyName())
                .title(exp.getTitle())
                .startDate(exp.getStartDate())
                .endDate(exp.getEndDate())
                .isCurrent(exp.getIsCurrent())
                .description(exp.getDescription())
                .build();
    }

    private SkillResponse mapSkill(CandidateSkill skill) {
        return SkillResponse.builder()
                .id(skill.getId())
                .skillName(skill.getSkillName())
                .proficiencyLevel(skill.getProficiencyLevel())
                .build();
    }

    private ProjectResponse mapProject(CandidateProject project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .technologies(project.getTechnologies())
                .build();
    }
}
