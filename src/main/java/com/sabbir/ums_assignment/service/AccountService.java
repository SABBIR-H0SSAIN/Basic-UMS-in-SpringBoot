package com.sabbir.ums_assignment.service;

import com.sabbir.ums_assignment.dto.*;
import com.sabbir.ums_assignment.entity.*;
import com.sabbir.ums_assignment.exception.ApiException;
import com.sabbir.ums_assignment.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public List<AccountResponse> findAll(String role) {
        List<AccountResponse> responses = new ArrayList<>();
        for (Account account : repository.findAll()) {
            if (account.getRole().equals(role)) {
                responses.add(AccountResponse.from(account));
            }
        }

        return responses;
    }

    public AccountResponse findById(String role, Long id) {
        return AccountResponse.from(findAccount(role, id));
    }

    @Transactional
    public AccountResponse create(String role, AccountRequest request) {
        validate(request);
        if (role.equals("STUDENT")) {
            validateStudentFields(request);
        }
        if (role.equals("TEACHER")) {
            validateTeacherFields(request);
        }
        if (repository.existsByUsername(request.username().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        Account account = switch (role) {
            case "ADMIN" -> new Admin();
            case "TEACHER" -> new Teacher();
            case "STUDENT" -> new Student();
            default -> throw new IllegalArgumentException("Unknown role");
        };
        setAccountFields(account, request);
        return AccountResponse.from(repository.saveAndFlush(account));
    }

    @Transactional
    public AccountResponse update(String role, Long id, AccountRequest request) {
        Account account = findAccount(role, id);
        validate(request);
        if (role.equals("STUDENT")) {
            validateStudentFields(request);
        }
        if (role.equals("TEACHER")) {
            validateTeacherFields(request);
        }
        Account existingAccount = repository.findByUsername(request.username().trim()).orElse(null);
        if (existingAccount != null && !existingAccount.getId().equals(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        setAccountFields(account, request);
        return AccountResponse.from(repository.saveAndFlush(account));
    }

    @Transactional
    public void delete(String role, Long id) {
        repository.delete(findAccount(role, id));
        repository.flush();
    }

    private Account findAccount(String role, Long id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found"));
        if (!account.getRole().equals(role)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found");
        }

        return account;
    }

    private void setAccountFields(Account account, AccountRequest request) {
        account.setUsername(request.username().trim());
        // Store the BCrypt hash, never the original password.
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setName(request.name().trim());
        account.setEmail(request.email().trim());
        if (account instanceof Student student) {
            student.setYear(request.year());
            student.setSemester(request.semester());
            student.setCgpa(request.cgpa());
        }
        if (account instanceof Teacher teacher) {
            teacher.setDepartment(request.department() == null || request.department().isBlank()
                    ? null : request.department().trim());
        }
    }

    private void validateStudentFields(AccountRequest request) {
        if (request.year() != null && (request.year() < 1 || request.year() > 4)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Year must be between 1 and 4");
        }
        if (request.semester() != null && (request.semester() < 1 || request.semester() > 2)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Semester must be 1 (1st) or 2 (2nd)");
        }
        BigDecimal cgpa = request.cgpa();
        if (cgpa != null && (cgpa.compareTo(BigDecimal.ZERO) < 0
                || cgpa.compareTo(new BigDecimal("4.00")) > 0
                || cgpa.stripTrailingZeros().scale() > 2)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CGPA must be between 0.00 and 4.00 with at most two decimal places");
        }
    }

    private void validateTeacherFields(AccountRequest request) {
        if (request.department() != null && request.department().trim().length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department must be at most 100 characters");
        }
    }

    private void validate(AccountRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        String username = request.username();
        if (username == null || !username.trim().matches("[A-Za-z0-9_.-]{3,50}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Username must contain 3-50 letters, digits, underscores, dots or hyphens");
        }

        String password = request.password();
        if (password == null || password.isBlank() || password.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must contain at least 8 characters");
        }

        // BCrypt supports passwords up to 72 bytes, which may differ from the character count.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must not exceed 72 UTF-8 bytes");
        }

        String name = request.name();
        if (name == null || name.isBlank() || name.trim().length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Name must contain 1-100 characters");
        }

        String email = request.email();
        if (email == null || email.length() > 254
                || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Provide a valid email of at most 254 characters");
        }
    }
}
