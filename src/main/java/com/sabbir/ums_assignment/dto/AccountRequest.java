package com.sabbir.ums_assignment.dto;

import java.math.BigDecimal;

public record AccountRequest(String username, String password, String name, String email,
                             Integer year, Integer semester, BigDecimal cgpa, String department) {
    public AccountRequest(String username, String password, String name, String email) {
        this(username, password, name, email, null, null, null, null);
    }
}
