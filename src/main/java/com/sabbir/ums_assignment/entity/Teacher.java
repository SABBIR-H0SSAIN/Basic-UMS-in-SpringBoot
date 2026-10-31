package com.sabbir.ums_assignment.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("TEACHER")
public class Teacher extends Account {
    @Column(length = 100)
    private String department;

    @Override
    public String getRole() { return "TEACHER"; }
}
