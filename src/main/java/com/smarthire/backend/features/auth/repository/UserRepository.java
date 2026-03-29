package com.smarthire.backend.features.auth.repository;

import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.shared.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGithubId(String githubId);

    long countByRole(Role role);

    long countByIsActive(Boolean isActive);
}
