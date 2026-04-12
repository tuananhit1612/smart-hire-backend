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

    public StartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isProd = activeProfiles.contains("prod") || activeProfiles.contains("production");

        log.info("═══════════════════════════════════════════════════");
        log.info("  SmartHire Backend — Startup Validation");
        log.info("  Active Profiles: {}", activeProfiles.isEmpty() ? "[default]" : activeProfiles);
        log.info("  Hibernate DDL:   {}", ddlAuto);
        log.info("  Show SQL:        {}", showSql);
        log.info("  Seed Data:       {}", seedEnabled);
        log.info("  Frontend URL:    {}", frontendUrl);
        log.info("═══════════════════════════════════════════════════");

        if (isProd) {
            // ── Critical checks for production ──
            if ("update".equalsIgnoreCase(ddlAuto) || "create".equalsIgnoreCase(ddlAuto)
                    || "create-drop".equalsIgnoreCase(ddlAuto)) {
                throw new IllegalStateException(
                        "⛔ UNSAFE: spring.jpa.hibernate.ddl-auto=" + ddlAuto
                                + " in PRODUCTION profile! Must be 'validate' or 'none'."
                );
            }

            if (seedEnabled) {
                log.warn("⚠️ WARNING: app.seed.enabled=true in production — seed data will be inserted!");
            }

            if (frontendUrl.contains("localhost")) {
                log.warn("⚠️ WARNING: app.frontend.url contains 'localhost' in production: {}", frontendUrl);
            }

            log.info("✅ Production validation passed.");
        } else {
            log.info("ℹ️  Running in DEV mode — skipping production safety checks.");
        }
    }
}
