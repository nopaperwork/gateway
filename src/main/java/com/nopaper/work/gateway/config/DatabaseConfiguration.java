/**
 * @package com.nopaper.work.gateway.config -> gateway
 * @author saikatbarman
 * @date 2025 27-Oct-2025 1:12:38 am
 * @git 
 */
package com.nopaper.work.gateway.config;

/**
 * 
 */

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Database configuration for R2DBC.
 * 
 * Configures:
 * - Schema initialization
 * - Connection pooling
 * - Auditing support
 */
@Slf4j
@Configuration
@EnableR2dbcAuditing
public class DatabaseConfiguration {
    
    @Value("classpath:schema.sql")
    private Resource schemaScript;
    
    /**
     * Initialize database schema on startup.
     * In production, use Flyway or Liquibase for migrations.
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schemaScript);
        initializer.setDatabasePopulator(populator);
        
        return initializer;
    }
}
