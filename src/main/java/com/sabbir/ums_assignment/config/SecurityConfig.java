package com.sabbir.ums_assignment.config;

import com.sabbir.ums_assignment.service.AccountUserDetailsService;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.*;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AccountUserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contexts) throws Exception {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        RequestMatcher signedInPages = new OrRequestMatcher(paths.matcher("/dashboard"),
                paths.matcher("/admins"), paths.matcher("/teachers"), paths.matcher("/students"));

        return http
                .securityContext(context -> context.securityContextRepository(contexts))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/login.html", "/dashboard.html", "/admins.html", "/teachers.html",
                                "/students.html", "/style.css", "/common.js", "/login.js",
                                "/dashboard.js", "/management.js").permitAll()
                        .requestMatchers(signedInPages).authenticated()
                        .requestMatchers("/api/auth/csrf", "/api/auth/login", "/error").permitAll()
                        .requestMatchers("/api/admins", "/api/admins/**").hasRole("ADMIN")
                        .requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/students", "/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), signedInPages)
                        .defaultAuthenticationEntryPointFor((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"message\":\"Login required\"}");
                        }, AnyRequestMatcher.INSTANCE)
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"message\":\"Access denied or missing/invalid CSRF token\"}");
                        }))
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .build();
    }
}
