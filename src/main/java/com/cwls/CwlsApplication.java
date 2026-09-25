package com.cwls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.cwls")
@EnableJpaRepositories(basePackages = "com.cwls.repository")
@EntityScan(basePackages = "com.cwls.model")
public class CwlsApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CwlsApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(CwlsApplication.class, args);
    }
}