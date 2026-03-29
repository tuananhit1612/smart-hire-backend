package com.smarthire.backend.infrastructure.database;

import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.company.entity.Company;
import com.smarthire.backend.features.company.repository.CompanyRepository;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.entity.JobSkill;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.CompanySize;
import com.smarthire.backend.shared.enums.JobLevel;
import com.smarthire.backend.shared.enums.JobStatus;
import com.smarthire.backend.shared.enums.JobType;
import com.smarthire.backend.shared.enums.Role;
import com.smarthire.backend.shared.enums.SkillType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DevJobSeeder implements ApplicationRunner {

    private static final String HR_EMAIL = "hr@smarthire.local";
    private static final String HR_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (jobRepository.count() > 0) {
            log.info("Dev seed skipped: jobs already exist");
            return;
        }

        User hrUser = userRepository.findByEmail(HR_EMAIL)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(HR_EMAIL)
                        .passwordHash(passwordEncoder.encode(HR_PASSWORD))
                        .role(Role.HR)
                        .fullName("HR Demo")
                        .phone("+84 900 000 001")
                        .isActive(true)
                        .isOnboarded(true)
                        .build()));

        Company novaTech = companyRepository.save(Company.builder()
                .name("NovaTech")
                .logoUrl("https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=256&q=80")
                .website("https://novatech.example")
                .industry("Software")
                .companySize(CompanySize.MEDIUM)
                .description("Sản phẩm SaaS cho thị trường Đông Nam Á.")
                .address("Quận 1")
                .city("Hồ Chí Minh")
                .createdBy(hrUser)
                .isVerified(true)
                .build());

        Company greenByte = companyRepository.save(Company.builder()
                .name("GreenByte")
                .logoUrl("https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=256&q=80")
                .website("https://greenbyte.example")
                .industry("FinTech")
                .companySize(CompanySize.STARTUP)
                .description("Team nhỏ, tốc độ cao, tập trung vào trải nghiệm người dùng.")
                .address("Hải Châu")
                .city("Đà Nẵng")
                .createdBy(hrUser)
                .isVerified(true)
                .build());

        List<Job> jobs = List.of(
                buildJob(
                        novaTech,
                        hrUser,
                        "Backend Engineer (Java Spring)",
                        "Xây dựng REST APIs, tối ưu truy vấn và đảm bảo chất lượng hệ thống.",
                        "- 2+ năm Java/Spring Boot\n- MySQL, JPA\n- Hiểu biết về security/JWT",
                        "- 13th month\n- Hybrid policy\n- Review lương 2 lần/năm",
                        JobType.FULL_TIME,
                        JobLevel.MID,
                        "Hồ Chí Minh",
                        false,
                        new BigDecimal("25000000"),
                        new BigDecimal("40000000"),
                        "VND",
                        LocalDate.now().plusDays(30),
                        List.of(
                                skill("Java", SkillType.MUST_HAVE),
                                skill("Spring Boot", SkillType.MUST_HAVE),
                                skill("MySQL", SkillType.MUST_HAVE),
                                skill("Docker", SkillType.NICE_TO_HAVE)
                        )
                ),
                buildJob(
                        novaTech,
                        hrUser,
                        "Frontend Engineer (Next.js)",
                        "Phát triển UI/UX, tối ưu performance và tích hợp API.",
                        "- React/Next.js\n- TypeScript\n- Biết Tailwind là lợi thế",
                        "- Remote 2 ngày/tuần\n- Macbook/Monitor\n- Training budget",
                        JobType.FULL_TIME,
                        JobLevel.JUNIOR,
                        "Hà Nội",
                        true,
                        new BigDecimal("18000000"),
                        new BigDecimal("28000000"),
                        "VND",
                        LocalDate.now().plusDays(25),
                        List.of(
                                skill("React", SkillType.MUST_HAVE),
                                skill("Next.js", SkillType.MUST_HAVE),
                                skill("TypeScript", SkillType.MUST_HAVE),
                                skill("Tailwind CSS", SkillType.NICE_TO_HAVE)
                        )
                ),
                buildJob(
                        greenByte,
                        hrUser,
                        "Data Analyst Intern",
                        "Hỗ trợ phân tích dữ liệu sản phẩm, tạo dashboard và báo cáo.",
                        "- SQL cơ bản\n- Excel/Google Sheets\n- Tư duy logic",
                        "- Phụ cấp\n- Mentor 1-1\n- Cơ hội lên Fresher",
                        JobType.INTERNSHIP,
                        JobLevel.INTERN,
                        "Đà Nẵng",
                        false,
                        new BigDecimal("3000000"),
                        new BigDecimal("6000000"),
                        "VND",
                        LocalDate.now().plusDays(20),
                        List.of(
                                skill("SQL", SkillType.MUST_HAVE),
                                skill("Excel", SkillType.MUST_HAVE),
                                skill("Power BI", SkillType.NICE_TO_HAVE)
                        )
                ),
                buildJob(
                        greenByte,
                        hrUser,
                        "DevOps Engineer",
                        "Xây dựng CI/CD, quan sát hệ thống và tối ưu hạ tầng.",
                        "- Linux\n- Docker\n- CI/CD (GitHub Actions)\n- Biết Kubernetes là lợi thế",
                        "- ESOP\n- Remote-first\n- Thiết bị tuỳ chọn",
                        JobType.CONTRACT,
                        JobLevel.SENIOR,
                        "Remote",
                        true,
                        new BigDecimal("35000000"),
                        new BigDecimal("55000000"),
                        "VND",
                        LocalDate.now().plusDays(35),
                        List.of(
                                skill("Linux", SkillType.MUST_HAVE),
                                skill("Docker", SkillType.MUST_HAVE),
                                skill("CI/CD", SkillType.MUST_HAVE),
                                skill("Kubernetes", SkillType.NICE_TO_HAVE)
                        )
                )
        );

        jobRepository.saveAll(jobs);
        log.info("Dev seed completed: inserted {} jobs (public: status OPEN)", jobs.size());
        log.info("Seed HR account: {} / {}", HR_EMAIL, HR_PASSWORD);
    }

    private static JobSkill skill(String name, SkillType type) {
        return JobSkill.builder()
                .skillName(name)
                .skillType(type)
                .build();
    }

    private static Job buildJob(
            Company company,
            User createdBy,
            String title,
            String description,
            String requirements,
            String benefits,
            JobType jobType,
            JobLevel jobLevel,
            String location,
            boolean isRemote,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            LocalDate deadline,
            List<JobSkill> skills
    ) {
        Job job = Job.builder()
                .company(company)
                .createdBy(createdBy)
                .title(title)
                .description(description)
                .requirements(requirements)
                .benefits(benefits)
                .jobType(jobType)
                .jobLevel(jobLevel)
                .location(location)
                .isRemote(isRemote)
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .salaryCurrency(salaryCurrency)
                .deadline(deadline)
                .status(JobStatus.OPEN)
                .build();

        skills.forEach(s -> {
            s.setJob(job);
            job.getSkills().add(s);
        });

        return job;
    }
}
