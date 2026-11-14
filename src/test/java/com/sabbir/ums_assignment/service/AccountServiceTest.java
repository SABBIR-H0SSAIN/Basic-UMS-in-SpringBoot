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
        var request = new AccountRequest(" ayesha ", "student123", "Ayesha Rahman",
                "ayesha@example.com", 2, 1, new BigDecimal("3.75"), null);
        when(passwordEncoder.encode("student123")).thenReturn("encoded-password");
        when(repository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            assertThat(student.getPassword()).isEqualTo("encoded-password");
            student.setId(1L);
            return student;
        });

        AccountResponse student = service.create("STUDENT", request);

        assertThat(student.id()).isEqualTo(1L);
        assertThat(student.username()).isEqualTo("ayesha");
        assertThat(student.role()).isEqualTo("STUDENT");
        assertThat(student.year()).isEqualTo(2);
        assertThat(student.semester()).isEqualTo(1);
        assertThat(student.cgpa()).isEqualByComparingTo("3.75");
    }

    @Test
    void listsOnlyAccountsWithTheRequestedRole() {
        var student = new Student();
        student.setUsername("ayesha");
        var teacher = new Teacher();
        teacher.setUsername("farhan");
        when(repository.findAll()).thenReturn(List.of(student, teacher));

        assertThat(service.findAll("TEACHER"))
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
        var request = new AccountRequest("ayesha", "student123", "Ayesha Rahman", "ayesha@example.com");
        when(repository.existsByUsername("ayesha")).thenReturn(true);

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsAShortPassword() {
        var request = new AccountRequest("ayesha", "short", "Ayesha Rahman", "ayesha@example.com");

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error).hasMessage("Password must contain at least 8 characters");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsAStudentYearOutsideTheAllowedRange() {
        var request = new AccountRequest("ayesha", "student123", "Ayesha Rahman",
                "ayesha@example.com", 5, 1, new BigDecimal("3.75"), null);

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error).hasMessage("Year must be between 1 and 4");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsACgpaWithMoreThanTwoDecimalPlaces() {
        var request = new AccountRequest("ayesha", "student123", "Ayesha Rahman",
                "ayesha@example.com", 2, 1, new BigDecimal("3.755"), null);

        var error = assertThrows(ApiException.class, () -> service.create("STUDENT", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error).hasMessageContaining("at most two decimal places");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsATeacherDepartmentLongerThanOneHundredCharacters() {
        var request = new AccountRequest("farhan", "teacher123", "Farhan Ahmed", "farhan@example.com",
                null, null, null, "a".repeat(101));

        var error = assertThrows(ApiException.class, () -> service.create("TEACHER", request));

        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error).hasMessage("Department must be at most 100 characters");
        verifyNoInteractions(repository);
    }
}
