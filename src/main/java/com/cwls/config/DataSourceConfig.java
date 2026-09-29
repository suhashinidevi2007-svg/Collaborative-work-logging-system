package com.cwls.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        HikariDataSource ds = new HikariDataSource();

        // 1. Retrieve potential database connection environment variables
        String dbUrl = env.getProperty("SPRING_DATASOURCE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = env.getProperty("SUPABASE_DB_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = env.getProperty("DATABASE_URL");
        }

        String username = env.getProperty("SPRING_DATASOURCE_USERNAME");
        if (username == null || username.isBlank()) {
            username = env.getProperty("SUPABASE_DB_USER");
        }

        String password = env.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (password == null || password.isBlank()) {
            password = env.getProperty("SUPABASE_DB_PASSWORD");
        }

        // 2. Check if placeholders were accidentally left in environment variables
        boolean hasPlaceholders = (dbUrl != null && (dbUrl.contains("<") || dbUrl.contains(">") || dbUrl.contains("SUPABASE_HOST") || dbUrl.contains("YOUR_")))
                || (username != null && (username.contains("<") || username.contains(">") || username.contains("SUPABASE_USER")))
                || (password != null && (password.contains("<") || password.contains(">") || password.contains("YOUR_")));

        if (hasPlaceholders) {
            System.err.println(">>> [CWLS WARNING] Placeholder detected in database config. Falling back to H2 in-memory mode.");
            dbUrl = null;
        }

        // 3. Handle Cloud Database URL (Supabase PostgreSQL / MySQL)
        if (dbUrl != null && !dbUrl.isBlank()) {
            // Support raw URI format: postgresql://user:password@host:port/database
            if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                try {
                    String rawUri = dbUrl.replace("jdbc:", "");
                    URI uri = new URI(rawUri);

                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        if (username == null || username.isBlank()) username = parts[0];
                        if (password == null || password.isBlank()) password = parts[1];
                    }

                    int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                    String path = uri.getPath();
                    String query = uri.getQuery();

                    String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path;
                    if (query != null && !query.isBlank()) {
                        jdbcUrl += "?" + query;
                    } else {
                        jdbcUrl += "?sslmode=require";
                    }
                    dbUrl = jdbcUrl;
                } catch (Exception e) {
                    if (!dbUrl.startsWith("jdbc:")) {
                        dbUrl = "jdbc:" + dbUrl;
                    }
                }
            }

            ds.setJdbcUrl(dbUrl);
            if (username != null && !username.isBlank()) ds.setUsername(username);
            if (password != null && !password.isBlank()) ds.setPassword(password);

            if (dbUrl.contains("postgresql")) {
                ds.setDriverClassName("org.postgresql.Driver");
            } else if (dbUrl.contains("mysql")) {
                ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }

            ds.setMaximumPoolSize(10);
            ds.setMinimumIdle(2);
            ds.setIdleTimeout(30000);
            ds.setMaxLifetime(600000);
            ds.setConnectionTimeout(20000);
            return ds;
        }

        // 4. Fallback for Render Free Tier or Cloud when no DB is provided:
        // Automatically run with in-memory H2 so deployment never fails!
        boolean isCloudEnvironment = System.getenv("RENDER") != null ||
                                     System.getenv("PORT") != null ||
                                     "h2".equalsIgnoreCase(env.getProperty("SPRING_PROFILES_ACTIVE"));

        String localMySqlUrl = env.getProperty("spring.datasource.url");
        if (isCloudEnvironment && (localMySqlUrl == null || localMySqlUrl.contains("localhost:3306"))) {
            System.out.println(">>> [CWLS] No cloud database URL configured on Render. Booting in H2 in-memory mode.");
            ds.setJdbcUrl("jdbc:h2:mem:cwls_db;DB_CLOSE_DELAY=-1;MODE=MySQL");
            ds.setDriverClassName("org.h2.Driver");
            ds.setUsername("sa");
            ds.setPassword("");
            ds.setMaximumPoolSize(5);
            return ds;
        }

        // 5. Default Local Development (MySQL localhost)
        ds.setJdbcUrl(localMySqlUrl != null ? localMySqlUrl : "jdbc:mysql://localhost:3306/cwls_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
        ds.setUsername(username != null ? username : "root");
        ds.setPassword(password != null ? password : "root");
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return ds;
    }
}
