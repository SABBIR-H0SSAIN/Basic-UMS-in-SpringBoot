package com.sabbir.ums_assignment.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends Account {
    @Override
    public String getRole() { return "ADMIN"; }
}
