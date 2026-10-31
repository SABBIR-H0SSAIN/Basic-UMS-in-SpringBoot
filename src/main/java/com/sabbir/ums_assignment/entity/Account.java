package com.sabbir.ums_assignment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
public abstract class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String username;
    @JsonIgnore
    @Column(nullable = false)
    private String password;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 254)
    private String email;
    public abstract String getRole();
}
