package com.sabbir.ums_assignment.service;

import com.sabbir.ums_assignment.dto.AccountRequest;
import com.sabbir.ums_assignment.dto.AccountResponse;
import com.sabbir.ums_assignment.entity.Student;
import com.sabbir.ums_assignment.entity.Teacher;
import com.sabbir.ums_assignment.exception.ApiException;
import com.sabbir.ums_assignment.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService service;

    @Test
    void createsStudentWithTrimmedUsernameAndEncodedPassword() {
        var request = new AccountRequest(" sabbir ", "student123", "Sabbir",
                "sabbir@example.com", 2, 1, new BigDecimal("3.75"), null);
        when(passwordEncoder.encode("student123")).thenReturn("encoded-password");
        when(repository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            assertThat(student.getPassword()).isEqualTo("encoded-password");
            student.setId(1L);
            return student;
        });

        AccountResponse student = service.create("STUDENT", request);

        assertThat(student.id()).isEqualTo(1L);
        assertThat(student.username()).isEqualTo("sabbir");
        assertThat(student.role()).isEqualTo("STUDENT");
        assertThat(student.year()).isEqualTo(2);
        assertThat(student.semester()).isEqualTo(1);
        assertThat(student.cgpa()).isEqualByComparingTo("3.75");
    }

    @Test
    void listsOnlyAccountsWithTheRequestedRole() {
        var student = new Student();
        student.setUsername("sabbir");
        var teacher = new Teacher();
        teacher.setUsername("farhan");
        when(repository.findAll()).thenReturn(List.of(student, teacher));

        assertThat(service.findAll("STUDENT"))
                .extracting(AccountResponse::username)
                .containsExactly("farhan");
    }

    @Test
    void returnsNotFoundWhenAccountHasAnotherRole() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Teacher()));

        var error = assertThrows(ApiException.class, () -> service.findById("STUDENT", 1L));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsAnExistingUsernameBeforeEncodingThePassword() {
        var request = new AccountRequest("sabbir", "student123", "Sabbir", "sabbir@example.com");
        when(repository.existsByUsername("sabbir")).thenReturn(true);

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsAShortPassword() {
        var request = new AccountRequest("sabbir", "short", "Sabbir", "sabbir@example.com");

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error).hasMessage("Password must contain at least 8 characters");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsCgpaGreaterThanFour() {
        var request = new AccountRequest("sabbir", "student123", "Sabbir",
                "sabbir@example.com", 2, 1, new BigDecimal("4.01"), null);

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error).hasMessageContaining("CGPA must be between 0.00 and 4.00");
        verifyNoInteractions(repository);
    }
}
