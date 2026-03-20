package com.smarthire.backend.features.candidate.service;

import com.smarthire.backend.features.candidate.dto.*;

import java.util.List;

public interface CandidateResumeService {

    // Education
    List<EducationResponse> getEducations();
    EducationResponse createEducation(EducationRequest request);
    EducationResponse updateEducation(Long id, EducationRequest request);
    void deleteEducation(Long id);

    // Experience
    List<ExperienceResponse> getExperiences();
    ExperienceResponse createExperience(ExperienceRequest request);
    ExperienceResponse updateExperience(Long id, ExperienceRequest request);
    void deleteExperience(Long id);

    // Skill
    List<SkillResponse> getSkills();
    SkillResponse createSkill(SkillRequest request);
    SkillResponse updateSkill(Long id, SkillRequest request);
    void deleteSkill(Long id);

    // Project
    List<ProjectResponse> getProjects();
    ProjectResponse createProject(ProjectRequest request);
    ProjectResponse updateProject(Long id, ProjectRequest request);
    void deleteProject(Long id);
}
