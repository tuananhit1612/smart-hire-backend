package com.smarthire.backend.core.config;

import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.auth.repository.UserRepository;
import com.smarthire.backend.features.company.entity.Company;
import com.smarthire.backend.features.company.entity.embeddable.CompanyBenefit;
import com.smarthire.backend.features.company.entity.embeddable.CompanySocialLink;
import com.smarthire.backend.features.company.repository.CompanyRepository;
import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.features.job.entity.JobSkill;
import com.smarthire.backend.features.job.repository.JobRepository;
import com.smarthire.backend.shared.enums.CompanySize;
import com.smarthire.backend.shared.enums.JobLevel;
import com.smarthire.backend.shared.enums.JobStatus;
import com.smarthire.backend.shared.enums.JobType;
import com.smarthire.backend.shared.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            log.info("Database seeding is disabled. Skipping...");
            return;
        }

        if (userRepository.findByEmail("hr@vng.com.vn").isPresent()) {
            log.info("Database already seeded with professional data. Skipping...");
            return;
        }

        log.info("Starting database seeding (20 Companies, 50 Jobs)...");

        String genericPassword = passwordEncoder.encode("Password123!");
        List<Company> createdCompanies = new ArrayList<>();

        // 1. Array of 20 Top Companies in VN
        Object[][] companyData = {
            {"VNG Corporation", "hr@vng.com.vn", "https://vcdn1-doanhnghiep.vnecdn.net/2021/04/09/VNG-Logo-1617940173-1617940259-8669-1617940381.png?w=0&h=0&q=100&dpr=1&fit=crop&s=fQJm5gK4r6qJ_oZQ-k7mow", "https://vng.com.vn/wp-content/uploads/2023/10/About-VNG-1.png", "https://vng.com.vn", "IT / Entertainment", CompanySize.ENTERPRISE, "Kiến tạo công nghệ, Dẫn dắt tương lai", "Z06 Đường số 13, Tân Thuận Đông, Quận 7", "Hồ Chí Minh", "2004", "Kiến tạo hệ sinh thái công nghệ hàng đầu Việt Nam với Zalo, Zing MP3, VNGGames."},
            {"FPT Software", "tuyendung@fsoft.com.vn", "https://fptsoftware.com/wp-content/uploads/2023/12/logo-fpt-software.png", "https://fpt.com/fpt-software/cover.jpg", "https://fptsoftware.com", "IT / Outsourcing", CompanySize.ENTERPRISE, "Tiên phong công nghệ số", "F-Town 3, Khu công nghệ cao, Quận 9", "Hồ Chí Minh", "1999", "Tập đoàn công nghệ hàng đầu Việt Nam cung cấp dịch vụ xuất khẩu phần mềm và chuyển đổi số."},
            {"Viettel Group", "hr@viettel.com.vn", "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/Viettel_logo_2021.svg/1200px-Viettel_logo_2021.svg.png", "https://viettel.vn/cover.jpg", "https://viettel.vn", "Telecommunications / IT", CompanySize.ENTERPRISE, "Hãy nói theo cách của bạn", "1 Trần Hữu Dực, Nam Từ Liêm", "Hà Nội", "1989", "Tập đoàn Công nghiệp - Viễn thông Quân đội lớn nhất Việt Nam."},
            {"MoMo (M-Service)", "talent@momo.vn", "https://upload.wikimedia.org/wikipedia/vi/f/fe/MoMo_Logo.png", "https://momo.vn/cover.jpg", "https://momo.vn", "FinTech", CompanySize.LARGE, "Siêu ứng dụng thanh toán", "Lầu 6, Toà nhà Phú Mỹ Hưng, Quận 7", "Hồ Chí Minh", "2007", "Kỳ lân công nghệ fintech hàng đầu cung cấp siêu ứng dụng thanh toán điện tử tại Việt Nam."},
            {"Shopee Việt Nam", "careers@shopee.vn", "https://upload.wikimedia.org/wikipedia/commons/f/fe/Shopee.svg", "https://shopee.vn/cover.jpg", "https://careers.shopee.vn", "E-commerce", CompanySize.ENTERPRISE, "Shopee Cần Gì Cũng Có", "Saigon Centre, 65 Lê Lợi, Bến Nghé, Quận 1", "Hồ Chí Minh", "2015", "Nền tảng thương mại điện tử hàng đầu khu vực Đông Nam Á và Đài Loan."},
            {"VNPAY", "tuyendung@vnpay.vn", "https://upload.wikimedia.org/wikipedia/commons/b/bc/VNPAY_logo.png", "https://vnpay.vn/cover.jpg", "https://vnpay.vn", "FinTech", CompanySize.LARGE, "Thanh toán không tiền mặt", "Phong Sắc, Dịch Vọng Hậu, Cầu Giấy", "Hà Nội", "2007", "Công ty cổ phần giải pháp thanh toán Việt Nam, sở hữu mạng lưới VNPAY-QR lớn nhất."},
            {"Tiki", "careers@tiki.vn", "https://upload.wikimedia.org/wikipedia/commons/1/18/Tiki_logo.png", "https://tiki.vn/cover.jpg", "https://tiki.vn", "E-commerce", CompanySize.LARGE, "Khách hàng là trên hết", "52 Út Tịch, Tân Bình", "Hồ Chí Minh", "2010", "Hệ sinh thái thương mại và công nghệ hàng đầu Việt Nam cam kết giao hàng nhanh."},
            {"Zalo Group", "careers@zalo.me", "https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/Zalo_Icon.svg/1200px-Zalo_Icon.svg.png", "https://zalo.me/cover.jpg", "https://zalo.me", "Product Company", CompanySize.LARGE, "Kết nối mọi người", "Z06 Khu chế xuất Tân Thuận, Quận 7", "Hồ Chí Minh", "2012", "Siêu ứng dụng nhắn tin và gọi điện phổ biến nhất Việt Nam."},
            {"CMC Corporation", "hr@cmc.com.vn", "https://cmc.com.vn/wp-content/uploads/2021/04/logo-cmc.png", "https://cmc.com.vn/cover.jpg", "https://cmc.com.vn", "IT / Telecommunications", CompanySize.ENTERPRISE, "Khát khao chinh phục", "Toà nhà CMC, Cầu Giấy", "Hà Nội", "1993", "Tập đoàn công nghệ thông tin và viễn thông uy tín tại Việt Nam."},
            {"NashTech", "recruitment@nashtechglobal.com", "https://nashtechglobal.com/wp-content/uploads/2022/10/nashtech-logo-red.svg", "https://nashtech.com/cover.jpg", "https://nashtechglobal.com", "IT / Outsourcing", CompanySize.LARGE, "Deliver Value, Build Trust", "Etown 1, 364 Cộng Hoà, Tân Bình", "Hồ Chí Minh", "2000", "Công ty gia công phần mềm hàng đầu thuộc tập đoàn Harvey Nash (Anh Quốc)."},
            {"KMS Technology", "careers@kms-technology.com", "https://kms-technology.com/wp-content/uploads/2023/07/logo-kms.png", "https://kms.com/cover.jpg", "https://kms-technology.com", "IT / Outsourcing / Product", CompanySize.LARGE, "Creating Impact Daily", "KMS Building, 290 Cống Quỳnh, Quận 1", "Hồ Chí Minh", "2009", "Công ty dịch vụ phần mềm chuyên vào thị trường Mỹ với văn hoá đậm chất Agile."},
            {"VIB - Ngân hàng Quốc tế", "tuyendung@vib.com.vn", "https://upload.wikimedia.org/wikipedia/vi/thumb/e/e0/VIB_Logo.svg/1200px-VIB_Logo.svg.png", "https://vib.com.vn/cover.jpg", "https://vib.com.vn", "Banking / Finance", CompanySize.ENTERPRISE, "Trở thành ngân hàng sáng tạo nhất", "Sailing Tower, 111A Pasteur, Quận 1", "Hồ Chí Minh", "1996", "Ngân hàng Thương mại Cổ phần Quốc tế Việt Nam đi đầu trong công nghệ Core Banking."},
            {"Techcombank", "hr@techcombank.com.vn", "https://upload.wikimedia.org/wikipedia/commons/3/30/Techcombank_logo.png", "https://techcombank.com.vn/cover.jpg", "https://techcombank.com.vn", "Banking / Finance", CompanySize.ENTERPRISE, "Dẫn dắt tương lai tài chính", "Techcombank Tower, Quận Hoàn Kiếm", "Hà Nội", "1993", "1 trong những ngân hàng tư nhân lớn nhất Việt Nam đẩy mạnh hạ tầng công nghệ đám mây."},
            {"Axon Enterprise", "careers-vietnam@axon.com", "https://axon.com/images/axon-logo.png", "https://axon.com/cover.jpg", "https://axon.com", "Product / IT", CompanySize.MEDIUM, "Protect Life", "Deutsches Haus, 33 Lê Duẩn, Quận 1", "Hồ Chí Minh", "1993", "Công ty công nghệ vũ khí cảnh sát và camera đeo trên người lớn nhất toàn cầu."},
            {"Grab Việt Nam", "careers.vn@grab.com", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Grab_Logo.svg/1200px-Grab_Logo.svg.png", "https://grab.com/cover.jpg", "https://grab.com/vn", "Super App / Ride-hailing", CompanySize.ENTERPRISE, "Thúc đẩy Đông Nam Á tiến lên", "Toà nhà Mapletree, Quận 7", "Hồ Chí Minh", "2012", "Siêu ứng dụng đa dịch vụ phục vụ đời sống hằng ngày của người Việt."},
            {"Gojek Việt Nam", "tuyendung@gojek.com", "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/Gojek_logo.svg/1200px-Gojek_logo.svg.png", "https://gojek.com/cover.jpg", "https://gojek.com/vn", "Super App / Ride-hailing", CompanySize.ENTERPRISE, "Chắc chắn có giải pháp", "Phú Mỹ Hưng, Quận 7", "Hồ Chí Minh", "2010", "Dịch vụ gọi xe công nghệ và giao thức ăn hàng đầu từ Indonesia."},
            {"VCCorp", "hr@vccorp.vn", "https://vccorp.vn/assets/images/logo.png", "https://vccorp.vn/cover.jpg", "https://vccorp.vn", "Media / IT", CompanySize.LARGE, "Nơi hội tụ những khát vọng", "Center Building, Hapulico, Thanh Xuân", "Hà Nội", "2006", "Hệ sinh thái truyền thông trực tuyến, quảng cáo số và chuyển đổi số."},
            {"Base.vn", "tuyendung@base.vn", "https://base.vn/wp-content/uploads/2023/10/Base-Logo-1.png", "https://base.vn/cover.jpg", "https://base.vn", "B2B SaaS", CompanySize.MEDIUM, "Kết nối doanh nghiệp", "Toà nhà The Landmark, Quận 1", "Hồ Chí Minh", "2016", "Nền tảng quản trị doanh nghiệp SaaS hàng đầu được hơn 8000 công ty sử dụng."},
            {"OneMount Group", "careers@onemount.com", "https://onemount.com/logo.png", "https://onemount.com/cover.jpg", "https://onemount.com", "Retail / Tech", CompanySize.LARGE, "Tiên phong xây dựng hệ sinh thái", "Times City, Minh Khai, Hai Bà Trưng", "Hà Nội", "2019", "Thành viên của tập đoàn Masan và Techcombank chuyên kiến tạo công nghệ chuỗi cung ứng."},
            {"VNPT Technology", "hr@vnpt-technology.vn", "https://vnpt.com.vn/Media/Images/logo.png", "https://vnpt.com.vn/cover.jpg", "https://vnpt.com.vn", "Telecommunications", CompanySize.ENTERPRISE, "Cuộc sống đích thực", "Khu CNC Hoà Lạc, Thạch Thất", "Hà Nội", "1995", "Doanh nghiệp chủ lực của Tập đoàn VNPT trong lĩnh vực nghiên cứu, phát triển công nghệ cao."}
        };

        for (Object[] cInfo : companyData) {
            String name = (String) cInfo[0];
            String email = (String) cInfo[1];
            String logoUrl = (String) cInfo[2];
            String coverUrl = (String) cInfo[3];
            String website = (String) cInfo[4];
            String industry = (String) cInfo[5];
            CompanySize size = (CompanySize) cInfo[6];
            String tagline = (String) cInfo[7];
            String address = (String) cInfo[8];
            String city = (String) cInfo[9];
            String founded = (String) cInfo[10];
            String description = (String) cInfo[11];

            // Create Company HR User
            User user = User.builder()
                .email(email)
                .passwordHash(genericPassword)
                .fullName("HR " + name)
                .role(Role.HR)
                .isActive(true)
                .isOnboarded(true)
                .build();
            userRepository.save(user);

            // Tech Stack & Benefits
            List<String> techStack = Arrays.asList("Java", "Spring Boot", "React", "AWS", "Docker", "Kubernetes", "PostgreSQL", "Kafka");
            CompanyBenefit b1 = new CompanyBenefit(UUID.randomUUID().toString(), "Heart", "Bảo hiểm sức khoẻ toàn diện", "Bảo hiểm ngoại trú 100%, bảo hiểm người thân.");
            CompanyBenefit b2 = new CompanyBenefit(UUID.randomUUID().toString(), "DollarSign", "Review lương 2 lần/năm", "Cơ hội thăng tiến và thưởng tháng 13, 14, KPI up to 3 tháng lương.");
            CompanyBenefit b3 = new CompanyBenefit(UUID.randomUUID().toString(), "Monitor", "Thiết bị cao cấp", "Cấp Macbook Pro M3 16GB hoặc Laptop tuỳ chọn cấu hình cao.");
            List<CompanyBenefit> benefits = Arrays.asList(b1, b2, b3);

            // Create Company
            Company company = Company.builder()
                .name(name)
                .email(email)
                .logoUrl(logoUrl)
                .coverUrl(coverUrl)
                .website(website)
                .industry(industry)
                .companySize(size)
                .tagline(tagline)
                .address(address)
                .city(city)
                .description(description)
                .founded(founded)
                .createdBy(user)
                .isVerified(true)
                .techStack(techStack)
                .benefits(benefits)
                .build();
            
            companyRepository.save(company);
            createdCompanies.add(company);
        }

        // 2. Arrays of data to randomize 50 Job Postings
        String[] jobTitles = {
            "Senior Backend Engineer (Java, Spring Boot)",
            "Middle ReactJS Developer",
            "DevOps Engineer (AWS, Kubernetes)",
            "Technical Project Manager (Scrum Master)",
            "Senior UI/UX Designer (Figma, Design System)",
            "Data Analyst (SQL, Python, PowerBI)",
            "AI/ML Research Scientist",
            "Mobile App Developer (Flutter/iOS)",
            "Business Analyst (IT)",
            "Senior QA Automation Engineer",
            "Fullstack JavaScript Developer (NodeJS/ReactJS)"
        };

        String[] requirementsTpl = {
            "<ul><li>Ít nhất 3 năm kinh nghiệm trong thiết kế phần mềm.</li><li>Nắm vững cấu trúc dữ liệu và giải thuật.</li><li>Có kinh nghiệm Microservices là một lợi thế cực lớn.</li><li>Khả năng làm việc nhóm và giao tiếp ứng xử tốt.</li><li>Tiếng Anh giao tiếp hoặc đọc hiểu tài liệu chuyên ngành.</li></ul>",
            "<ul><li>Nắm vững các design pattern của OOP, am hiểu kiến trúc hệ thống lớn.</li><li>Sử dụng thành thạo Git và các quy trình Agile/Scrum.</li><li>Biết tối ưu hoá SQL/NoSQL ở mức nâng cao (Index, Execution Plan).</li><li>Có mindset về hệ thống phân tán và bảo mật (OWASP).</li><li>Kỹ năng problem-solving vượt trội.</li></ul>"
        };

        String[] benefitsTpl = {
            "<ul><li>Lương cứng cực kỳ hấp dẫn theo năng lực.</li><li>Thưởng tháng 13 và Performance Bonus lên tới 4 tháng lương/năm.</li><li>Gói bảo hiểm sức khỏe VIP AON cho cá nhân và gia đình.</li><li>Tham gia các seminar, khoá đào tạo quốc tế miễn phí.</li><li>Trợ cấp ăn trưa 50.000 VNĐ/ngày.</li></ul>",
            "<ul><li>Thời làm việc linh hoạt (Flexible Working Hours/Hybrid).</li><li>Môi trường quốc tế, chuyên nghiệp và cực kỳ cởi mở.</li><li>Hoạt động văn hoá phong phú (teambuilding, dã ngoại 2-3 lần/năm).</li><li>Tháng lương thứ 14 và review lương 2 lần/năm.</li><li>Chính sách mua cổ phiếu ESOP ưu đãi.</li></ul>"
        };
        
        String descTpl = "<p>Chúng tôi đang tìm kiếm đồng đội đầy đam mê và nhiệt huyết để cùng tham gia xây dựng các hệ thống Core với scale hàng triệu DAU (Daily Active Users). Nếu bạn là một chuyên gia thực sự yêu thích các thử thách kỹ thuật và muốn tạo ra giá trị thiết thực để thay đổi cuộc sống của hàng trăm nghìn người thì vị trí này sinh ra là dành cho bạn.</p>\n<p><strong>Mô tả công việc:</strong></p>\n<ul><li>Phân tích yêu cầu, thiết kế kiến trúc hệ thống với độ ổn định cao (High Availability).</li><li>Tham gia xây dựng các microservice phức tạp, đáp ứng tải lớn.</li><li>Review code, hướng dẫn các thành viên Junior trong nhóm.</li><li>Liên tục tối ưu hiệu năng (Performance Tuning) cho ứng dụng.</li></ul>";

        Random rand = new Random();

        // Generate exactly 50 jobs
        for (int i = 0; i < 50; i++) {
            // Pick a random company
            Company comp = createdCompanies.get(rand.nextInt(createdCompanies.size()));
            
            String title = jobTitles[rand.nextInt(jobTitles.length)];
            String requirements = requirementsTpl[rand.nextInt(requirementsTpl.length)];
            String benefits = benefitsTpl[rand.nextInt(benefitsTpl.length)];

            JobLevel level = (title.contains("Senior") || title.contains("Manager") || title.contains("Research")) ? JobLevel.SENIOR : (title.contains("Middle") ? JobLevel.MID : JobLevel.JUNIOR);
            long minSal = 15000000L + (long)(rand.nextInt(20)) * 1000000L;
            long maxSal = minSal + (long)(rand.nextInt(25)) * 1000000L;
            if (level == JobLevel.SENIOR) {
                minSal += 15000000L;
                maxSal += 20000000L;
            }

            Job job = Job.builder()
                .company(comp)
                .createdBy(comp.getCreatedBy())
                .title(title)
                .description(descTpl)
                .requirements(requirements)
                .benefits(benefits)
                .jobType(JobType.FULL_TIME)
                .jobLevel(level)
                .location(comp.getCity() + " - " + comp.getAddress())
                .isRemote(rand.nextBoolean())
                .salaryMin(BigDecimal.valueOf(minSal))
                .salaryMax(BigDecimal.valueOf(maxSal))
                .salaryCurrency("VND")
                .deadline(LocalDate.now().plusDays(30 + rand.nextInt(30)))
                .status(JobStatus.OPEN)
                .build();
            
            // Generate some random skills for this job
            List<String> rawSkills = Arrays.asList("Java", "Spring Boot", "React", "Vue", "Docker", "Agile", "Microservices", "Python", "SQL", "Kafka", "Figma", "AWS");
            Collections.shuffle(rawSkills);
            for (int k = 0; k < 4; k++) {
                JobSkill jskill = new JobSkill();
                jskill.setSkillName(rawSkills.get(k));
                jskill.setJob(job);
                job.getSkills().add(jskill);
            }

            jobRepository.save(job);
        }

        log.info("Finished Database Seeding! Total Companies: {} | Total Jobs: 50", createdCompanies.size());
    }
}
