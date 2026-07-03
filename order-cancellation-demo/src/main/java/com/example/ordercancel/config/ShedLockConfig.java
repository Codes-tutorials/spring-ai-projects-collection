package com.example.ordercancel.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * ShedLock Configuration
 * ----------------------
 * Ensures only ONE instance of the application runs a scheduled task at a time
 * in a clustered deployment (e.g., 3 Kubernetes pods).
 *
 * Uses the same relational DB (H2 locally, PostgreSQL in prod) to coordinate locks.
 * The `shedlock` table is created by Flyway migration V1.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")  // Max lock duration: 30 seconds
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()   // Use database server time (avoids clock skew between nodes)
                        .build()
        );
    }
}
