package com.sabbir.ums_assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sabbir.ums_assignment.entity.Account;
import com.sabbir.ums_assignment.entity.Student;
import com.sabbir.ums_assignment.entity.Teacher;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(Long id, String username, String name, String email, String role,
                              Integer year, Integer semester, BigDecimal cgpa, String department) {
    public static AccountResponse from(Account account) {
        if (account instanceof Student student) {
            return new AccountResponse(student.getId(), student.getUsername(), student.getName(),
                    student.getEmail(), student.getRole(), student.getYear(), student.getSemester(), student.getCgpa(), null);
        }
        if (account instanceof Teacher teacher) {
            return new AccountResponse(teacher.getId(), teacher.getUsername(), teacher.getName(),
                    teacher.getEmail(), teacher.getRole(), null, null, null, teacher.getDepartment());
        }
        return new AccountResponse(account.getId(), account.getUsername(), account.getName(),
                account.getEmail(), account.getRole(), null, null, null, null);
    }
}
