package com.sabbir.ums_assignment.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@DiscriminatorValue("STUDENT")
public class Student extends Account {
    private Integer year;
    private Integer semester;
    @Column(precision = 3, scale = 2)
    private BigDecimal cgpa;


    @Override
    public String getRole() { return "STUDENT"; }
}
