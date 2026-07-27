package com.itnoduck.acmate.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    public FlywayConfig(DataSource dataSource,
                        @Value("${spring.flyway.locations:classpath:db/migration}") String[] locations) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .load()
                .migrate();
    }
}
