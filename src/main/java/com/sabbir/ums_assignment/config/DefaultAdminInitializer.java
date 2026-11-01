package com.sabbir.ums_assignment.config;

import com.sabbir.ums_assignment.dto.AccountRequest;
import com.sabbir.ums_assignment.entity.Teacher;
import com.sabbir.ums_assignment.repository.AccountRepository;
import com.sabbir.ums_assignment.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements CommandLineRunner {
    private final AccountRepository repository;
    private final AccountService service;
    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String password;

    @Override
    public void run(String... args) {
        // Seed each sample account independently; never reset an existing account's password.
        seed("ADMIN", adminUsername, password, "University Administrator", "admin@university.edu");
        seed("STUDENT", "student", "admin123", "Ayesha Rahman", "ayesha.rahman@student.university.edu");
        if (!repository.existsByUsername("teacher")) {
            service.create("TEACHER", new AccountRequest("teacher", "admin123", "Dr. Farhan Ahmed",
                    "farhan.ahmed@university.edu", null, null, null, "Computer Science and Engineering"));
        } else {
            repository.findByUsername("teacher").ifPresent(account -> {
                if (account instanceof Teacher teacher && teacher.getDepartment() == null
                        && teacher.getEmail().equals("farhan.ahmed@university.edu")) {
                    teacher.setDepartment("Computer Science and Engineering");
                    repository.save(teacher);
                }
            });
        }
    }

    private void seed(String role, String username, String password, String name, String email) {
        if (!repository.existsByUsername(username)) {
            service.create(role, new AccountRequest(username, password, name, email));
        }
    }
}
