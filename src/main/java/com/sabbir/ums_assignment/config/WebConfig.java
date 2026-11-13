package com.sabbir.ums_assignment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/login");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/dashboard").setViewName("dashboard");
        registry.addViewController("/admins").setViewName("admins");
        registry.addViewController("/teachers").setViewName("teachers");
        registry.addViewController("/students").setViewName("students");

        registry.addRedirectViewController("/login.html", "/login");
        registry.addRedirectViewController("/dashboard.html", "/dashboard");
        registry.addRedirectViewController("/admins.html", "/admins");
        registry.addRedirectViewController("/teachers.html", "/teachers");
        registry.addRedirectViewController("/students.html", "/students");
    }
}
