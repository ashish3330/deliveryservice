package com.railswad.deliveryservice.entity;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_roles")
@Data
public class UserRole {
    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}