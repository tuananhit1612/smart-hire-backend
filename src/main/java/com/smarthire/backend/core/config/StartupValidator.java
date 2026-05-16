package com.smarthire.backend.core.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

/**
 * Validates critical configuration values at startup.
 * Prevents accidental deployment with dev-mode settings.
 */
@Slf4j
@Configuration
public class StartupValidator {

    private final Environment environment;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${spring.jpa.show-sql:true}")
    private String showSql;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.datasource.password:}")
    private String databasePassword;

    public StartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.contains("prod") || activeProfiles.contains("production");

        log.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");
        log.info("  SmartHire Backend â€” Startup Validation");
        log.info("  Active Profiles: {}", activeProfiles.isEmpty() ? "[default]" : activeProfiles);
        log.info("  Hibernate DDL:   {}", ddlAuto);
        log.info("  Show SQL:        {}", showSql);
        log.info("  Seed Data:       {}", seedEnabled);
        log.info("  Frontend URL:    {}", frontendUrl);
        log.info("â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        if (isProd) {
            // â”€â”€ Critical checks for production â”€â”€
            if ("update".equalsIgnoreCase(ddlAuto) || "create".equalsIgnoreCase(ddlAuto)
                    || "create-drop".equalsIgnoreCase(ddlAuto)) {
                throw new IllegalStateException(
                        "â›” UNSAFE: spring.jpa.hibernate.ddl-auto=" + ddlAuto
                                + " in PRODUCTION profile! Must be 'validate' or 'none'."
                );
            }

            if (seedEnabled) {
                log.warn("âš ï¸ WARNING: app.seed.enabled=true in production â€” seed data will be inserted!");
            }

            if (frontendUrl.contains("localhost")) {
                log.warn("âš ï¸ WARNING: app.frontend.url contains 'localhost' in production: {}", frontendUrl);
            }

            if (jwtSecret.isBlank() || jwtSecret.length() < 32) {
                throw new IllegalStateException("UNSAFE: app.jwt.secret must be set to at least 32 characters in production.");
            }

            if (databasePassword.isBlank() || "root".equals(databasePassword) || "password".equalsIgnoreCase(databasePassword)) {
                throw new IllegalStateException("UNSAFE: spring.datasource.password must be a strong non-default value in production.");
            }

            log.info("Production validation passed.");
        } else {
            log.info("â„¹ï¸  Running in DEV mode â€” skipping production safety checks.");
        }
    }
}
