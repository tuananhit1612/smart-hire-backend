package com.smarthire.backend.features.job.repository;

import com.smarthire.backend.features.job.entity.Job;
import com.smarthire.backend.shared.enums.JobLevel;
import com.smarthire.backend.shared.enums.JobStatus;
import com.smarthire.backend.shared.enums.JobType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> search(String keyword, String location, String jobLevel,
                                             String jobType, BigDecimal salaryMin, BigDecimal salaryMax) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only show OPEN jobs in public listing
            predicates.add(cb.equal(root.get("status"), JobStatus.OPEN));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                query.distinct(true);
                jakarta.persistence.criteria.Join<Object, Object> skillsJoin = root.join("skills", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("company").get("name")), pattern),
                        cb.like(cb.lower(skillsJoin.get("skillName")), pattern)
                ));
            }

            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }

            if (jobLevel != null && !jobLevel.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("jobLevel"), JobLevel.valueOf(jobLevel.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            if (jobType != null && !jobType.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("jobType"), JobType.valueOf(jobType.toUpperCase())));
                } catch (IllegalArgumentException ignored) {}
            }

            if (salaryMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), salaryMin));
            }

            if (salaryMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), salaryMax));
            }

            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
